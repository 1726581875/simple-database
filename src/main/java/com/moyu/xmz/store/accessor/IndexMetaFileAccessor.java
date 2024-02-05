package com.moyu.xmz.store.accessor;

import com.moyu.xmz.store.common.meta.IndexMetadata;
import com.moyu.xmz.common.exception.ExceptionUtil;
import com.moyu.xmz.store.common.block.TableIndexBlock;
import com.moyu.xmz.common.util.PathUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xiaomingzhang
 * @date 2023/5/30
 */
public class IndexMetaFileAccessor {

    private String filePath;

    public static final String COLUMN_META_FILE_NAME = "index.meta";

    private FileAccessor fileAccessor;


    private List<TableIndexBlock> indexBlockList = new ArrayList<>();

    /**
     * key: tableId
     * value: ColumnIndexBlock
     */
    private Map<Integer, TableIndexBlock> indexBlockMap = new HashMap<>();

    private Integer databaseId;



    public IndexMetaFileAccessor(Integer databaseId) throws IOException {
        this.databaseId = databaseId;
        this.filePath = PathUtil.getIndexMetaPath(databaseId);
        init();
    }


    public void saveIndexMetadata(Integer tableId, IndexMetadata indexMetadata) {
        synchronized (ColumnMetaFileAccessor.class) {
            TableIndexBlock block = indexBlockMap.get(tableId);
            if (block == null) {
                TableIndexBlock lastData = getLastColumnBlock();
                long startPos = lastData == null ? 0 : lastData.getStartPos() + TableIndexBlock.TABLE_COLUMN_BLOCK_SIZE;
                int blockIndex = lastData == null ? 0 : lastData.getBlockIndex() + 1;
                TableIndexBlock columnBlock = new TableIndexBlock(blockIndex, startPos, tableId);
                indexMetadata.setStartPos(columnBlock.getIndexStartPos());
                columnBlock.addIndex(indexMetadata);
                fileAccessor.write(columnBlock.getByteBuffer(), startPos);
                indexBlockMap.put(columnBlock.getTableId(), columnBlock);
                indexBlockList.add(columnBlock);
            } else {
                List<IndexMetadata> list = block.getIndexMetadataList();
                if (list != null && list.size() > 0) {
                    IndexMetadata index = list.get(list.size() - 1);
                    indexMetadata.setStartPos(index.getStartPos() + index.getTotalByteLen());
                } else {
                    indexMetadata.setStartPos(block.getIndexStartPos());
                }
                block.addIndex(indexMetadata);
                fileAccessor.write(block.getByteBuffer(), block.getStartPos());
            }
        }
    }


