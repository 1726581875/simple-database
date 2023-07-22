package com.moyu.test.store;

import com.moyu.test.exception.FileOperationException;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author xiaomingzhang
 * @date 2023/4/21
 */
public class FileStore {

    private String fileFullPath;

    private FileChannel fileChannel;

    private RandomAccessFile randomAccessFile;

    private long endPosition;


    public FileStore(String fileFullPath) throws IOException {
        this.fileFullPath = fileFullPath;
        randomAccessFile = new RandomAccessFile(this.fileFullPath, "rw");
        fileChannel = randomAccessFile.getChannel();
        this.endPosition = fileChannel.size();
    }


    /**
     * 文件指定位置读取
     * @param pos 开始位置
     * @param len 读取的长度
     * @return
     */
    public ByteBuffer read(long pos, int len) {
        ByteBuffer dst = ByteBuffer.allocate(len);
        try {
            do {
                int l = fileChannel.read(dst, pos);
                if (l < 0) {
                    throw new EOFException();
                }
                pos += l;
            } while (dst.remaining() > 0);
            dst.rewind();
        } catch (IOException e) {
            e.printStackTrace();
            throw new FileOperationException("读文件发生异常");
        }
        return dst;
    }


    /**
     * 文件指定位置写入
     * @param byteBuffer 写入内容字节缓存数组
     * @param startPos 写入开始位置
     */
    public void write(ByteBuffer byteBuffer, long startPos) {
        try {
            int off = 0;
            do {
                int len = fileChannel.write(byteBuffer, startPos + off);
                off += len;
            } while (byteBuffer.remaining() > 0);
        } catch (IOException e) {
            e.printStackTrace();
            throw new FileOperationException("写文件发生异常");
        }

        // TODO 可优化
        try {
            endPosition = fileChannel.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void truncate(long size) {
        try {
            //System.out.println("删除前endPosition=" + endPosition + ",size=" + size);
            fileChannel.truncate(size);
            endPosition = Math.min(endPosition, size);
            //System.out.println("删除后大小" + fileChannel.size());
        } catch (IOException e) {
            throw new FileOperationException("truncate文件发生异常");
        }
    }


    public long getEndPosition() {
        return endPosition;
    }

    public void close() {
        try {
            fileChannel.close();
            randomAccessFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
