package com.moyu.xmz.store;

import com.moyu.xmz.command.dml.expression.Expression;
import com.moyu.xmz.command.dml.plan.SelectIndex;
import com.moyu.xmz.command.dml.sql.QueryTable;
import com.moyu.xmz.common.exception.ExceptionUtil;
import com.moyu.xmz.store.common.dto.TableInfo;
import com.moyu.xmz.store.cursor.BTreeIndexCursor;
import com.moyu.xmz.store.cursor.BtreeCursor;
import com.moyu.xmz.store.cursor.Cursor;
import com.moyu.xmz.store.cursor.RowEntity;
import com.moyu.xmz.store.tree.BTreeMap;
import com.moyu.xmz.store.tree.BTreeStore;
import com.moyu.xmz.store.tree.Page;
import com.moyu.xmz.store.type.DataType;
import com.moyu.xmz.store.type.dbtype.AbstractDbType;
import com.moyu.xmz.store.type.dbtype.LongType;
import com.moyu.xmz.store.type.obj.ArrayDataType;
import com.moyu.xmz.store.type.obj.RowDataType;
import com.moyu.xmz.store.type.value.ArrayValue;
import com.moyu.xmz.store.type.value.LongValue;
import com.moyu.xmz.store.type.value.RowValue;
import com.moyu.xmz.store.type.value.Value;
import com.moyu.xmz.common.exception.DbException;
import com.moyu.xmz.store.accessor.IndexMetaAccessor;
import com.moyu.xmz.store.common.dto.Column;
import com.moyu.xmz.store.common.meta.IndexMeta;
import com.moyu.xmz.common.util.FileUtils;
import com.moyu.xmz.common.util.PathUtils;
import com.moyu.xmz.common.util.TypeConvertUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xiaomingzhang
 * @date 2023/7/1
 */
public class YanEngine extends StoreEngine {


    public YanEngine(TableInfo tableInfo) {
        super(tableInfo.getSession(), tableInfo.getTableName(), tableInfo.getTableColumns());
        super.allIndexList = tableInfo.getAllIndexList();
    }


    @Override
    public int insert(RowEntity rowEntity) {

        BTreeMap bTreeMap = null;
        try {
            bTreeMap = getBTreeMap();
            Column primaryKey = getPrimaryKey(rowEntity.getColumns());
            long nextRowId = bTreeMap.getNextRowId();
            rowEntity.setRowId(nextRowId);
            RowValue rowValue = new RowValue(0L, rowEntity.getColumns(), nextRowId);
            // 如果不存在主键，以行id作为b+树的key
            if (primaryKey == null) {
                bTreeMap.put(nextRowId, rowValue);
            } else {
                bTreeMap.put(primaryKey.getValue(), rowValue);
            }
            // 如果存在索引插入到对应索引树
            insertIndexTree(allIndexList, rowEntity, primaryKey);
        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.buildDbException("插入数据异常");
        } finally {
            if(bTreeMap != null) {
                bTreeMap.close();
            }
        }
        return 1;
    }

