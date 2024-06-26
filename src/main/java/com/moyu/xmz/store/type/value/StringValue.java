package com.moyu.xmz.store.type.value;

import com.moyu.xmz.store.type.DataType;
import com.moyu.xmz.store.type.dbtype.StringType;

import java.nio.ByteBuffer;

/**
 * @author xiaomingzhang
 * @date 2023/7/9
 */
public final class StringValue extends Value {

    private String value;

    public StringValue(String value) {
        this.value = value;
    }

    @Override
    public DataType getDataTypeObj() {
        return new StringType();
    }

    @Override
    public int getType() {
        return TYPE_STRING;
    }

    @Override
    public Object getObjValue() {
        return value;
    }

    @Override
    public int compare(Value v) {
        return value.compareTo(((StringValue) v).getValue());
    }

    @Override
    public int getMaxSize() {
        if(value != null) {
            return 4 + (value.length() * 3);
        }
        return 0;
    }

    public String getValue() {
        return value;
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return null;
    }
}
