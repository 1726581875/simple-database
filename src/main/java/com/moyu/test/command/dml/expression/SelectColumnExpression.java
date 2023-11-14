package com.moyu.test.command.dml.expression;

import com.moyu.test.command.dml.function.FuncArg;
import com.moyu.test.constant.ColumnTypeConstant;
import com.moyu.test.constant.FunctionConstant;
import com.moyu.test.exception.ExceptionUtil;
import com.moyu.test.store.data.cursor.RowEntity;
import com.moyu.test.store.metadata.obj.Column;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author xiaomingzhang
 * @date 2023/10/24
 */
public class SelectColumnExpression extends Expression {

    public static final String SIMPLE_COLUMN = "SIMPLE_COLUMN";
    public static final String FUNC_COLUMN = "FUNC_COLUMN";
    public static final String CONSTANT = "CONSTANT";
    public static final String NULL = "NULL";

    /**
     * SIMPLE_COLUMN
     */
    private String type;

    private Column tableColumn;

    private String functionName;

    private List<FuncArg> funcArgList;

    private String constantValue;

    public SelectColumnExpression(Column tableColumn, String type) {
        this.tableColumn = tableColumn;
        this.type = type;
    }

    public SelectColumnExpression(String constantValue) {
        this.constantValue = constantValue;
    }

    public SelectColumnExpression(String functionName, List<FuncArg> funcArgList) {
        this.functionName = functionName;
        this.funcArgList = funcArgList;
        this.type = FUNC_COLUMN;
    }

    @Override
    public Object getValue(RowEntity rowEntity) {
        switch (type) {
            case CONSTANT:
                // TODO 封装成字段形式
                return constantValue;
            case NULL:
                // TODO 封装成字段形式
                return null;
            case SIMPLE_COLUMN:
                Column column = rowEntity.getColumn(this.tableColumn.getColumnName(), this.tableColumn.getTableAlias());
                if(column == null) {
                    ExceptionUtil.throwSqlIllegalException("字段不存在:{}.{}",this.tableColumn.getTableAlias(), this.tableColumn.getColumnName());
                }
                return column;
            case FUNC_COLUMN:
                return getFunctionResult();
            default:
                ExceptionUtil.throwDbException("不支持该字段类型:{}", type);
        }
        return null;
    }


    private Column getFunctionResult() {

        switch (this.functionName) {
            case FunctionConstant.FUNC_UUID:
                String uuid = UUID.randomUUID().toString();
                Column uuidColumn = new Column(FunctionConstant.FUNC_UUID, ColumnTypeConstant.CHAR, -1, uuid.length());
                uuidColumn.setValue(uuid);
                return uuidColumn;
            case FunctionConstant.FUNC_NOW:
                Date now = new Date();
                Column dateColumn = new Column(FunctionConstant.FUNC_UUID, ColumnTypeConstant.TIMESTAMP, -1, 8);
                dateColumn.setValue(now);
                return dateColumn;
            default:
                ExceptionUtil.throwSqlIllegalException("不支持函数{}", this.functionName);
        }
        return null;
    }



    @Override
    public Expression optimize() {
        return null;
    }

    @Override
    public void getSQL(StringBuilder sqlBuilder) {

    }


    public static SelectColumnExpression newSimpleTableColumnExpr(Column tableColumn) {
        return new SelectColumnExpression(tableColumn, SIMPLE_COLUMN);
    }

    public static SelectColumnExpression newFuncColumnExpr(String functionName, List<FuncArg> funcArgList) {
        return new SelectColumnExpression(functionName, funcArgList);
    }

}