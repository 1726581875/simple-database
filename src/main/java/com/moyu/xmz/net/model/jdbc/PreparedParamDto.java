package com.moyu.xmz.net.model.jdbc;

import com.moyu.xmz.common.DynByteBuffer;
import com.moyu.xmz.store.type.DataType;
import com.moyu.xmz.store.type.dbtype.AbstractDbType;

import java.nio.ByteBuffer;

/**
 * @author xiaomingzhang
 * @date 2023/9/29
 * 网络传输，预编译sql的参数对象
 */
public class PreparedParamDto {

    private int totalByteLen;
    /**
     * 参数个数
     */
    private int size;
    /**
     * 参数类型数组
     */
    private byte[] typeArr;
    /**
     * 参数值
     */
    private Object[] valueArr;


    public PreparedParamDto(byte[] typeArr, Object[] valueArr) {
        this.totalByteLen = 0;
        if(typeArr != null) {
            this.size = typeArr.length;
        }
        this.typeArr = typeArr;
        this.valueArr = valueArr;
    }

    public PreparedParamDto(ByteBuffer byteBuffer) {
        this.totalByteLen = byteBuffer.getInt();
        this.size = byteBuffer.getInt();
        if(this.size > 0) {
            this.typeArr = new byte[this.size];
            this.valueArr = new Object[this.size];
            for (int i = 0; i < this.size; i++) {
                this.typeArr[i] = byteBuffer.get();
            }
            for (int i = 0; i < this.size; i++) {
                if(this.typeArr[i] == -1) {
                    this.valueArr[i] = null;
                } else {
                    DataType dataType = AbstractDbType.getDataType(this.typeArr[i]);
                    this.valueArr[i] = dataType.read(byteBuffer);
                }
            }
        }
    }


    public ByteBuffer getByteBuffer() {
        DynByteBuffer dynByteBuffer = new DynByteBuffer();
        dynByteBuffer.putInt(this.totalByteLen);
        dynByteBuffer.putInt(this.size);
        if (this.size > 0) {
            for (int i = 0; i < this.size; i++) {
                dynByteBuffer.put(this.typeArr[i]);
            }
            for (int i = 0; i < this.size; i++) {
                DataType dataType = AbstractDbType.getDataType(this.typeArr[i]);
                dataType.write(dynByteBuffer, this.valueArr[i]);
            }
        }
        this.totalByteLen = dynByteBuffer.position();
        dynByteBuffer.putInt(0, this.totalByteLen);
        ByteBuffer buffer = dynByteBuffer.getBuffer();
        buffer.flip();
        return buffer;
    }

    public int getTotalByteLen() {
        return totalByteLen;
    }

    public int getSize() {
        return size;
    }

    public byte[] getTypeArr() {
        return typeArr;
    }

    public Object[] getValueArr() {
        return valueArr;
    }
}
