package com.moyu.test.command.dml.sql;

import com.moyu.test.store.metadata.obj.Column;

/**
 * @author xiaomingzhang
 * @date 2023/9/6
 */
public class GroupField {

    private Column column;

    public GroupField(Column column){
        this.column = column;
    }

    public Column getColumn() {
        return column;
    }

    public void setColumn(Column column) {
        this.column = column;
    }
}
