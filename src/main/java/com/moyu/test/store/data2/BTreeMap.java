package com.moyu.test.store.data2;

import com.moyu.test.store.type.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xiaomingzhang
 * @date 2023/6/30
 */
public class BTreeMap<K, V> {

    private DataType<K> keyType;

    private DataType<V> valueType;

    private BTreeStore bpTreeStore;

    private int level;

    private int maxNodeNum = 1024;

    private Page<K, V> rootNode;

    private boolean isAutoCommit;

    private int nextPageIndex;



    public BTreeMap(DataType<K> keyType,DataType<V> valueType,BTreeStore bpTreeStore, boolean isAutoCommit) {
        this.keyType = keyType;
        this.valueType = valueType;
        this.bpTreeStore = bpTreeStore;
        this.isAutoCommit = isAutoCommit;

        initRootNode();
    }



    public void initRootNode() {
        if (bpTreeStore.getPageCount() == 0) {
            this.rootNode = Page.createLeaf(this, new ArrayList(), new ArrayList<>(),  bpTreeStore.getNextPageIndex());
            bpTreeStore.savePage(rootNode);
            bpTreeStore.updateRootPos(rootNode.getStartPos());
        } else {
            this.rootNode = bpTreeStore.getRootPage(this);
        }
        this.nextPageIndex++;
    }


    public V get(K k) {
        Page<K, V> page = rootNode;
        while (true) {
            int index = page.binarySearch(k);
            if (page.isLeaf()) {
                return index >= 0 ? page.getValueList().get(index) : null;
            } else if (index++ < 0) {
                index = -index;
            }
            page = getChildPage(page, index);
        }
    }


    /**
     * 获取第一个叶子节点页(b+树最左边叶子节点)
     * @return
     */
    public Page<K, V> getFirstLeafPage() {
        Page<K, V> page = rootNode;
        while (!page.isLeaf()) {
            page = page.getMap().getChildPage(page, 0);
        }
        return page;
    }

    /**
     * 获取可能出现k的叶子节点
     * @param k
     * @return
     */
    public Page<K, V> getPage(K k) {
        BTreePathPos<K, V> cursor = BTreePathPos.traverseDown(rootNode, k);
        return cursor.getTreeNode();
    }


    public Page<K, V> getPageByPos(Long pos) {
        return bpTreeStore.getPage(pos, this);
    }




    public Page<K, V> getChildPage(Page<K, V> page, int index) {
        List<Page.PageReference<K, V>> childNodeList = page.getChildNodeList();
        Page.PageReference<K, V> pageReference = childNodeList.get(index);
        Page<K, V> childPage = null;
        if(pageReference.getPage() == null) {
            childPage = bpTreeStore.getPage(pageReference.getPos(), this);
        } else {
            childPage = pageReference.getPage();
        }
        return childPage;
    }

    public long getNextRowId(){
       return bpTreeStore.getNextRowId();
    }

    /**
     * 参考H2database关于b+树的实现，写得非常好的一段代码
     * 也删减不必要的程序，使其更加简单
     * @param key
     * @param value
     */
    public void put(K key, V value) {
        // 从根节点往下找，并且记录查找路径
        BTreePathPos<K,V> cursor = BTreePathPos.traverseDown(rootNode, key);
        int index = cursor.getIndex();
        Page<K, V> node = cursor.getTreeNode();
        cursor = cursor.getParent();
        // index < 0表示找不到关键字,-1表示比任何关键字要小。其他数值计算通过(-index - 1)可以知道要插入的位置
        if (index < 0) {
            // 插入到叶子节点，b树每次插入都是在叶子节点插入
            node.insertLeaf(-index - 1, key, value);

            int keyCount;
            // 判断b树节点是否要分裂，当结点数大于最大节点树。关键子和值从中间分裂为两个节点，并会把中间关键字提取到父节点
            while ((keyCount = node.getKeywordCount()) > maxNodeNum
                    || node.getCurrMaxByteLen() > Page.PAGE_SIZE) {
                //获取中间关键字
                int at = keyCount >> 1;
                K k = node.getKeyword(at);
                // 从中间分裂出新节点
                Page<K, V> split = node.split(at);
                // 如果父节点是空，表示当前就是根节点, 新建新节点作为父节点，原先分裂的两个节点作为其子节点
                if (cursor == null) {
                    // 设置关键字
                    List<K> keys = new ArrayList<>(1);
                    keys.add(k);
                    // 设置子节点
                    List<Page.PageReference<K,V>> children = new ArrayList<>(2);
                    children.add(new Page.PageReference<K,V>(node));
                    children.add(new Page.PageReference<K,V>(split));
                    int nextPageIndex = bpTreeStore.getNextPageIndex();
                    rootNode = Page.createNonLeaf(this, keys, children, nextPageIndex);
                    level++;
                    // 保存新的根节点到磁盘
                    bpTreeStore.savePage(rootNode);
                    bpTreeStore.updateRootPos(rootNode.getStartPos());
                    // 保存左边节点和右边节点
                    bpTreeStore.savePage(node);
                    bpTreeStore.savePage(split);
                    // 结束
                    break;
                }

                // 当前节点赋值给c
                Page<K, V> c = node;
                // 赋值为父节点
                node = cursor.getTreeNode();
                // 拿到父节点中的插入位置
                index = cursor.getIndex();
                // 赋值为父节点, 往上，一直保持是node的父节点位置
                cursor = cursor.getParent();
                // 重新设置前面分裂左节点
                node.setChild(index, new Page.PageReference<>(c));
                // 插入前面分裂的右节点
                node.insertNonLeaf(index + 1, k, new Page.PageReference<>(split));

                // 更新到磁盘
                bpTreeStore.savePage(c);
                bpTreeStore.savePage(split);
            }
            bpTreeStore.savePage(node);
        } else {
            // index > 0 在叶子节点找到对应关键字， 直接替换为新值
            node.setLeafValue(index, value);
            // 更新到磁盘
            bpTreeStore.savePage(node);
        }
    }


