package com.moyu.xmz.common.constant;

import com.moyu.xmz.common.exception.DbException;

import java.util.Date;

/**
 * @author xiaomingzhang
 * @date 2023/5/16
 */
public enum ColumnTypeEnum {
    TINY_INT("tinyint", DbTypeConstant.TINY_INT),
    INT("int", DbTypeConstant.INT_4),
    INT_4("int4", DbTypeConstant.INT_4),
    BIGINT("bigint", DbTypeConstant.INT_8),
    CHAR("char", DbTypeConstant.CHAR),
    VARCHAR("varchar", DbTypeConstant.VARCHAR),
    TIMESTAMP("timestamp", DbTypeConstant.TIMESTAMP),
    DATETIME("datetime", DbTypeConstant.TIMESTAMP),
    TEXT("text", DbTypeConstant.VARCHAR)
    ;
    private String typeName;

    private Byte columnType;

    ColumnTypeEnum(String columnTypeName, Byte columnType) {
        this.typeName = columnTypeName;
        this.columnType = columnType;
    }


    public static Byte getColumnTypeByName(String typeName){
        for (ColumnTypeEnum typeEnum : ColumnTypeEnum.values()) {
            if(typeEnum.getTypeName().equals(typeName)) {
                return typeEnum.getColumnType();
            }
        }
        return null;
    }

    public static String getNameByType(Byte columnType){
        for (ColumnTypeEnum typeEnum : ColumnTypeEnum.values()) {
            if(typeEnum.getColumnType().equals(columnType)) {
                return typeEnum.getTypeName();
            }
        }
        return null;
    }


    public static Class<?> getJavaTypeClass(byte type) {
        for (ColumnTypeEnum typeEnum : ColumnTypeEnum.values()) {
            if(typeEnum.getColumnType().equals(type)) {
                return getJavaTypeClass(typeEnum);
            }
        }
        throw new DbException("类型不合法,type=" + type);
    }


    public static Class<?> getJavaTypeClass(ColumnTypeEnum typeEnum) {

        switch (typeEnum) {
            case INT_4:
            case INT:
                return Integer.class;
            case VARCHAR:
            case CHAR:
                return String.class;
            case BIGINT:
            case TIMESTAMP:
                return Date.class;
            default:
                throw new DbException("类型不合法,type=" + typeEnum);
        }
    }


    public String getTypeName() {
        return typeName;
    }

    public Byte getColumnType() {
        return columnType;
    }
}
