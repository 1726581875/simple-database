package com.moyu.xmz.store.type.dbtype;

import com.moyu.xmz.common.DynByteBuffer;
import com.moyu.xmz.common.util.DataByteUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * @author xiaomingzhang
 * @date 2023/10/17
 */
public class UnsignedLongType extends AbstractDbType<BigInteger> {

    @Override
    int getMaxByteLen(BigInteger value) {
        return 8;
    }

    @Override
    protected BigInteger readValue(ByteBuffer byteBuffer) {
        Long value = DataByteUtils.readLong(byteBuffer);
        return BigInteger.valueOf(value);
    }

    @Override
    protected void writeValue(DynByteBuffer buffer, BigInteger value) {
        buffer.putLong(value.longValue());
    }

    @Override
    public Class<?> getValueTypeClass() {
        return BigInteger.class;
    }
}
