package com.moyu.xmz.store.transaction;

import com.moyu.xmz.store.common.SerializableByte;
import com.moyu.xmz.store.common.meta.RowMeta;
import com.moyu.xmz.common.util.DataByteUtils;

import java.nio.ByteBuffer;

/**
 * @author xiaomingzhang
 * @date 2023/6/13
 */
public class RowLogRecord implements SerializableByte {

    public static final byte TYPE_INSERT = 0;
    public static final byte TYPE_UPDATE = 1;
    public static final byte TYPE_DELETE = 2;

    /**
     * 占用字节长度
     */
    private int totalByteLen;
    /**
     * 开始位置
     */
    private long startPos;
    /**
     * 事务id
     */
    private int transactionId;

    /**
     * 数据库id
     */
    private int databaseId;
    /**
     * 所属表名
     */
    private String tableName;
    /**
     * 数据块位置
     */
    private long blockPos;
    /**
     * 数据行id
     */
    private long rowId;
    /**
     * 版本
     */
    private int version;

    /**
     * 操作类型
     * 0新增、1修改、2删除
     */
    private byte type;

    /**
     * 旧数据
     */
    private RowMeta oldRow;


    public RowLogRecord(String tableName, RowMeta oldRow, byte type) {
        this.tableName = tableName;
        this.oldRow = oldRow;
        this.type = type;
        int rowLen = this.oldRow == null ? 0 : (int)this.oldRow.getTotalByteLen();
        // tableName + oldRow length + 4 int + 3 long + 1 byte
        this.totalByteLen = (4 + this.tableName.length() * 3) + rowLen + (4 * 4) + (3 * 8) + 1;
    }

    public RowLogRecord(ByteBuffer byteBuffer) {
        this.totalByteLen = DataByteUtils.readInt(byteBuffer);
        this.startPos = DataByteUtils.readLong(byteBuffer);
        this.transactionId = DataByteUtils.readInt(byteBuffer);
        this.databaseId = DataByteUtils.readInt(byteBuffer);
        int tableNameLen = DataByteUtils.readInt(byteBuffer);
        this.tableName = DataByteUtils.readString(byteBuffer, tableNameLen);
        this.blockPos = DataByteUtils.readLong(byteBuffer);
        this.rowId = DataByteUtils.readLong(byteBuffer);
        this.version = DataByteUtils.readInt(byteBuffer);
        this.type = byteBuffer.get();
        if (this.type != RowLogRecord.TYPE_INSERT) {
            this.oldRow = new RowMeta(byteBuffer);
        }
    }

    @Override
    public ByteBuffer getByteBuffer() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(this.totalByteLen);
        DataByteUtils.writeInt(byteBuffer, this.totalByteLen);
        DataByteUtils.writeLong(byteBuffer, this.startPos);
        DataByteUtils.writeInt(byteBuffer, this.transactionId);
        DataByteUtils.writeInt(byteBuffer, this.databaseId);

        DataByteUtils.writeInt(byteBuffer, this.tableName.length());
        DataByteUtils.writeStringData(byteBuffer, this.tableName, this.tableName.length());

        DataByteUtils.writeLong(byteBuffer, this.blockPos);
        DataByteUtils.writeLong(byteBuffer, this.rowId);
        DataByteUtils.writeInt(byteBuffer, this.version);
        byteBuffer.put(this.type);
        if (this.type != RowLogRecord.TYPE_INSERT) {
            byteBuffer.put(this.oldRow.getByteBuff());
        }
        // 获取真实长度
        this.totalByteLen = byteBuffer.position();
        byteBuffer.putInt(0, this.totalByteLen);
        byteBuffer.flip();
        return byteBuffer;
    }


    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public long getBlockPos() {
        return blockPos;
    }

    public void setBlockPos(long blockPos) {
        this.blockPos = blockPos;
    }

    public long getRowId() {
        return rowId;
    }

    public void setRowId(long rowId) {
        this.rowId = rowId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public RowMeta getOldRow() {
        return oldRow;
    }

    public void setOldRow(RowMeta oldRow) {
        this.oldRow = oldRow;
    }

    public Integer getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(Integer databaseId) {
        this.databaseId = databaseId;
    }

    public int getTotalByteLen() {
        return totalByteLen;
    }

    public void setTotalByteLen(int totalByteLen) {
        this.totalByteLen = totalByteLen;
    }

    public long getStartPos() {
        return startPos;
    }

    public void setStartPos(long startPos) {
        this.startPos = startPos;
    }

    public byte getType() {
        return type;
    }

}