    @Override
    public int batchFastInsert(List<RowEntity> rowList) {
        BTreeMap bTreeMap = null;
        try {
            bTreeMap = getBTreeMap();
            for (RowEntity rowEntity : rowList) {
                Column primaryKey = getPrimaryKey(rowEntity.getColumns());
                long nextRowId = bTreeMap.getNextRowId();
                RowValue rowValue = new RowValue(0L, rowEntity.getColumns(), nextRowId);
                // 如果不存在主键，以行id作为b+树的key
                if (primaryKey == null) {
                    bTreeMap.put(nextRowId, rowValue);
                } else {
                    bTreeMap.put(primaryKey.getValue(), rowValue);
                }

                // 如果存在索引插入到对应索引树
                insertIndexTree(allIndexList, rowEntity, primaryKey);

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.buildDbException("插入数据异常");
        } finally {
            bTreeMap.close();
        }
        return 1;
    }


    private void insertIndexTree(List<IndexMeta> indexList, RowEntity row, Column primaryKey) {
        // 插入索引
        if (indexList != null && indexList.size() > 0) {
            for (IndexMeta index : indexList) {
                Column indexColumn = getIndexColumn(index.getColumnName(), row.getColumns());
                if (indexColumn != null && indexColumn.getValue() != null) {
                    insertIndexValue(index, row, primaryKey);
                }
            }
        }
    }



    private Column getPrimaryKey(Column[] columns) {
        for (Column column : columns) {
            if (column.getIsPrimaryKey() == (byte) 1) {
                return column;
            }
        }
        return null;
    }


    @Override
    public int update(Column[] updateColumns, Expression condition) {
        int updateNum = 0;
        BTreeMap bTreeMap = null;
        try {
            bTreeMap = getBTreeMap();
            Page page = bTreeMap.getFirstLeafPage();
            while (page != null) {
                List<RowValue> valueList = page.getValueList();
                int i = 0;
                while (i < valueList.size()) {
                    RowValue rowValue = valueList.get(i);
                    RowEntity rowEntity = rowValue.getRowEntity(tableColumns);
                    if (Expression.isMatch(rowEntity, condition)) {
                        // 更新数据
                        Column[] columns = rowEntity.getColumns();
                        for (Column updateColumn : updateColumns) {
                            columns[updateColumn.getColumnIndex()].setValue(updateColumn.getValue());
                        }
                        rowValue.setColumns(columns);
                        updateNum++;
                    }
                    i++;
                }

                page.commit();

                Long rightPos = page.getRightPos();
                if (rightPos == null || rightPos < 0) {
                    break;
                }
                page = bTreeMap.getPageByPos(rightPos);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.buildDbException("更新数据异常");
        } finally {
            if(bTreeMap != null) {
                bTreeMap.close();
            }
        }
        return updateNum;
    }

    @Override
    public int delete(Expression condition) {
        int deleteNum = 0;
        BTreeMap bTreeMap = null;
        try {
            bTreeMap = getBTreeMap();
            Page page = bTreeMap.getFirstLeafPage();
            while (page != null) {
                List<RowValue> valueList = page.getValueList();
                int i = 0;
                while (i < valueList.size()) {
                    RowValue rowValue = valueList.get(i);
                    RowEntity rowEntity = rowValue.getRowEntity(tableColumns);
                    if (Expression.isMatch(rowEntity, condition)) {
                        rowValue.setIsDeleted((byte) 1);
                        deleteNum++;

                        // 删除索引项
                        Column primaryKey = getPrimaryKey(rowEntity.getColumns());
                        if (allIndexList != null && allIndexList.size() > 0) {
                            for (IndexMeta index : allIndexList) {
                                Column indexColumnValue = getIndexColumn(index.getColumnName(), rowEntity.getColumns());
                                if (indexColumnValue != null && indexColumnValue.getValue() != null) {
                                    removeIndexItemValue(index, rowEntity, primaryKey);
                                }
                            }
                        }
                    }
                    i++;
                }
                page.commit();

                Long rightPos = page.getRightPos();
                if (rightPos == null || rightPos < 0) {
                    break;
                }
                page = bTreeMap.getPageByPos(rightPos);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.buildDbException("删除数据异常");
        } finally {
            if(bTreeMap != null) {
                bTreeMap.close();
            }
        }
        return deleteNum;
    }

    @Override
    public void createIndex(Integer tableId, String indexName, String columnName, byte indexType) {
        IndexMetaAccessor indexMetaAccessor = null;
        BTreeStore bTreeIndexStore = null;
        try {
            indexMetaAccessor = new IndexMetaAccessor(session.getDatabase().getDatabaseId());
            IndexMeta oldIndex = indexMetaAccessor.getIndex(tableId, indexName);
            // 存在则先删除索引元数据
            if (oldIndex != null) {
                indexMetaAccessor.dropIndexMetadata(tableId, indexName);
            }
            // 保存索引元数据
            IndexMeta index = new IndexMeta(0L, tableId, indexName, columnName, indexType);
            indexMetaAccessor.saveIndexMetadata(tableId, index);

            // 索引路径
            String indexPath = getIndexPath(indexName);
            // 索引文件存在则先删除
            File file = new File(indexPath);
            if (file.exists()) {
                file.delete();
            }
            // 创建索引文件
            FileUtils.createFileIfNotExists(indexPath);

            // 获取b-tree的键类型
            Column indexColumn = getIndexColumn(columnName, tableColumns);
            bTreeIndexStore = new BTreeStore(indexPath);
            DataType keyDataType = AbstractDbType.getDataType(indexColumn.getColumnType());

            BTreeMap bTreeIndexMap = new BTreeMap(keyDataType, new ArrayDataType(), bTreeIndexStore, false);

            // 遍历所有数据，构建b+树
            BTreeMap bTreeMap = getBTreeMap();
            Cursor cursor = new BtreeCursor(tableColumns, bTreeMap);
            RowEntity row = null;
            while ((row = cursor.next()) != null) {
                Column indexColumnValue = getIndexColumn(columnName, row.getColumns());
                ArrayValue keyArrayValue = (ArrayValue) bTreeIndexMap.get(indexColumnValue.getValue());

                Column primaryKey = getPrimaryKey(row.getColumns());
                // 有主键则使用主键作为b-tree叶子节点的值，没有则使用行id作为值
                Value value = primaryKey != null ? getIndexValueObject(primaryKey) : new LongValue(row.getRowId());
                DataType valueArrItemType = primaryKey != null
                        ? AbstractDbType.getDataType(primaryKey.getColumnType()) : new LongType();
                // 把一级索引的键插入叶子节点作为二级索引的值
                keyArrayValue = insertNodeArray(value, valueArrItemType, keyArrayValue);
                bTreeIndexMap.putUnSaveDisk(indexColumnValue.getValue(), keyArrayValue);
            }
            bTreeIndexMap.commitSaveDisk();
        } catch (Exception e) {
            e.printStackTrace();
            throw new DbException("创建索引发生异常");
        } finally {
            if(indexMetaAccessor != null) {
                indexMetaAccessor.close();
            }
            if(bTreeIndexStore != null) {
                bTreeIndexStore.close();
            }
        }
    }



    private void insertIndexValue(IndexMeta index, RowEntity row, Column primaryKey) {
        BTreeStore bTreeIndexStore = null;
        try {
            // 索引路径
            String indexPath = getIndexPath(index.getIndexName());
            Column indexColumn = getIndexColumn(index.getColumnName(), tableColumns);
            bTreeIndexStore = new BTreeStore(indexPath);
            DataType keyDataType = AbstractDbType.getDataType(indexColumn.getColumnType());
            BTreeMap bTreeIndexMap = new BTreeMap(keyDataType, new ArrayDataType(), bTreeIndexStore, true);
            // 有主键则使用主键作为b-tree叶子节点的值，没有则使用行id作为值
            Value value = primaryKey != null ? getIndexValueObject(primaryKey) : new LongValue(row.getRowId());
            DataType valueArrItemType = primaryKey != null
                    ? AbstractDbType.getDataType(primaryKey.getColumnType()) : new LongType();

            Column indexColumnValue = getIndexColumn(index.getColumnName(), row.getColumns());
            ArrayValue keyArrayValue = (ArrayValue) bTreeIndexMap.get(indexColumnValue.getValue());

            keyArrayValue = insertNodeArray(value, valueArrItemType, keyArrayValue);
            bTreeIndexMap.put(indexColumnValue.getValue(), keyArrayValue);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(bTreeIndexStore != null) {
                bTreeIndexStore.close();
            }
        }
    }


    private void removeIndexItemValue(IndexMeta index, RowEntity row, Column primaryKey) {
        BTreeStore bTreeIndexStore = null;
        try {
            // 索引路径
            String indexPath = getIndexPath(index.getIndexName());
            Column indexColumn = getIndexColumn(index.getColumnName(), tableColumns);
            bTreeIndexStore = new BTreeStore(indexPath);
            DataType keyDataType = AbstractDbType.getDataType(indexColumn.getColumnType());
            BTreeMap bTreeIndexMap = new BTreeMap(keyDataType, new ArrayDataType(), bTreeIndexStore, true);
            // 有主键则使用主键作为b-tree叶子节点的值，没有则使用行id作为值
            Value value = primaryKey != null ? getIndexValueObject(primaryKey) : new LongValue(row.getRowId());
            DataType valueArrItemType = primaryKey != null
                    ? AbstractDbType.getDataType(primaryKey.getColumnType()) : new LongType();

            Column indexColumnValue = getIndexColumn(index.getColumnName(), row.getColumns());
            ArrayValue keyArrayValue = (ArrayValue) bTreeIndexMap.get(indexColumnValue.getValue());

            if (keyArrayValue != null) {
                Value[] arr = keyArrayValue.getArr();
                List<Value> valueList = new ArrayList<>(arr.length);
                for (Value v : arr) {
                    if(v.compare(value) != 0) {
                        valueList.add(v);
                    }
                }
                keyArrayValue = new ArrayValue<>(valueList.toArray(new Value[0]), valueArrItemType);
            }
            bTreeIndexMap.put(indexColumnValue.getValue(), keyArrayValue);
        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.buildDbException("删除索引数据异常");
        } finally {
            if(bTreeIndexStore != null) {
                bTreeIndexStore.close();
            }
        }
    }


    private Column getIndexColumn(String columnName, Column[] columns) {
        for (Column c : columns) {
            if (columnName.equals(c.getColumnName())) {
                return c;
            }
        }
        return null;
    }


    @Override
    public Cursor getQueryCursor(QueryTable table) throws IOException {
        Cursor cursor = null;
        BTreeMap clusteredIndexMap = getBTreeMap();
        if (table.getSelectIndex() == null) {
            System.out.println("不用索引，表:" + table.getTableName() + ",存储引擎:" + table.getEngineType());
            cursor = new BtreeCursor(table.getTableColumns(), clusteredIndexMap);
        } else {
            System.out.println("使用索引查询，索引:" + table.getSelectIndex().getIndexName()
                    + ",表:" + table.getTableName() + ",存储引擎:" + table.getEngineType());

            // todo 根据索引类型进行不同操作，如果是主键索引不需要进行回表操作

            ArrayValue keyArrValue = null;
            SelectIndex selectIndex = table.getSelectIndex();
            Column indexColumn = selectIndex.getIndexColumn();
            String indexPath = getIndexPath(selectIndex.getIndexName());
            BTreeStore bTreeIndexStore = null;
            try {
                bTreeIndexStore = new BTreeStore(indexPath);
                DataType keyDataType = AbstractDbType.getDataType(selectIndex.getIndexColumn().getColumnType());
                BTreeMap bTreeIndexMap = new BTreeMap(keyDataType, new ArrayDataType(), bTreeIndexStore, true);
                Object value = TypeConvertUtils.convertValueType(String.valueOf(indexColumn.getValue()), indexColumn.getColumnType());
                keyArrValue = (ArrayValue) bTreeIndexMap.get(value);
            } catch (Exception e) {
                e.printStackTrace();
                throw ExceptionUtil.buildDbException("发生数据库内部异常");
            } finally {
                if (bTreeIndexStore != null) {
                    bTreeIndexStore.close();
                }
            }
            Object[] keyObjArray = null;
            Value[] keyArr = null;
            if (keyArrValue != null) {
                keyArr = keyArrValue.getArr();
                keyObjArray = new Object[keyArr.length];
                for (int i = 0; i < keyArr.length; i++) {
                    keyObjArray[i] = keyArr[i].getObjValue();
                }
            }
            cursor = new BTreeIndexCursor(table.getTableColumns(), clusteredIndexMap, keyObjArray);
        }
        return cursor;
    }


    private String getIndexPath(String indexName) {
        String dirPath = PathUtils.getBaseDirPath() + File.separator + this.session.getDatabaseId();
        String indexPath = dirPath + File.separator + tableName + "_" + indexName + ".idx";
        return indexPath;
    }


    private BTreeMap getBTreeMap() throws IOException {
        return getBTreeMap(this.tableName);
    }

    private BTreeMap getBTreeMap(String tableName) throws IOException {
        BTreeStore bTreeStore = new BTreeStore(PathUtils.getYanEngineDataFilePath(session.getDatabaseId(), tableName));
        BTreeMap bTreeMap = null;
        try {
            Column primaryKey = getPrimaryKey(tableColumns);
            if (primaryKey == null) {
                bTreeMap = new BTreeMap(new LongType(), new RowDataType(), bTreeStore, true);
            } else {
                DataType primaryType = AbstractDbType.getDataType(primaryKey.getColumnType());
                bTreeMap = new BTreeMap(primaryType, new RowDataType(), bTreeStore, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            bTreeStore.close();
        }
        return bTreeMap;
    }

    @Override
    public boolean addOrDropColumnResetData(Column defColumn, Column[] newColumns, boolean isAdd) {

        boolean isSuccess = false;
        BTreeMap originalTable = null;
        BtreeCursor originalCursor = null;
        BTreeMap temTable = null;
        String tmpTableName = this.tableName + "_temp" + System.currentTimeMillis();
        try {
            originalTable = getBTreeMap();
            temTable = getBTreeMap(tmpTableName);
            originalCursor = new BtreeCursor(this.tableColumns, originalTable);
            RowEntity rowEntity = originalCursor.next();
            while (rowEntity != null) {
                Column primaryKey = getPrimaryKey(rowEntity.getColumns());
                if(!rowEntity.isDeleted()) {
                    for (Column c : newColumns) {
                        Column vColumn = rowEntity.getColumn(c.getColumnName());
                        if(vColumn != null) {
                            c.setValue(vColumn.getValue());
                        } else {
                            if(isAdd) {
                                if (c.getColumnName().equals(defColumn.getColumnName())) {
                                    c.setValue(defColumn.getValue());
                                } else {
                                    ExceptionUtil.throwDbException("复制值失败，字段{}找不到", c.getColumnName());
                                }
                            } else {
                                ExceptionUtil.throwDbException("复制值失败，字段{}找不到", c.getColumnName());
                            }
                        }
                    }
                    RowValue rowValue = new RowValue(0L, newColumns, rowEntity.getRowId());
                    // 如果不存在主键，以行id作为b+树的key
                    if (primaryKey == null) {
                        temTable.put(rowEntity.getRowId(), rowValue);
                    } else {
                        temTable.put(primaryKey.getValue(), rowValue);
                    }
                }
                rowEntity = originalCursor.next();
            }
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.buildDbException("发生数据库内部异常");
        } finally {
            if(originalTable != null) {
                originalTable.close();
            }
            if(temTable != null) {
                temTable.close();
            }
            if(originalCursor != null) {
                originalCursor.close();
            }
        }

        if(isSuccess) {
            isSuccess = false;
            String originalFilePath = PathUtils.getYanEngineDataFilePath(session.getDatabaseId(), tableName);
            String temFilePath = PathUtils.getYanEngineDataFilePath(session.getDatabaseId(), tmpTableName);
            File originalFile = new File(originalFilePath);

            String bakOriginalPath = originalFilePath + "_bak";
            File bakFile = new File(bakOriginalPath);
            if(bakFile.exists()) {
                bakFile.delete();
            }
            // 备份原始的数据文件
            boolean r1 = originalFile.renameTo(bakFile);
            if(r1) {
                // 临时文件（新的数据）文件命名为正式文件
                File temFile = new File(temFilePath);
                boolean r2 = temFile.renameTo(new File(originalFilePath));
                if(r2) {
                    // 删除备份
                    bakFile.deleteOnExit();
                    isSuccess = true;
                } else {
                    // 还原数据文件
                    System.out.println("修改表字段，文件重命名失败");
                    boolean r3 = bakFile.renameTo(new File(originalFilePath));
                    if(!r3) {
                        System.out.println("恢复失败:bakOriginalPath:" + bakOriginalPath + ", originalFilePath:" + originalFilePath);
                    }
                }
                temFile.deleteOnExit();
            } else {
                System.out.println("修改字段失败，备份失败");
            }
        }

        return isSuccess;

    }
}
