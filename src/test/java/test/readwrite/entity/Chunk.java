package test.readwrite.entity;

import com.moyu.xmz.common.util.DataByteUtils;

import java.nio.ByteBuffer;

/**
 * @author xiaomingzhang
 * @date 2023/4/26
 */
public class Chunk {
    /**
     * 4字节
     */
    private int chunkLen;
    /**
     * 8字节
     */
    private long chunkStartPos;
    /**
     * 下一个数据块开始位置，8字节
     */
    private long nextChunkPos;
    /**
     * 数据字节长度，4字节
     */
    private int dataLen;
    /**
     * 数据字符长度，4字节
     */
    private int charLen;

    private String data;


    public Chunk(long chunkStartPos, String data) {
        this.chunkStartPos = chunkStartPos;
        this.charLen = data.length();
        this.dataLen = DataByteUtils.getDateStringByteLength(data);
        // chunkStartPos + chunkLen + nextChunkPos + dataLen + charLen = 28 再加上data的字节长度
        this.chunkLen = 28 + dataLen;
        this.nextChunkPos = chunkStartPos + chunkLen;
        this.data = data;
    }


    public Chunk(ByteBuffer readBuff) {
        this.chunkLen = DataByteUtils.readInt(readBuff);
        this.chunkStartPos = DataByteUtils.readLong(readBuff);
        this.nextChunkPos = DataByteUtils.readLong(readBuff);
        this.dataLen = DataByteUtils.readInt(readBuff);
        this.charLen = DataByteUtils.readInt(readBuff);
        this.data = DataByteUtils.readString(readBuff, charLen);
    }


    public ByteBuffer getByteBuff() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(chunkLen);
        DataByteUtils.writeInt(byteBuffer, chunkLen);
        DataByteUtils.writeLong(byteBuffer, chunkStartPos);
        DataByteUtils.writeLong(byteBuffer, nextChunkPos);
        DataByteUtils.writeInt(byteBuffer, dataLen);
        DataByteUtils.writeInt(byteBuffer, charLen);
        DataByteUtils.writeStringData(byteBuffer, data, data.length());
        byteBuffer.rewind();
        return byteBuffer;
    }


    public int getChunkLen() {
        return chunkLen;
    }

    public void setChunkLen(int chunkLen) {
        this.chunkLen = chunkLen;
    }

    public long getChunkStartPos() {
        return chunkStartPos;
    }

    public void setChunkStartPos(long chunkStartPos) {
        this.chunkStartPos = chunkStartPos;
    }

    public long getNextChunkPos() {
        return nextChunkPos;
    }

    public void setNextChunkPos(long nextChunkPos) {
        this.nextChunkPos = nextChunkPos;
    }

    public int getDataLen() {
        return dataLen;
    }

    public void setDataLen(int dataLen) {
        this.dataLen = dataLen;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "chunkLen=" + chunkLen +
                ", chunkStartPos=" + chunkStartPos +
                ", nextChunkPos=" + nextChunkPos +
                ", dataLen=" + dataLen +
                ", data='" + data + '\'' +
                '}';
    }
}