    public Integer getNextPageIndex() {
        if (isAutoCommit) {
            int nextPageIndex = bpTreeStore.getNextPageIndex();
            return nextPageIndex;
        } else {
            Integer next = nextPageIndex;
            nextPageIndex++;
            return next;
        }
    }


    public void putUnSaveDisk(K key, V value) {
        // 从根节点往下找，并且记录查找路径
        BTreePathPos<K,V> cursor = BTreePathPos.traverseDown(rootNode, key);
        int index = cursor.getIndex();
        Page<K, V> node = cursor.getTreeNode();
        cursor = cursor.getParent();
        // index < 0表示找不到关键字,-1表示比任何关键字要小。其他数值计算通过(-index - 1)可以知道要插入的位置
        if (index < 0) {
            // 插入到叶子节点，b树每次插入都是在叶子节点插入
            node.insertLeaf(-index - 1, key, value);
            int keyCount;
            // 判断b树节点是否要分裂，当结点数大于最大节点树。关键子和值从中间分裂为两个节点，并会把中间关键字提取到父节点
            while ((keyCount = node.getKeywordCount()) > maxNodeNum
                    || node.getCurrMaxByteLen() > com.moyu.test.store.data.tree.Page.PAGE_SIZE) {
                // 相当于除2
                int at = keyCount >> 1;
                //获取中间关键字
                K k = node.getKeyword(at);
                // 从中间分裂出新节点
                Page<K, V> split = node.split(at);
                // 如果父节点是空，表示当前就是根节点, 新建新节点作为父节点，原先分裂的两个节点作为其子节点
                if (cursor == null) {
                    // 设置关键字
                    List<K> keys = new ArrayList<>();
                    keys.add(k);
                    // 设置子节点
                    List<Page.PageReference<K,V>> children = new ArrayList<>(2);
                    children.add(new Page.PageReference<K,V>(node));
                    children.add(new Page.PageReference<K,V>(split));
                    int nextPageIndex = bpTreeStore.getNextPageIndex();
                    rootNode = Page.createNonLeaf(this, keys, children, nextPageIndex);
                    level++;
                    // 结束
                    break;
                }

                // 当前节点赋值给c
                Page<K, V> c = node;
                // 赋值为父节点
                node = cursor.getTreeNode();
                // 拿到父节点中的插入位置
                index = cursor.getIndex();
                // 赋值为父节点, 往上，一直保持是node的父节点位置
                cursor = cursor.getParent();
                // 重新设置前面分裂左节点
                node.setChild(index, new Page.PageReference<>(c));
                // 插入前面分裂的右节点
                node.insertNonLeaf(index + 1, k, new Page.PageReference<>(split));
            }
        } else {
            // index > 0 在叶子节点找到对应关键字， 直接替换为新值
            node.setLeafValue(index, value);
        }
    }




    public void remove(K key) {
        BTreePathPos<K,V> cursor = BTreePathPos.traverseDown(rootNode, key);
        int index = cursor.getIndex();
        Page<K, V> node = cursor.getTreeNode();
        cursor = cursor.getParent();

        if(index < 0) {
            return;
        }

        node.remove(index);
        bpTreeStore.savePage(node);
    }

    public void clear() {
        bpTreeStore.clear();
        initRootNode();
    }



    public void commitSaveDisk() {
        long startPos = rootNode.getStartPos();
        bpTreeStore.updateRootPos(startPos);
        saveDisk(rootNode);

    }

    public void saveDisk(Page<K, V> node) {
        if (node.isLeaf()) {
            bpTreeStore.savePage(node);
        } else {
            bpTreeStore.savePage(node);
            List<Page.PageReference<K, V>> childNodeList = node.getChildNodeList();
            for (Page.PageReference<K, V> childNode : childNodeList) {
                saveDisk(childNode.getPage());
            }
        }
    }





    public Page<K, V> getRootNode() {
        return rootNode;
    }

    public int getLevel() {
        return level;
    }

    public DataType<K> getKeyType() {
        return keyType;
    }

    public void setKeyType(DataType<K> keyType) {
        this.keyType = keyType;
    }

    public DataType<V> getValueType() {
        return valueType;
    }

    public void setValueType(DataType<V> valueType) {
        this.valueType = valueType;
    }


    public BTreeStore getBpTreeStore() {
        return bpTreeStore;
    }


    public void close(){
        bpTreeStore.close();
    }



}
