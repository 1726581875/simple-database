package com.moyu.xmz.common.exception;

/**
 * @author xiaomingzhang
 * @date 2023/5/18
 */
public class SqlExecutionException extends DbException {

    public SqlExecutionException(String msg) {
        super(msg);
    }
}
