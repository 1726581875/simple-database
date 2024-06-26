package test.transaction;

import com.moyu.xmz.command.Command;
import com.moyu.xmz.command.QueryResult;
import com.moyu.xmz.command.SqlParser;
import com.moyu.xmz.session.ConnectSession;
import com.moyu.xmz.store.transaction.Transaction;
import com.moyu.xmz.store.transaction.TransactionManager;
import com.moyu.xmz.terminal.util.PrintResultUtil;

/**
 * @author xiaomingzhang
 * @date 2023/6/13
 */
public class SimpleTransactionTest {


    public static void main(String[] args) {

        ConnectSession connectSession = new ConnectSession("xmz", 1);
        testExecSQL("create table xmz_yan (id int, name varchar(10), time timestamp)", connectSession);
        testExecSQL("insert into xmz_yan(id,name,time) value (1,'111','2023-05-19 00:00:00')", connectSession);
        testExecSQL("insert into xmz_yan(id,name,time) value (2,'222','2023-05-19 00:00:00')", connectSession);
        testExecSQL("insert into xmz_yan(id,name,time) value (3,'333','2023-05-19 00:00:00')", connectSession);


        int tid = TransactionManager.initTransaction(connectSession);
        testExecSQL("update xmz_yan set name = '520' where id = 1", connectSession);
        testExecSQL("update xmz_yan set name = '250' where id = 2", connectSession);
        testExecSQL("select * from xmz_yan", connectSession);
        Transaction transaction = TransactionManager.getTransaction(tid);
        transaction.rollback();


        testExecSQL("select * from xmz_yan", connectSession);

        testExecSQL("drop table xmz_yan", connectSession);
    }

    private static void testExecSQL(String sql, ConnectSession session) {
        System.out.println("====================================");
        System.out.println("执行语句 " + sql + "");
        SqlParser sqlParser = new SqlParser(session);
        Command command = sqlParser.prepareCommand(sql);
        QueryResult queryResult = command.exec();
        System.out.println("执行结果:");
        PrintResultUtil.printResult(queryResult);
        System.out.println("====================================");
    }

}
