package com.moyu.test.net.model.terminal;
import com.moyu.test.net.model.BaseResultDto;
import com.moyu.test.net.util.ReadWriteUtil;
import com.moyu.test.store.WriteBuffer;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author xiaomingzhang
 * @date 2023/9/10
 */
public class QueryResultDto implements BaseResultDto {

    private int totalByteLen;

    private int columnsLen;

    private int rowsLen;

    private byte hasNext;

    private ColumnMetaDto[] columns;

    private RowDto[] rows;

    /**
     * 描述
     */
    private String desc;



    public QueryResultDto(ColumnMetaDto[] columns, RowDto[] rows, String desc) {
        this.totalByteLen = 0;
        this.columnsLen = columns.length;
        this.columns = columns;
        if (rows == null) {
            this.rowsLen = 0;
        } else {
            this.rowsLen = rows.length;
        }
        this.rows = rows;
        this.desc = desc;
    }

    public QueryResultDto(ByteBuffer byteBuffer) {
        this.totalByteLen = byteBuffer.getInt();
        this.columnsLen = byteBuffer.getInt();
        this.rowsLen = byteBuffer.getInt();
        this.hasNext = byteBuffer.get();
        this.columns = new ColumnMetaDto[columnsLen];
        for (int i = 0; i < columnsLen; i++) {
            this.columns[i] = new ColumnMetaDto(byteBuffer);
        }
        this.rows = new RowDto[rowsLen];
        for (int i = 0; i < rowsLen; i++) {
            this.rows[i] = new RowDto(byteBuffer, this.columns);
        }
        this.desc = ReadWriteUtil.readString(byteBuffer);
    }


    @Override
    public ByteBuffer getByteBuffer() {
        WriteBuffer writeBuffer = new WriteBuffer(128);
        writeBuffer.putInt(totalByteLen);
        writeBuffer.putInt(columnsLen);
        writeBuffer.putInt(rowsLen);
        writeBuffer.put(this.hasNext);
        for (ColumnMetaDto columnDto : columns) {
            writeBuffer.put(columnDto.getByteBuffer());
        }
        if (rowsLen > 0) {
            for (RowDto rowValueDto : rows) {
                writeBuffer.put(rowValueDto.getByteBuffer(columns));
            }
        }
        ReadWriteUtil.writeString(writeBuffer, desc);
        totalByteLen = writeBuffer.position();
        writeBuffer.putInt(0, totalByteLen);
        ByteBuffer buffer = writeBuffer.getBuffer();
        buffer.flip();
        return buffer;
    }


    public QueryResultDto(int totalByteLen, int columnsLen, int rowsLen, ColumnMetaDto[] columns, RowDto[] rows, String desc) {
        this.totalByteLen = totalByteLen;
        this.columnsLen = columnsLen;
        this.rowsLen = rowsLen;
        this.columns = columns;
        this.rows = rows;
        this.desc = desc;
    }


    public ColumnMetaDto[] getColumns() {
        return columns;
    }

    public RowDto[] getRows() {
        return rows;
    }

    public String getDesc() {
        return desc;
    }

    public int getColumnsLen() {
        return columnsLen;
    }

    public int getRowsLen() {
        return rowsLen;
    }

    public void setHasNext(byte hasNext) {
        this.hasNext = hasNext;
    }

    public byte getHasNext() {
        return hasNext;
    }

    @Override
    public String toString() {
        return "QueryResultDto{" +
                "totalByteLen=" + totalByteLen +
                ", columnsLen=" + columnsLen +
                ", rowsLen=" + rowsLen +
                ", columns=" + Arrays.toString(columns) +
                ", rows=" + Arrays.toString(rows) +
                ", desc='" + desc + '\'' +
                '}';
    }

}
