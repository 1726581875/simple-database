package test.readwrite.entity;

import com.moyu.xmz.common.util.DataByteUtils;

import java.nio.ByteBuffer;

/**
 * @author xiaomingzhang
 * @date 2023/4/28
 */
public class FileHeader {

    /**
     * 第一个数据块的开始位置
     */
    private long firstChunkStartPos;
    /**
     * 最后一个数据块的开始位置
     */
    private long lastChunkStartPos;

    /**
     * 数据块的数量
     */
    private int totalChunkNum;
    /**
     * 文件最后位置
     */
    private long fileEndPos;


    /**
     *  3个long + 1个int = 28 byte
     */
    public static final int  HEADER_LENGTH = 28;



    public FileHeader(long firstChunkStartPos, long lastChunkStartPos, int totalChunkNum, long fileEndPos) {
        this.firstChunkStartPos = firstChunkStartPos;
        this.lastChunkStartPos = lastChunkStartPos;
        this.totalChunkNum = totalChunkNum;
        this.fileEndPos = fileEndPos;
    }


    public FileHeader(ByteBuffer byteBuffer) {
        this.firstChunkStartPos = DataByteUtils.readLong(byteBuffer);
        this.lastChunkStartPos = DataByteUtils.readLong(byteBuffer);
        this.totalChunkNum = DataByteUtils.readInt(byteBuffer);
        this.fileEndPos = DataByteUtils.readLong(byteBuffer);
    }



    public ByteBuffer getByteBuff() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_LENGTH);
        DataByteUtils.writeLong(byteBuffer, firstChunkStartPos);
        DataByteUtils.writeLong(byteBuffer, lastChunkStartPos);
        DataByteUtils.writeInt(byteBuffer, totalChunkNum);
        DataByteUtils.writeLong(byteBuffer, fileEndPos);
        byteBuffer.rewind();
        return byteBuffer;
    }




    public long getFirstChunkStartPos() {
        return firstChunkStartPos;
    }

    public void setFirstChunkStartPos(long firstChunkStartPos) {
        this.firstChunkStartPos = firstChunkStartPos;
    }

    public long getLastChunkStartPos() {
        return lastChunkStartPos;
    }

    public void setLastChunkStartPos(long lastChunkStartPos) {
        this.lastChunkStartPos = lastChunkStartPos;
    }

    public int getTotalChunkNum() {
        return totalChunkNum;
    }

    public void setTotalChunkNum(int totalChunkNum) {
        this.totalChunkNum = totalChunkNum;
    }

    public long getFileEndPos() {
        return fileEndPos;
    }

    public void setFileEndPos(long fileEndPos) {
        this.fileEndPos = fileEndPos;
    }
}
