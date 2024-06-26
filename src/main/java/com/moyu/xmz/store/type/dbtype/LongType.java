package com.moyu.xmz.store.type.dbtype;

import com.moyu.xmz.common.DynByteBuffer;
import com.moyu.xmz.common.util.DataByteUtils;
import java.nio.ByteBuffer;

/**
 * @author xiaomingzhang
 * @date 2023/5/12
 */
public class LongType extends AbstractDbType<Long> {

    @Override
    protected Long readValue(ByteBuffer byteBuffer) {
        return DataByteUtils.readLong(byteBuffer);
    }

    @Override
    protected void writeValue(DynByteBuffer buffer, Long value) {
        buffer.putLong(value);
    }

    @Override
    public Class<?> getValueTypeClass() {
        return Long.class;
    }

    @Override
    public int getMaxByteLen(Long value) {
        return 8;
    }
}
