package test.store.metadata;

import com.moyu.xmz.common.constant.ColumnTypeConstant;
import com.moyu.xmz.store.accessor.ColumnMetaFileAccessor;
import com.moyu.xmz.store.common.dto.Column;
import com.moyu.xmz.store.common.block.TableColumnBlock;
import com.moyu.xmz.common.util.FileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author xiaomingzhang
 * @date 2023/5/15
 */
public class ColumnMetadataStoreTest {


    private static String filePath = "D:\\mytest\\fileTest\\";


    public static void main(String[] args) {
        FileUtil.deleteOnExists(filePath + ColumnMetaFileAccessor.COLUMN_META_FILE_NAME);


        testCreateColumnBlock(1);
        testCreateColumnBlock(2);
        testCreateColumnBlock(3);
        //testDropColumnBlock(3);
    }


    private static void testCreateColumnBlock(Integer tableId){
        ColumnMetaFileAccessor metadataStore = null;
        try {
            metadataStore = new ColumnMetaFileAccessor(filePath);
            List<Column> columnDtoList = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Column columnDto = new Column("column_" + i, ColumnTypeConstant.VARCHAR, i, 64);
                columnDtoList.add(columnDto);
            }
            metadataStore.createColumnBlock(tableId, columnDtoList);
            Map<Integer, TableColumnBlock> columnMap = metadataStore.getColumnMap();
            columnMap.forEach((k,v) -> {
                System.out.println("=======  tableId="+ k +" ========");
                v.getColumnMetadataList().forEach(System.out::println);
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            metadataStore.close();
        }
    }


    private static void testDropColumnBlock(Integer tableId){
        ColumnMetaFileAccessor metadataStore = null;
        try {
            metadataStore = new ColumnMetaFileAccessor(filePath);
            metadataStore.dropColumnBlock(tableId);
            Map<Integer, TableColumnBlock> columnMap = metadataStore.getColumnMap();
            System.out.println("======= drop tableId="+ tableId +" ========");
            columnMap.forEach((k,v) -> {
                System.out.println("=======  tableId="+ k +" ========");
                System.out.println("v=" + v);
                v.getColumnMetadataList().forEach(System.out::println);
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            metadataStore.close();
        }
    }

}
