package com.moyu.test.command.dml;

import com.moyu.test.command.AbstractCommand;
import com.moyu.test.command.condition.ConditionComparator;
import com.moyu.test.command.condition.ConditionTree;
import com.moyu.test.store.data.DataChunk;
import com.moyu.test.store.data.DataChunkStore;
import com.moyu.test.store.data.RowData;
import com.moyu.test.store.metadata.obj.Column;
import com.moyu.test.util.PathUtil;

import java.util.List;

/**
 * @author xiaomingzhang
 * @date 2023/5/18
 */
public class DeleteCommand extends AbstractCommand {

    private static final String FILE_PATH = PathUtil.getBaseDirPath();

    private String tableName;

    private Column[] columns;

    private ConditionTree conditionTree;


    public DeleteCommand(String tableName, Column[] columns, ConditionTree conditionTree) {
        this.tableName = tableName;
        this.columns = columns;
        this.conditionTree = conditionTree;
    }

    @Override
    public String execute() {
        int deleteRowNum = 0;
        DataChunkStore dataChunkStore = null;
        try {
            String fileFullPath = FILE_PATH + tableName + ".d";
            dataChunkStore = new DataChunkStore(fileFullPath);
            int dataChunkNum = dataChunkStore.getDataChunkNum();
            // 遍历数据块
            for (int i = 0; i < dataChunkNum; i++) {
                DataChunk chunk = dataChunkStore.getChunk(i);
                if (chunk == null) {
                    break;
                }
                // delete语句没有带任何条件，直接清空块内所有数据
                if(conditionTree == null) {
                    deleteRowNum += chunk.getRowNum();
                    // 清空块内所有行数据
                    chunk.clear();
                } else {
                    // 带条件，按条件筛选符号条件的行并且进行移除
                    List<RowData> dataRowList = chunk.getDataRowList();
                    if (dataRowList == null || dataRowList.size() == 0) {
                        continue;
                    }

                    int index = dataRowList.size() - 1;
                    do {
                        RowData rowData = dataRowList.get(index);
                        Column[] columnData = rowData.getColumnData(columns);
                        boolean compareResult = ConditionComparator.analyzeConditionTree(conditionTree, columnData);
                        // 只移除符号条件的行
                        if (compareResult) {
                            chunk.removeRow(index);
                            deleteRowNum++;
                        }
                        index--;
                    } while (index >= 0);

                }
                // 更新块整个数据块到磁盘
                dataChunkStore.updateChunk(chunk);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataChunkStore.close();
        }

        return "共删除了" + deleteRowNum + "行数据";
    }




}