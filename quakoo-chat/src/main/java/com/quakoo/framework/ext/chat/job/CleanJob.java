package com.quakoo.framework.ext.chat.job;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import com.google.common.collect.Lists;
import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.distributed.DistributedConfig;
import com.quakoo.framework.ext.chat.model.constant.Status;
import com.quakoo.framework.ext.chat.util.PropertyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 清理任务
 * class_name: CleanJob
 * package: com.quakoo.framework.ext.chat.job
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:04
 **/
public class CleanJob {

    private Logger logger = LoggerFactory.getLogger(CleanJob.class);

    private PropertyUtil propertyUtil = PropertyUtil.getInstance("chat.properties");

    private int step_day = 1000 * 60 * 60 * 24; //保留天数

    @Resource
    private AbstractChatInfo chatInfo;

    @Resource
    private JdbcTemplate jdbcTemplate;


//    private void cleanSingleChatQueue(long time) {
//        try {
//            List<String> tableNames = chatInfo.single_chat_queue_table_names;
//            String sqlFormat = "delete from %s where status = %d and time < %d";
//            List<String> sqls = Lists.newArrayList();
//            for(String tableName : tableNames) {
//                sqls.add(String.format(sqlFormat, tableName, Status.finished, time));
//            }
//            this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//        }
//    }

//    private void cleanManyChatQueue(long time) {
//        try {
//            List<String> tableNames = chatInfo.many_chat_queue_table_names;
//            String sqlFormat = "delete from %s where status = %d and time < %d";
//            List<String> sqls = Lists.newArrayList();
//            for(String tableName : tableNames) {
//                sqls.add(String.format(sqlFormat, tableName, Status.finished, time));
//            }
//            this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//        }
//    }

    private void cleanMessage(long time) {
        try {
            List<String> tableNames = chatInfo.message_table_names;
            String sqlFormat = "delete from %s where time < %d";
            List<String> sqls = Lists.newArrayList();
            for(String tableName : tableNames) {
                sqls.add(String.format(sqlFormat, tableName, time));
            }
            this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

//    private void cleanPushQueue(long time) {
//        try {
//            String sqlFormat = "delete from push_queue where status = %d and time < %d";
//            String sql = String.format(sqlFormat, Status.finished, time);
//            this.jdbcTemplate.update(sql);
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//        }
//    }

    private void cleanUserStream(long time) {
        try {
            double sort = new Double(time).doubleValue();
            List<String> tableNames = chatInfo.user_stream_table_names;
            String sqlFormat = "delete from %s where sort < %s";
            List<String> sqls = Lists.newArrayList();
            for(String tableName : tableNames) {
                sqls.add(String.format(sqlFormat, tableName, sort));
            }
            this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

//    private void cleanUserClientInfo(long time) {
//        try {
//            List<String> tableNames = chatInfo.user_client_info_table_names;
//            String sqlFormat = "delete from %s where ctime < %s";
//            List<String> sqls = Lists.newArrayList();
//            for(String tableName : tableNames) {
//                sqls.add(String.format(sqlFormat, tableName, time));
//            }
//            this.jdbcTemplate.batchUpdate(sqls.toArray(new String[]{}));
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//        }
//    }

    private void _getAllOSS(OSSClient ossClient, String bucketName, String nextMarker, int maxKeys, List<OSSObjectSummary> list) {
        ListObjectsRequest request = new ListObjectsRequest(bucketName).withMaxKeys(maxKeys);
        if(StringUtils.isNotBlank(nextMarker)) {
            request.setMarker(nextMarker);
        }
        ObjectListing objectListing = ossClient.listObjects(request);
        nextMarker = objectListing.getNextMarker();
        List<OSSObjectSummary> subList = objectListing.getObjectSummaries();
        list.addAll(subList);
        if(subList.size() == maxKeys) {
            _getAllOSS(ossClient, bucketName, nextMarker, maxKeys, list);
        }
    }

    private List<OSSObjectSummary> getAllOss(OSSClient ossClient, String bucketName) {
        List<OSSObjectSummary> res = Lists.newArrayList();
        final int maxKeys = 1000;
        _getAllOSS(ossClient, bucketName, null, maxKeys, res);
        return res;
    }

    private void delOss(OSSClient ossClient, String bucketName, List<OSSObjectSummary> list, long minTime) {
        List<String> allDelKeys = Lists.newArrayList();
        for(OSSObjectSummary one : list) {
            if(one.getLastModified().getTime() < minTime) allDelKeys.add(one.getKey());
        }
        List<List<String>> delKeysList = Lists.partition(allDelKeys, 1000);
        for(List<String> delKeys : delKeysList) {
            DeleteObjectsResult deleteObjectsResult = ossClient.deleteObjects(new DeleteObjectsRequest(bucketName).withKeys(delKeys));
            List<String> deletedObjects = deleteObjectsResult.getDeletedObjects();
        }
    }

    private void cleanOss(String endpoint, String accessKeyId, String accessKeySecret, String bucketName, long minTime) {
        OSSClient ossClient = null;
        try {
            ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
            List<OSSObjectSummary> list = getAllOss(ossClient, bucketName);
            delOss(ossClient, bucketName, list, minTime);
        } finally {
            if(ossClient != null) ossClient.shutdown();
        }
    }

    public void handle() {
        if(DistributedConfig.canRunClean) {
            int dayNum = 4;
            String cleanStepDay = propertyUtil.getProperty("chat.clean.step.day");
            if(StringUtils.isNotBlank(cleanStepDay)) {
                dayNum = Integer.parseInt(cleanStepDay);
            }
            long currentTime = System.currentTimeMillis();
//        cleanSingleChatQueue(currentTime - step_day);
//        cleanManyChatQueue(currentTime - step_day);
//        cleanPushQueue(currentTime - step_day);
            cleanUserStream(currentTime - step_day * dayNum);
            cleanMessage(currentTime - step_day * dayNum);
//        cleanUserClientInfo(currentTime - step_day * 7);

        }
    }

}
