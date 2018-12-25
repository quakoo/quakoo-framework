package com.quakoo.hbaseFramework;

import com.quakoo.baseFramework.serialize.ScloudSerializable;
import com.quakoo.baseFramework.serialize.ScloudSerializeUtil;
import com.quakoo.baseFramework.thread.NamedThreadFactory;
import com.quakoo.baseFramework.thread.SafeThreadPoolExecutor;
import com.quakoo.baseFramework.util.ByteUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import redis.clients.util.SafeEncoder;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by 136249 on 2015/3/14.
 */
public class HbaseClient implements Serializable, InitializingBean {
    final static Logger logger = LoggerFactory.getLogger(HbaseClient.class);
    static Map<String, ShardedHbasePool> shardedHbasePoolMap = new ConcurrentHashMap<>();
    public static final String columnFamily = "f";
    public static final String qualifier = "q";

    static Map<String, List<Connection>> connectionMap = new HashMap<String, List<Connection>>();

    public static final ExecutorService hbaseClientExecutor = new SafeThreadPoolExecutor(32, 512, 20, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), new NamedThreadFactory("hbaseClient"),
            new ThreadPoolExecutor.AbortPolicy(),11000);

    public static final ExecutorService hconnectionExecutor = new SafeThreadPoolExecutor(32, 512, 20, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), new NamedThreadFactory("hconnectionExecutor"),
            new ThreadPoolExecutor.AbortPolicy(),10000);

    private static final int putSingleTimeOut = 5000;
    private static final int getSingleTimeOut = 5000;
    private static final int putMultiTimeOut = 10000;
    private static final int getMultiTimeOut = 10000;
    private static final int scanTimeOut = 20000;

    /**
     * 预热table
     */
    private List<String> initTables;
    /**
     * 本地的hdfs文件
     */
    private List<String> localResources;
    private int coreConnectionNum = 2;

    public List<String> getInitTables() {
        return initTables;
    }

    public void setInitTables(List<String> initTables) {
        this.initTables = initTables;
    }

    public List<String> getLocalResources() {
        return localResources;
    }

    public void setLocalResources(List<String> localResources) {
        this.localResources = localResources;
    }

    public int getCoreConnectionNum() {
        return coreConnectionNum;
    }

    public void setCoreConnectionNum(int coreConnectionNum) {
        this.coreConnectionNum = coreConnectionNum;
    }

    private String key = "defaulKey";

    @Override
    public void afterPropertiesSet() throws Exception {
        //init connection
        if (localResources != null) {
            for (String localResource : localResources) {
                key = key + localResource;
            }
        }
        synchronized (connectionMap) {
            if (connectionMap.get(key) == null) {
                List<Connection> connections = new ArrayList<Connection>();
                connectionMap.put(key, connections);
                Configuration conf = HBaseConfiguration.create();
                if (localResources != null) {
                    for (String local : localResources) {
                        conf.addResource(local);
                    }
                }
                for (int i = 0; i < coreConnectionNum; i++) {
                    Connection connection = ConnectionFactory.createConnection(conf);
                    connections.add(connection);
                    // init tables
                    if (initTables != null) {
                        for (String tableName : initTables) {
                            Table table = connection.getTable(TableName.valueOf(tableName));
                            Get get = new Get(Bytes.toBytes(1));
                            table.get(get);
                            table.close();
                        }

                    }
                }
            }
        }


    }


    Table getTable(String tableName) {
        try {
            List<Connection> connections = connectionMap.get(key);
            Random random = new Random();
            return connections.get(random.nextInt(connections.size())).getTable(TableName.valueOf(tableName),hconnectionExecutor);
        } catch (Exception e) {
            throw new HbaseException("", e);
        }
    }


    public void createTable(String tableName, String columnFamily, byte[][] spilts) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        Connection connection = ConnectionFactory.createConnection(conf);
        Admin admin = connection.getAdmin();
        HTableDescriptor tableDesc = new HTableDescriptor(TableName.valueOf(tableName));
        if (StringUtils.isBlank(columnFamily)) {
            tableDesc.addFamily(new HColumnDescriptor(this.columnFamily));
        } else {
            tableDesc.addFamily(new HColumnDescriptor(columnFamily));
        }
        if (spilts == null) {
            admin.createTable(tableDesc);
        } else {
            admin.createTable(tableDesc, spilts);
        }
        admin.close();
    }

    public void cleanTable(String tableName) throws Exception {
        long cleankeyNum = 0;
        byte[] start = new byte[]{(byte) 0};
        for (; ; ) {
            List<byte[]> bytesList = scanKey(tableName, start, null, 1000);
            if (bytesList == null || bytesList.size() == 0) {
                break;
            }
            cleankeyNum = cleankeyNum + bytesList.size();
            batchDelete(tableName, bytesList, null);

        }
    }


    public void deleteTable(String tableName) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        Connection connection = ConnectionFactory.createConnection(conf);
        Admin admin = connection.getAdmin();
        admin.disableTable(TableName.valueOf(tableName));
        admin.deleteTable(TableName.valueOf(tableName));
    }

    public <T extends ScloudSerializable> void put(String tableName, byte[] rowKey, T value, String logKey) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        byte[] result = ScloudSerializeUtil.encode(value);
        put(tableName, rowKey, result,logKey);
    }


    public void put(final String tableName, final byte[] rowKey, final byte[] values, String logKey)  {
        putQualifier(tableName, null,rowKey,new byte[][]{values},logKey);

    }

    public void putQualifier(final String tableName, final String[] customerQualifier, final byte[] rowKey, final byte[][] values, String logKey)  {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Future future = hbaseClientExecutor.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    Table table = null;
                    try {
                        table = getTable(tableName);
                        Put put = new Put(rowKey);

                        for(int i=0;i<values.length;i++){
                            if(customerQualifier==null||(StringUtils.isBlank(customerQualifier[i]))) {
                                put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier),  values[i]);
                            }else{
                                put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(customerQualifier[i]), values[i]);
                            }
                        }
                        table.put(put);
                    } catch (Exception e) {
                        throw new HbaseException("", e);
                    } finally {
                        if (table != null) {
                            try {
                                table.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                    return null;
                }
            });
            try {
                future.get(putSingleTimeOut, TimeUnit.MILLISECONDS);
            }catch (Exception e){
                throw new HbaseException("", e);
            }
        } finally {
            logger.info("hbase put tableName:{},key:{},time:{},", new Object[]{tableName, logKey, stopWatch.getTime()});
        }

    }


    public <T extends ScloudSerializable> void batchPutObj(String tableName, List<byte[]> rowKey,
                                                           List<T> values, String logKey) {
        List<byte[]> result = new ArrayList<>();
        for (T value : values) {
            result.add(ScloudSerializeUtil.encode(value));
        }
        batchPut(tableName, rowKey, result,logKey);
    }
    public void batchPut(final String tableName, final List<byte[]> rowKeys, final List<byte[]> values,String logKey)  {
        List<byte[][]> listValues=new ArrayList<>();
        for(byte[] value:values){
            listValues.add(new byte[][]{value});
        }
        batchPutQualifier(tableName,rowKeys,null,listValues,logKey);
    }

    public void batchPutQualifier(final String tableName, final List<byte[]> rowKeys,final List<String[]> customerQualifiers, final List<byte[][]> listValues,String logKey)  {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {

            Future future = hbaseClientExecutor.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    Table table = null;
                    try {
                        table = getTable(tableName);
                        List<Put> puts = new ArrayList<>();
                        for (int i = 0; i < rowKeys.size(); i++) {
                            byte[] rowKey = rowKeys.get(i);
                            Put put = new Put(rowKey);
                            byte[][] values=listValues.get(i);
                            String[] customerQualifier=null;
                            if(customerQualifiers!=null){
                                customerQualifier= customerQualifiers.get(i);
                            }
                            for(int j=0;j<values.length;j++){
                                if(customerQualifier==null|| (StringUtils.isBlank( customerQualifier[j]))) {
                                    put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier),  values[j]);
                                }else{
                                    put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(customerQualifier[j]), values[j]);
                                }
                            }
                            puts.add(put);
                        }
                        table.put(puts);
                    } catch (Exception e) {
                        throw new HbaseException("", e);
                    } finally {
                        if (table != null) {
                            try {
                                table.close();
                            } catch (Exception e) {

                            }
                        }
                    }
                    return null;
                }
            });
            try {
                future.get(putMultiTimeOut, TimeUnit.MILLISECONDS);
            }catch (Exception e){
                throw new HbaseException("", e);
            }
        } finally {
            logger.info("hbase batchPut tableName:{},key:{},time:{},", new Object[]{tableName, logKey, stopWatch.getTime()});
        }
    }

    public <T extends ScloudSerializable> T get(String tableName, byte[] rowKey, Class<T> clazz, String logkey)  {
        byte[] result = get(tableName, rowKey,logkey);
        if (result == null) {
            return null;
        } else {
            return ScloudSerializeUtil.decode(result, clazz);
        }
    }

    public byte[] get(final String tableName, final byte[] rowKey,String logkey){
        Map<String,byte[]> map=getQualifier(tableName,rowKey,logkey);
        if(map==null){
            return null;
        }
        return map.get(qualifier);
    }


    public Map<String,byte[]> getQualifier(final String tableName, final byte[] rowKey,final String logkey){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Future< Map<String,byte[]>> future = hbaseClientExecutor.submit(new Callable< Map<String,byte[]>>() {
                @Override
                public  Map<String,byte[]> call() throws Exception {
                    Table table = null;
                    try {
                        table = getTable(tableName);
                        Get get = new Get(rowKey);
                        Result result = table.get(get);
                        if (result != null && result.rawCells().length > 0) {
                            Map<String,byte[]> map=new HashMap<>();
                            for (Cell rowKV : result.rawCells()) {
                                map.put(Bytes.toString(CellUtil.cloneQualifier(rowKV)),CellUtil.cloneValue(rowKV));
                            }
                            return map;
                        }
                    } catch (Exception e) {
                        throw new HbaseException("", e);
                    } finally {
                        if (table != null) {
                            try {
                                table.close();
                            } catch (Exception e) {

                            }
                        }
                    }
                    return null;
                }
            });
            try {
                return future.get(getSingleTimeOut, TimeUnit.MILLISECONDS);
            }catch (Exception e){
                throw new HbaseException("", e);
            }
        } finally {
            logger.info("hbase get tableName:{},key:{},time:{},", new Object[]{tableName, logkey, stopWatch.getTime()});
        }
    }


    public <T extends ScloudSerializable> Map<byte[], T> batchGet(String tableName, List<byte[]> rowKey, Class<T> clazz, String logkey) {
        Map<byte[], byte[]> result = batchGet(tableName, rowKey,logkey);
        if (result == null) {
            return null;
        } else {
            Map<byte[], T> map = new HashMap<>();
            Iterator<Map.Entry<byte[], byte[]>> it=result.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<byte[], byte[]> entry=it.next();
                map.put(entry.getKey(), ScloudSerializeUtil.decode(entry.getValue(), clazz));
            }
            return map;
        }
    }


    public Map<byte[], byte[]> batchGet(final String tableName, final List<byte[]> rowKey,String logkey) {
        Map<byte[], Map<String,byte[]>> result=   batchGetQualifier(tableName,rowKey,logkey);
        if(result==null){
            return null;
        }
        Map<byte[], byte[]> map=new HashMap<>();
        Iterator<Map.Entry<byte[], Map<String,byte[]>>> iterator=result.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<byte[], Map<String,byte[]>> entry= iterator.next();
            byte[] rk=entry.getKey();
            Map<String,byte[]> rv=entry.getValue();
            byte[] rr=  rv.get(qualifier);
            map.put(rk,rr);
        }
        return map;
    }


    public Map<byte[], Map<String,byte[]>> batchGetQualifier(final String tableName, final List<byte[]> rowKey,String logkey) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Future<Map<byte[], Map<String,byte[]>>> future = hbaseClientExecutor.submit(new Callable<Map<byte[], Map<String,byte[]>>>() {
                @Override
                public Map<byte[], Map<String,byte[]>> call() throws Exception {
                    Table table = null;
                    Map<byte[], Map<String,byte[]>> result = new HashMap<>();
                    try {
                        table = getTable(tableName);
                        final List<Get> gets = new ArrayList<>();
                        for (byte[] byteKey : rowKey) {
                            Get get = new Get(byteKey);
                            gets.add(get);
                        }
                        Result[] results = table.get(gets);
                        for (Result cellResult : results) {
                            if (cellResult != null && cellResult.rawCells().length > 0) {
                                Map<String,byte[]> map=new HashMap<>();
                                byte[] rowkey=null;
                                for (Cell rowKV : cellResult.rawCells()) {
                                    rowkey=CellUtil.cloneRow(rowKV);
                                    map.put(Bytes.toString(CellUtil.cloneQualifier(rowKV)),CellUtil.cloneValue(rowKV));
                                }
                                result.put(rowkey,map);
                            }
                        }
                    } catch (Exception e) {
                        throw new HbaseException("", e);
                    } finally {
                        if (table != null) {
                            try {
                                table.close();
                            } catch (Exception e) {

                            }
                        }
                    }
                    return result;
                }
            });
            try {
                return future.get(getMultiTimeOut, TimeUnit.MILLISECONDS);
            }catch (Exception e){
                throw new HbaseException("", e);
            }
        } finally {
            logger.info("hbase batchGet tableName:{},key:{},time:{},", new Object[]{tableName, logkey, stopWatch.getTime()});
        }
    }


    public void delete(final String tableName, final byte[] rowKey, final String logkey){
        Future future = hbaseClientExecutor.submit(new Callable() {
            @Override
            public Object call() throws Exception {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                Table table = null;
                try {
                    table = getTable(tableName);
                    Delete delete = new Delete(rowKey);
                    table.delete(delete);
                } catch (Exception e) {
                    throw new HbaseException("", e);
                } finally {
                    if (table != null) {
                        try {
                            table.close();
                        } catch (Exception e) {
                        }
                    }
                    logger.info("hbase batchGet tableName:{},key:{},time:{},", new Object[]{tableName, logkey, stopWatch.getTime()});
                }
                return null;
            }
        });
        try {
            future.get(putSingleTimeOut, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            throw new HbaseException("", e);
        }
    }


    public void batchDelete(final String tableName, final List<byte[]> rowKeys, final List<String> logkeys) {
        Future future = hbaseClientExecutor.submit(new Callable() {
            @Override
            public Object call() throws Exception {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                Table table = null;
                try {
                    table = getTable(tableName);
                    List<Delete> deletes = new ArrayList<Delete>();
                    for (byte[] rowKey : rowKeys) {
                        Delete delete = new Delete(rowKey);
                        deletes.add(delete);
                    }
                    table.batch(deletes);
                } catch (Exception e) {
                    throw new HbaseException("", e);
                } finally {
                    if (table != null) {
                        try {
                            table.close();
                        } catch (Exception e) {
                        }
                    }
                    logger.info("hbase batchGet tableName:{},key:{},time:{},", new Object[]{tableName, logkeys, stopWatch.getTime()});
                }
                return null;
            }
        });
        try {
            future.get(putMultiTimeOut, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            throw new HbaseException("", e);
        }
    }


    public <T extends ScloudSerializable> List<T> scan(String tableName, byte[] startRow, byte[] endRow, int limit, Class<T> clazz, String logkey) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            List<byte[]> result = scan(tableName, startRow, endRow, limit,logkey);
            if (result == null) {
                return null;
            } else {
                List<T> list = new ArrayList<>();
                for (byte[] bytes : result) {
                    list.add(ScloudSerializeUtil.decode(bytes, clazz));
                }
                return list;
            }
        } finally {
            logger.info("hbase scan tableName:{},message:{},time:{},", new Object[]{tableName, logkey, stopWatch.getTime()});
        }
    }


    public List<byte[]> scan(final String tableName, final byte[] startRow, final byte[] endRow, final int limit, String logkey) {
        List<Map<String,byte[]>> list= scanQualifier(tableName, startRow,endRow, limit, logkey);
        if(list==null){
            return null;
        }
        List<byte[]>  result=new ArrayList<>();
        for (Map<String,byte[]> subResult:list){
            byte[] rr=  subResult.get(qualifier);
            result.add(rr);
        }
        return result;
    }

    public List<Map<String,byte[]>> scanQualifier(final String tableName, final byte[] startRow, final byte[] endRow, final int limit, String logkey) {
        Future<List<Map<String,byte[]>>> future = hbaseClientExecutor.submit(new Callable<List<Map<String,byte[]>>>() {
            @Override
            public List<Map<String,byte[]>> call() throws Exception {
                Table table = null;
                List<Map<String,byte[]>> result = new ArrayList<>();
                try {
                    table = getTable(tableName);
                    Scan scan = new Scan();
                    if (startRow != null) {
                        scan.setStartRow(startRow);
                    }
                    if (endRow != null) {
                        scan.setStopRow(endRow);
                    }
                    if (limit != 0) {
                        scan.setFilter(new PageFilter(limit));
                    }
                    ResultScanner resultScanner = table.getScanner(scan);
                    if (resultScanner == null) {
                        return result;
                    }

                    Iterator<Result> resultIterator = resultScanner.iterator();
                    while (resultIterator.hasNext()) {
                        Result hbaseResult = resultIterator.next();
                        if (hbaseResult != null && hbaseResult.rawCells().length > 0) {
                            Map<String,byte[]> map=new HashMap<>();
                            String key= Bytes.toString(hbaseResult.getRow());
                            for (Cell rowKV : hbaseResult.rawCells()) {
                                map.put(Bytes.toString(CellUtil.cloneQualifier(rowKV)),CellUtil.cloneValue(rowKV));
                               
                            }
                            
                            result.add(map);
                        }
                    }
                } catch (Exception e) {
                    throw new HbaseException("", e);
                } finally {
                    if (table != null) {
                        try {
                            table.close();
                        } catch (Exception e) {

                        }
                    }
                }
                return result;
            }
        });
        try {
            return future.get(scanTimeOut, TimeUnit.MILLISECONDS);
        } catch (Exception e){
            throw new HbaseException("", e);
        }
    }


    public List<byte[]> scanKey(final String tableName, final byte[] startRow, final byte[] endRow, final int limit) {
        Future<List<byte[]>> future = hbaseClientExecutor.submit(new Callable<List<byte[]>>() {
            @Override
            public List<byte[]> call() throws Exception {
                List<byte[]> result = new ArrayList<>();
                Table table = null;
                try {
                    table = getTable(tableName);
                    Scan scan = new Scan();
                    if (startRow != null) {
                        scan.setStartRow(startRow);
                    }
                    if (endRow != null) {
                        scan.setStopRow(endRow);
                    }
                    if (limit != 0) {
                        scan.setFilter(new PageFilter(limit));
                    }
                    ResultScanner resultScanner = table.getScanner(scan);
                    if (resultScanner == null) {
                        return result;
                    }
                    Iterator<Result> resultIterator = resultScanner.iterator();
                    while (resultIterator.hasNext()) {
                        Result hbaseResult = resultIterator.next();
                        if (hbaseResult != null && hbaseResult.rawCells().length > 0) {
                            for (Cell rowKV : hbaseResult.rawCells()) {
                                result.add(CellUtil.cloneRow(rowKV));
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new HbaseException("", e);
                } finally {
                    if (table != null) {
                        try {
                            table.close();
                        } catch (Exception e) {
                        }
                    }
                }
                return result;
            }
        });
        try {
            return future.get(putSingleTimeOut, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            throw new HbaseException("", e);
        }
    }

    /**
     * 顺序自增ID转换
     *
     * @return
     */
    public static byte[] getIncrementerByte(long x) {
        try {
            byte[] bb = new byte[ByteUtil.SIZEOF_LONG];
            bb[7] = (byte) (x >> 56);
            bb[6] = (byte) (x >> 48);
            bb[5] = (byte) (x >> 40);
            bb[4] = (byte) (x >> 32);
            bb[3] = (byte) (x >> 24);
            bb[2] = (byte) (x >> 16);
            bb[1] = (byte) (x >> 8);
            bb[0] = (byte) (x >> 0);
            return bb;
        } catch (Exception e) {
            throw new HbaseException("getIncrementerByte error", e);
        }
    }

    public static byte[][] getIncrementerPerSpilts(int splits) {
        try {
            int splitsNum = splits - 1;
            byte[][] bbs = new byte[splitsNum][];
            for (int i = 0; i < splitsNum; i++) {
                double b = 256d / (double) splits * (double) (i + 1);
                System.out.println((long) b);
                bbs[i] = getIncrementerByte((long) b);
            }
            return bbs;
        } catch (Exception e) {
            throw new HbaseException("getIncrementerByte error", e);
        }
    }


    /**
     * 字符串随机分布(不能是汉字之类的)
     *
     * @param splits
     * @param keyByteLength
     * @return
     */
    public static byte[][] getStringStartSplits(int splits, int keyByteLength) {
        try {
            int splitsNum = splits - 1;
            byte[][] bbs = new byte[splitsNum][];
            for (int i = 0; i < splitsNum; i++) {
                double b = 256d / (double) splits * (double) (i + 1);
                System.out.println((long) b);
                bbs[i] = new byte[keyByteLength];
                bbs[i][0] = (byte) ((long) b >> 0);
                for (int m = 1; m < keyByteLength; m++) {
                    bbs[i][m] = (byte) 0;
                }
            }
            return bbs;
        } catch (Exception e) {
            throw new HbaseException("getIncrementerByte error", e);
        }
    }


    /**
     * 字符串以0-F的形式
     *
     * @param splits
     * @param keyByteLength
     * @return
     */
    public static byte[][] getHexStringStartSplits(int splits, int keyByteLength) {
        try {
            int splitsNum = splits - 1;
            byte[][] bbs = new byte[splitsNum][];
            for (int i = 0; i < splitsNum; i++) {
                double b = 256d / (double) splits * (double) (i + 1);
                String hex = Integer.toHexString((int) b);
                if (hex.length() == 1) {
                    hex = hex + "0";
                }
                byte[] hexBytes = SafeEncoder.encode(hex);
                bbs[i] = new byte[keyByteLength];
                bbs[i][0] = hexBytes[0];
                bbs[i][1] = hexBytes[1];
                for (int m = 1; m < keyByteLength; m++) {
                    bbs[i][m] = (byte) 0;
                }
            }
            return bbs;
        } catch (Exception e) {
            throw new HbaseException("getIncrementerByte error", e);
        }
    }


    public static void main(String[] fwef) throws Exception {
        for (int i = 10; i < 30; i++) {
            getHexStringStartSplits(4, i);
            System.out.println("========");
        }
    }


}
