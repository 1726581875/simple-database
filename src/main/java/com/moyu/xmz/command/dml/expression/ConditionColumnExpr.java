package com.moyu.xmz.command.dml.expression;

import com.moyu.xmz.store.cursor.RowEntity;
import com.moyu.xmz.store.common.dto.Column;

/**
 * @author xiaomingzhang
 * @date 2023/7/17
 */
public class ConditionColumnExpr extends Expression {

    private Column column;

    public ConditionColumnExpr(Column column) {
        this.column = column;
    }

    @Override
    public Object getValue(RowEntity rowEntity) {
        Column column = rowEntity.getColumn(this.column.getColumnName(), this.column.getTableAlias());
        return column.getValue();
    }


    @Override
    public Expression optimize() {
        return this;
    }

    @Override
    public void getSQL(StringBuilder sqlBuilder) {
        sqlBuilder.append(column.getTableAliasColumnName());
    }

    public Column getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof ConditionColumnExpr) {
            ConditionColumnExpr that = (ConditionColumnExpr) o;
            if (column == that.getColumn()) {
                return true;
            }

            if(column.getTableAlias() == null && that.getColumn().getTableAlias() == null) {
                if (column.getColumnName().equals(that.getColumn().getColumnName())) {
                    return true;
                }
            } else if(column.getTableAlias() != null) {
                if (column.getTableAlias().equals(that.getColumn().getTableAlias())
                        && column.getColumnName().equals(that.getColumn().getColumnName())) {
                    return true;
                }
            }
        }
        return false;
    }

}
