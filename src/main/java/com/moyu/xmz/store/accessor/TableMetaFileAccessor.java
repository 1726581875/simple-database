package com.moyu.xmz.store.accessor;

import com.moyu.xmz.store.common.block.TableColumnBlock;
import com.moyu.xmz.store.common.meta.ColumnMetadata;
import com.moyu.xmz.store.common.meta.TableMetadata;
import com.moyu.xmz.common.constant.JavaTypeConstant;
import com.moyu.xmz.common.exception.ExceptionUtil;
import com.moyu.xmz.common.exception.SqlExecutionException;
import com.moyu.xmz.common.util.DataByteUtils;
import com.moyu.xmz.common.util.FileUtil;
import com.moyu.xmz.common.util.PathUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xiaomingzhang
 * @date 2023/5/8
 */
public class TableMetaFileAccessor {


    private static final String DEFAULT_META_PATH =  PathUtil.getMetaDirPath();

    private String filePath;

    public static final String TABLE_META_FILE_NAME = "table.meta";

    private FileAccessor fileAccessor;

    private Integer databaseId;

    private List<TableMetadata> tableMetadataList = new ArrayList<>();


    public TableMetaFileAccessor(Integer databaseId) throws IOException {
        this(databaseId, DEFAULT_META_PATH);
    }


    public TableMetaFileAccessor(Integer databaseId, String filePath) throws IOException {
        this.filePath = filePath;
        this.databaseId = databaseId;
        init();
    }


    public TableMetadata createTable(String tableName, String engineType) {
        TableMetadata metadata = null;
        synchronized (TableMetaFileAccessor.class) {
            checkTableName(tableName);
            TableMetadata lastData = getLastTable();
            int nextTableId = lastData == null ? 0 : lastData.getTableId() + 1;
            long startPos = lastData == null ? 0L : lastData.getStartPos() + lastData.getTotalByteLen();
            metadata = new TableMetadata(tableName, nextTableId, databaseId, startPos, null);
            metadata.setEngineType(engineType);
            ByteBuffer byteBuffer = metadata.getByteBuffer();
            fileAccessor.write(byteBuffer, startPos);
            tableMetadataList.add(metadata);
        }
        return metadata;
    }


    public TableMetadata dropTable(String tableName) {
        TableMetadata tableMetadata = null;
        int dropIndex = 0;
        for (int i = 0; i < tableMetadataList.size(); i++) {
            TableMetadata metadata = tableMetadataList.get(i);
            if (tableName.equals(metadata.getTableName())) {
                tableMetadata = metadata;
                dropIndex = i;
                break;
            }
        }

        if(tableMetadata == null) {
            ExceptionUtil.throwSqlExecutionException("表{}不存在", tableName);
        }

        long startPos = tableMetadata.getStartPos();
        long endPos = tableMetadata.getStartPos() + tableMetadata.getTotalByteLen();
        if(endPos >= fileAccessor.getEndPosition()) {
            fileAccessor.truncate(startPos);
        } else {
            long nextStartPos = endPos;
            while (nextStartPos < fileAccessor.getEndPosition()) {
                int dataByteLen = DataByteUtils.readInt(fileAccessor.read(nextStartPos, JavaTypeConstant.INT_LENGTH));
                ByteBuffer readBuffer = fileAccessor.read(nextStartPos, dataByteLen);
                TableMetadata metadata = new TableMetadata(readBuffer);
                metadata.setStartPos(startPos);
                fileAccessor.write(metadata.getByteBuffer(), startPos);
                startPos += dataByteLen;
                nextStartPos += dataByteLen;
            }
            fileAccessor.truncate(startPos);
        }

        tableMetadataList.remove(dropIndex);

        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return tableMetadata;
    }



    public List<TableMetadata> getCurrDbAllTable() {
        List<TableMetadata> currDbTables = new ArrayList<>();
        for (TableMetadata table : tableMetadataList) {
            if(databaseId.equals(table.getDatabaseId())) {
                currDbTables.add(table);
            }
        }
        return currDbTables;
    }

    public TableMetadata getTable(String tableName) {
        for (TableMetadata metadata : tableMetadataList) {
            if(databaseId.equals(metadata.getDatabaseId())
                    && metadata.getTableName().equals(tableName)) {
                return metadata;
            }
        }
        return null;
    }



    public List<ColumnMetadata> getColumnList(Integer tableId) {
        ColumnMetaFileAccessor columnMetaFileAccessor = null;
        try {
            columnMetaFileAccessor = new ColumnMetaFileAccessor(filePath);
            TableColumnBlock columnBlock = columnMetaFileAccessor.getColumnMap().get(tableId);
            if (columnBlock != null) {
                return columnBlock.getColumnMetadataList();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (columnMetaFileAccessor != null) {
                columnMetaFileAccessor.close();
            }
        }
        return new ArrayList<>();
    }


    private void checkTableName(String tableName) {
        for (TableMetadata metadata : tableMetadataList) {
            if (tableName.equals(metadata.getTableName())
                    && databaseId.equals(metadata.getDatabaseId())) {
                try {
                    init();
                    for (TableMetadata m : tableMetadataList) {
                        if (tableName.equals(m.getTableName()) && databaseId.equals(m.getDatabaseId())) {
                            System.out.println("表" + m.getTableName() + "已存在");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                throw new SqlExecutionException("表" + tableName + "已存在");
            }
        }
    }

    private TableMetadata getLastTable() {
        if (tableMetadataList.size() > 0) {
            return tableMetadataList.get(tableMetadataList.size() - 1);
        } else {
            return null;
        }
    }


    private void init() throws IOException {
        // 初始化table的元数据文件，不存在会创建文件，并把所有表信息读取到内存
        String basePath = filePath + File.separator + databaseId;
        FileUtil.createDirIfNotExists(basePath);

        String tableMetaPath = basePath + File.separator + TABLE_META_FILE_NAME;

        File dbFile = new File(tableMetaPath);
        if (!dbFile.exists()) {
            dbFile.createNewFile();
        }
        fileAccessor = new FileAccessor(tableMetaPath);
        long endPosition = fileAccessor.getEndPosition();
        tableMetadataList = new ArrayList<>();
        if (endPosition > JavaTypeConstant.INT_LENGTH) {
            long currPos = 0;
            while (currPos < endPosition) {
                int dataByteLen = DataByteUtils.readInt(fileAccessor.read(currPos, JavaTypeConstant.INT_LENGTH));
                ByteBuffer readBuffer = fileAccessor.read(currPos, dataByteLen);
                TableMetadata dbMetadata = new TableMetadata(readBuffer);
                tableMetadataList.add(dbMetadata);
                currPos += dataByteLen;
            }
        }
    }

    public void close() {
        if (fileAccessor != null) {
            fileAccessor.close();
        }
    }




}