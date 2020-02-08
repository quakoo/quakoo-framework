package com.quakoo.framework.ext.recommend.service.impl;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.property.PropertyLoader;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import com.quakoo.framework.ext.recommend.bean.DelIndex;
import com.quakoo.framework.ext.recommend.service.RecommendIndexService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.List;

public class RecommendIndexServiceImpl implements InitializingBean, RecommendIndexService {

    Logger logger = LoggerFactory.getLogger(RecommendIndexServiceImpl.class);

    private PropertyLoader propertyLoader = PropertyLoader.getInstance("dao.properties");

    private RestHighLevelClient esClient;

    private JedisX cache;

    @Resource
    private AbstractRecommendInfo recommendInfo;

    private String del_list_key = "%s_recommend_del_list";

    @Override
    public void afterPropertiesSet() throws Exception {
        String esHostport = propertyLoader.getProperty("es.hostport.list");
        if(StringUtils.isBlank(esHostport)) throw new IllegalStateException("es.hostport.list is null");

        List<String> hostportList = Lists.newArrayList(StringUtils.split(esHostport, ","));
        List<HttpHost> httpHosts = Lists.newArrayList();
        for(String hostport : hostportList) {
            String host = StringUtils.split(hostport, ":")[0];
            int port = Integer.parseInt(StringUtils.split(hostport, ":")[1]);
            HttpHost httpHost = new HttpHost(host, port, "http");
            httpHosts.add(httpHost);
        }
        esClient = new RestHighLevelClient(RestClient.builder(httpHosts.toArray(new HttpHost[]{})));
        cache = new JedisX(recommendInfo.redisInfo, recommendInfo.redisConfig, 2000);
    }

    @Override
    public void recordDelIndex(String index, List<String> ids) throws Exception {
        String key = String.format(del_list_key, recommendInfo.projectName);
        List<Object> list = Lists.newArrayList();
        for(String id : ids) {
            DelIndex delIndex = new DelIndex();
            delIndex.setIndex(index);
            delIndex.setId(id);
            list.add(delIndex);
        }
        cache.piprpushObject(key, list);
    }

    @Override
    public void handleDelIndex(int size) throws Exception {
        String key = String.format(del_list_key, recommendInfo.projectName);
        List<Object> list = cache.lrangeAndDelObject(key, 0, size, null);
        if(list != null && list.size() > 0) {
            List<DeleteRequest> deleteRequests = Lists.newArrayList();
            for(Object obj : list) {
                DelIndex delIndex = (DelIndex)obj;
                DeleteRequest deleteRequest = new DeleteRequest(delIndex.getIndex());
                deleteRequest.id(delIndex.getId());
                deleteRequests.add(deleteRequest);
            }
            BulkRequest bulkRequest = new BulkRequest();
            for (DeleteRequest deleteRequest : deleteRequests) {
                bulkRequest.add(deleteRequest);
            }
            BulkResponse bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            boolean sign = !bulkResponse.hasFailures();
//        System.out.println("delete : " + sign);
            if(!sign) {
                logger.error("delete index error msg : " + bulkResponse.buildFailureMessage());
            }
        }
    }

}
