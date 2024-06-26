package com.moyu.xmz.store.type.obj;

import com.moyu.xmz.common.DynByteBuffer;
import com.moyu.xmz.store.type.value.RowValue;

import java.nio.ByteBuffer;

/**
 * @author xiaomingzhang
 * @date 2023/6/30
 */
public class RowDataType extends AbstractObjType<RowValue> {

    @Override
    public RowValue read(ByteBuffer byteBuffer) {
        return new RowValue(byteBuffer);
    }

    @Override
    public void write(DynByteBuffer buffer, RowValue rowValue) {
        buffer.put(rowValue.getByteBuffer());
    }

    @Override
    public int compare(RowValue a, RowValue b) {
        throw new UnsupportedOperationException("类型RowValue不支持compare函数");
    }

    @Override
    public int getMaxByteSize(RowValue value) {
        return value.getTotalByteLen();
    }
}
