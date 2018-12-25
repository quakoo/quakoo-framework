import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.quakoo.baseFramework.secure.MD5Utils;
import com.quakoo.baseFramework.secure.Sha1Utils;
import com.quakoo.baseFramework.serialize.ScloudSerializeUtil;
import com.quakoo.hbaseFramework.BaseDalService;
import com.quakoo.hbaseFramework.HbaseClient;

import org.apache.commons.lang.time.StopWatch;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Test;

/**
 * Created by 136249 on 2015/3/16.
 */
public class TestHbaseClient {

	static HbaseClient hbaseClient=new HbaseClient();
    String name="testBlock";
    String tableNameBlock="tableNameBlock";
    String test1="test1";
    String test2="test2";
    static {
	
		try {
			//hbaseClient.setCoreConnectionNum(10);
			hbaseClient.afterPropertiesSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    @Test
    public void testRnad() throws Exception{
    	HbaseClient hbaseClient=new HbaseClient();


    }
    
    @Test
    public void testCreate() throws Exception {

    	HbaseClient hbaseClient=new HbaseClient();
    	
////getHexStringStartSplits
//    	hbaseClient.createTable("testBlock",null,hbaseClient.getIncrementerPerSpilts(4));
//		hbaseClient.createTable("BlockInfo", null,hbaseClient.getHexStringStartSplits(4,72));
//		hbaseClient.createTable("DeletedBlockInfo", null,hbaseClient.getHexStringStartSplits(4,72));
//		hbaseClient.createTable("UnsyncedBlock", null,hbaseClient.getHexStringStartSplits(4,72));
//    	hbaseClient.createTable("AppFile", null,hbaseClient.getHexStringStartSplits(4,36));
//    	hbaseClient.createTable("IndexAppFile", null,hbaseClient.getHexStringStartSplits(4,76));
//    	hbaseClient.createTable("DeletedAppFile", null,hbaseClient.getHexStringStartSplits(4,76));
//    	
//    	hbaseClient.createTable("Block", null,hbaseClient.getIncrementerPerSpilts(4));
//    	hbaseClient.createTable("File", null,hbaseClient.getIncrementerPerSpilts(4));
//    	hbaseClient.createTable("RelationFile", null,hbaseClient.getIncrementerPerSpilts(4));
//    	
//    	hbaseClient.createTable("IndexBlock", null,hbaseClient.getStringStartSplits(4,36));
//    	hbaseClient.createTable("IndexFile", null,hbaseClient.getStringStartSplits(4,36));
//    	
//
//    	
//    	hbaseClient.createTable("SyncItem",null,null);
//    	hbaseClient.createTable("SyncTaskUnProcessed",null,null);
//    	hbaseClient.createTable("SyncTaskProcessing",null,null);
//    	hbaseClient.createTable("SyncTaskProcessed",null,null);
//    	hbaseClient.createTable("SyncItemVersions",null,null);
//    	hbaseClient.createTable("SyncItemUserInfo",null,hbaseClient.getStringStartSplits(4,16));

    }

    
    

    @Test
    public void testPut()throws Exception{
    	final String sha1=Sha1Utils.sha1ReStr("test".getBytes());
    	final String md5=MD5Utils.md5ReStr("test".getBytes());
    	final long time=System.currentTimeMillis();
    	System.out.println("======================"+time);
			
			for(int j=0;j<1;j++){
				StopWatch stopWatch=new StopWatch();
				stopWatch.start();
				int thread=1;
				//131-134 10个线程 qps:4300
				//131-134 20个线程 qps:6451 ||10region qps:8000||4region qps:8000||
				//131-134 30个线程 qps:6385
				//131-134 40个线程：qps:3000||4region qps:10200
				final CountDownLatch countDownLatch=new CountDownLatch(thread);
				for(int i=0;i<thread;i++) {
					final int b=i;
					new Thread(new Runnable() {
						
						@Override
						public void run() {
						
						try{
							for(int m=0;m<1;m++){
								Block block=new Block(b*m, m*b,sha1 , md5, 1, 1, "ceshi", null, time, time, sha1+md5);
						    	//HbaseClient.put(name, Bytes.toBytes(m*b),ScloudSerializeUtil.encode(block));
								hbaseClient.put(name, HbaseClient.getIncrementerByte(m*b),ScloudSerializeUtil.encode(block),"");
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}finally{
							
							countDownLatch.countDown();
						}
						}
					}).start();
					
				}
				countDownLatch.await();
				System.out.println("==========="+stopWatch.getTime());
			}
    }
    
    @Test
    public void testTwoTable(){
    	try{
    	 Configuration conf=HBaseConfiguration.create();
    	 Connection connection = ConnectionFactory.createConnection(conf);
         Table table=connection.getTable(TableName.valueOf("test1"));
         Put put = new Put("1".getBytes());
         put.addColumn(Bytes.toBytes("f"), Bytes.toBytes("q"), "test11111".getBytes());
         table.put(put);
         table=connection.getTable(TableName.valueOf("test2"));
         put = new Put("1".getBytes());
         put.addColumn(Bytes.toBytes("f"), Bytes.toBytes("q"), "test2222".getBytes());
         table.put(put);
         table=connection.getTable(TableName.valueOf("test1"));
         Get get=new Get("1".getBytes());
         Result result = table.get(get);
         if (result != null && result.rawCells().length > 0) {
             for (Cell rowKV : result.rawCells()) {
                 System.out.println( "test1"+new String(CellUtil.cloneValue(rowKV)));
             }
         }
         table.close();
         table=connection.getTable(TableName.valueOf("test2"));
         get=new Get("1".getBytes());
         result = table.get(get);
         if (result != null && result.rawCells().length > 0) {
             for (Cell rowKV : result.rawCells()) {
                 System.out.println( "test2"+new String(CellUtil.cloneValue(rowKV)));
             }
         }
         
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }

	@Test
	public void testGet1()throws Exception{
		for(int j=0;j<3;j++){
			StopWatch stopWatch=new StopWatch();
			stopWatch.start();
			int thread=30;
			//131-134 线程10 qps:16666
			//131-134 线程20 qps:18181
			//131-134 线程30 qps:18181
			final CountDownLatch countDownLatch=new CountDownLatch(thread);
			for(int i=0;i<thread;i++) {
				final int b=i;
				BaseDalService.hbaseExecutor.execute(new Runnable() {
					
					@Override
					public void run() {
					
					try{
						for(int m=0;m<10000;m++){
						byte[] bytes= hbaseClient.get(name, HbaseClient.getIncrementerByte(m*b),"");
						if(bytes!=null){
							Block block=ScloudSerializeUtil.decode(bytes, Block.class);
						}else{
							System.out.println("=========================delete");
							hbaseClient.delete(name,  HbaseClient.getIncrementerByte(m*b), "sss");
						}	
				    	//System.out.println("========================="+block.getCtime());
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}finally{
						
						countDownLatch.countDown();
					}
					}
				});
				
			}
			countDownLatch.await();
			System.out.println("==========="+stopWatch.getTime());
		}
	}

    @Test
    public void testGet()throws Exception{
    	for(int j=0;j<10;j++){
    	StopWatch stopWatch=new StopWatch();
    	stopWatch.start();
        for(int i=0;i<10000;i++) {
           byte[] bytes= hbaseClient.get(name, Bytes.toBytes(i),"");
           Block block=ScloudSerializeUtil.decode(bytes, Block.class);
           assert block.getBlockId()==i;
        }
        System.out.println("==========="+stopWatch.getTime());
    	}
    }

	@Test
	public void testScan()throws Exception{
		for(int j=0;j<10;j++){
			StopWatch stopWatch=new StopWatch();
			stopWatch.start();
			List<Block> blockList =hbaseClient.scan(name, Bytes.toBytes(1), null, 100, Block.class, "test");
			System.out.println("==========="+stopWatch.getTime());
			System.out.println("===========blockList:"+blockList.size()+" "+blockList);
		}
	}
    
   

}