    public void dropIndexMetadata(Integer tableId, String indexName) {
        synchronized (ColumnMetaFileAccessor.class) {
            TableIndexBlock block = indexBlockMap.get(tableId);
            if (block == null) {
                ExceptionUtil.throwSqlExecutionException("索引{}不存在", indexName);
            } else {
                List<IndexMetadata> list = block.getIndexMetadataList();

                int k = -1;
                if (list != null && list.size() > 0) {
                    for (int i = list.size() - 1; i >= 0; i--) {
                        IndexMetadata indexMetadata = list.get(i);
                        if(indexMetadata.getIndexName().equals(indexName)) {
                            k = i;
                            break;
                        }
                    }
                }

                if(k == -1) {
                    ExceptionUtil.throwSqlExecutionException("索引{}不存在", indexName);
                } else {
                    list.remove(k);
                }

                // 重新写索引块
                TableIndexBlock newBlock = new TableIndexBlock(block.getBlockIndex(), block.getStartPos(), block.getTableId());
                long startPos = newBlock.getStartPos();
                for (int i = 0; i < list.size(); i++) {
                    IndexMetadata index = list.get(i);
                    index.setStartPos(startPos);
                    newBlock.addIndex(index);
                    startPos += index.getStartPos() + index.getTotalByteLen();
                }
                fileAccessor.write(newBlock.getByteBuffer(), newBlock.getStartPos());
            }

            try {
                init();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public IndexMetadata getIndex(Integer tableId, String indexName) {
        TableIndexBlock block = indexBlockMap.get(tableId);
        if(block == null) {
            return null;
        }
        List<IndexMetadata> list = block.getIndexMetadataList();
        if (list != null && list.size() > 0) {
            for (int i = list.size() - 1; i >= 0; i--) {
                IndexMetadata indexMetadata = list.get(i);
                if (indexMetadata.getIndexName().equals(indexName)) {
                    return indexMetadata;
                }
            }
        }
        return null;
    }



    public void dropIndexBlock(Integer tableId) {
        TableIndexBlock columnBlock = indexBlockMap.get(tableId);

        if (columnBlock == null) {
            ExceptionUtil.throwSqlExecutionException("删除索引失败，找不到表id{}对应的索引块", tableId);
        }

        long startPos = columnBlock.getStartPos();
        long endPos = columnBlock.getStartPos() + TableIndexBlock.TABLE_COLUMN_BLOCK_SIZE;
        if (endPos >= fileAccessor.getEndPosition()) {
            fileAccessor.truncate(startPos);
        } else {
            int blockIndex = columnBlock.getBlockIndex();
            long oldNextStarPos = endPos;
            while (oldNextStarPos < fileAccessor.getEndPosition()) {
                ByteBuffer readBuffer = fileAccessor.read(oldNextStarPos, TableIndexBlock.TABLE_COLUMN_BLOCK_SIZE);
                TableIndexBlock block = new TableIndexBlock(readBuffer);
                block.setBlockIndex(blockIndex);

                fileAccessor.write(block.getByteBuffer(), startPos);
                startPos += TableIndexBlock.TABLE_COLUMN_BLOCK_SIZE;
                oldNextStarPos += TableIndexBlock.TABLE_COLUMN_BLOCK_SIZE;
                blockIndex++;
            }
            fileAccessor.truncate(startPos);
        }

        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("init error");
        }

    }


    public List<TableIndexBlock> getIndexBlockList() {
        return indexBlockList;
    }

    public TableIndexBlock getColumnBlock(Integer tableId) {
        return indexBlockMap.get(tableId);
    }

    public Map<Integer, TableIndexBlock> getIndexMap() {
        return indexBlockMap;
    }


    private TableIndexBlock getLastColumnBlock() {
        if (indexBlockList.size() > 0) {
            return indexBlockList.get(indexBlockList.size() - 1);
        } else {
            return null;
        }
    }


    private void init() throws IOException {
        this.indexBlockList = new ArrayList<>();
        this.indexBlockMap = new HashMap<>();

        // 初始化table的元数据文件，不存在会创建文件，并把所有表信息读取到内存
        String columnPath = filePath + File.separator + COLUMN_META_FILE_NAME;
        File dbFile = new File(columnPath);
        if (!dbFile.exists()) {
            dbFile.createNewFile();
        }
        fileAccessor = new FileAccessor(columnPath);
        long endPosition = fileAccessor.getEndPosition();
        if (endPosition >= TableIndexBlock.TABLE_COLUMN_BLOCK_SIZE) {
            long currPos = 0;
            while (currPos < endPosition) {
                ByteBuffer readBuffer = fileAccessor.read(currPos, TableIndexBlock.TABLE_COLUMN_BLOCK_SIZE);
                TableIndexBlock columnBlock = new TableIndexBlock(readBuffer);
                indexBlockList.add(columnBlock);
                currPos += TableIndexBlock.TABLE_COLUMN_BLOCK_SIZE;
            }

            for (TableIndexBlock columnBlock : indexBlockList) {
                indexBlockMap.put(columnBlock.getTableId(), columnBlock);
            }
        }
    }

    public void close() {
        if (fileAccessor != null) {
            fileAccessor.close();
        }
    }

}