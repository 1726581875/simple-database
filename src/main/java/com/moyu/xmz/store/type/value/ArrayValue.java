package com.moyu.xmz.store.type.value;

import com.moyu.xmz.store.type.DataType;
import com.moyu.xmz.store.type.obj.ArrayDataType;
import com.moyu.xmz.common.exception.DbException;
import com.moyu.xmz.common.DynByteBuffer;

import java.nio.ByteBuffer;

/**
 * @author xiaomingzhang
 * @date 2023/6/27
 */
public class ArrayValue<V extends Value> extends Value {

    private DataType itemDataType;

    private V[] arr;


    public ArrayValue(V[] arr, DataType itemDataType) {
        this.arr = arr;
        this.itemDataType = itemDataType;
    }


    public ArrayValue(ByteBuffer byteBuffer) {
        byte flag = byteBuffer.get();
        int itemType = byteBuffer.getInt();
        DataType dataType = Value.getDataTypeObj(itemType);
        this.itemDataType = dataType;
        if (flag == (byte) 0) {
            arr = null;
        } else {
            int len = byteBuffer.getInt();
            Value[] array = new Value[len];
            int i = 0;
            while (i < len) {
                Object objValue = this.itemDataType.read(byteBuffer);
                Value value = createValue(objValue);
                array[i++] = value;
            }
            this.arr = (V[]) array;
        }

    }


    private Value createValue(Object value) {
        if (value instanceof Integer) {
            return new IntegerValue((Integer) value);
        } else if (value instanceof Long) {
            return new LongValue((Long) value);
        } else if (value instanceof String) {
            return new StringValue((String) value);
        } else {
            throw new DbException("不支持类型" + value);
        }

    }


    @Override
    public ByteBuffer getByteBuffer() {
        DynByteBuffer dynByteBuffer = new DynByteBuffer();
        int dataType = getDataType(itemDataType);
        if (arr == null) {
            dynByteBuffer.put((byte) 0);
            dynByteBuffer.putInt(dataType);
        } else {
            dynByteBuffer.put((byte) 1);
            dynByteBuffer.putInt(dataType);
            dynByteBuffer.putInt(arr.length);
            int i = 0;
            while (i < arr.length) {
                itemDataType.write(dynByteBuffer, arr[i++].getObjValue());
            }
        }
        ByteBuffer buffer = dynByteBuffer.getBuffer();
        buffer.flip();
        return buffer;
    }

    @Override
    public DataType getDataTypeObj() {
        return new ArrayDataType();
    }

    @Override
    public int getType() {
        return Value.TYPE_VALUE_ARR;
    }

    @Override
    public Object getObjValue() {
        return null;
    }

    @Override
    public int compare(Value v) {
        return 0;
    }

    @Override
    public int getMaxSize() {
        if (arr == null) {
            return 1 + 4;
        } else {
            int size = 1 + 4 + 4;
            int i = 0;
            while (i < arr.length) {
                size += itemDataType.getMaxByteSize(arr[i].getObjValue());
                i++;
            }
            return size;
        }
    }

    public V[] getArr() {
        return arr;
    }
}
