package com.quakoo.framework.ext.recommend.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.property.PropertyLoader;
import com.quakoo.framework.ext.recommend.bean.ESField;
import com.quakoo.framework.ext.recommend.dao.SyncInfoDao;
import com.quakoo.framework.ext.recommend.model.SyncInfo;
import com.quakoo.framework.ext.recommend.service.SyncInfoService;
import com.quakoo.framework.ext.recommend.util.ESUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Data
public class SyncInfoServiceImpl implements SyncInfoService, InitializingBean {

    private PropertyLoader propertyLoader = PropertyLoader.getInstance("dao.properties");

    @Resource
    private SyncInfoDao syncInfoDao;

    private RestHighLevelClient esClient;

    private int esNumberShards;

    private int esNumberReplicas;

    @Override
    public void afterPropertiesSet() throws Exception {
        String esHostport = propertyLoader.getProperty("es.hostport.list");
        String esNumberShardsStr = propertyLoader.getProperty("es.number.shards");
        String esNumberReplicasStr = propertyLoader.getProperty("es.number.replicas");
        if(StringUtils.isBlank(esHostport)) throw new IllegalStateException("es.hostport.list is null");
        if(StringUtils.isBlank(esNumberShardsStr)) throw new IllegalStateException("es.number.shards is null");
        if(StringUtils.isBlank(esNumberReplicasStr)) throw new IllegalStateException("es.number.replicas is null");

        List<String> hostportList = Lists.newArrayList(StringUtils.split(esHostport, ","));
        List<HttpHost> httpHosts = Lists.newArrayList();
        for(String hostport : hostportList) {
            String host = StringUtils.split(hostport, ":")[0];
            int port = Integer.parseInt(StringUtils.split(hostport, ":")[1]);
            HttpHost httpHost = new HttpHost(host, port, "http");
            httpHosts.add(httpHost);
        }
        esClient = new RestHighLevelClient(RestClient.builder(httpHosts.toArray(new HttpHost[]{})));
        esNumberShards = Integer.parseInt(esNumberShardsStr);
        esNumberReplicas = Integer.parseInt(esNumberReplicasStr);
    }

    private void initESIndex(RestHighLevelClient esClient, int esNumberShards, int esNumberReplicas, SyncInfo syncInfo) throws Exception {
        long lastTrackingValue = syncInfo.getLastTrackingValue();
        if(lastTrackingValue == 0) {
            String esIndex = syncInfo.getEsIndex();
            List<ESField> esFields = syncInfo.getEsFields();
            String json = ESUtils.toIndexJson(esFields);
            GetIndexRequest getIndexRequest = new GetIndexRequest(esIndex);
            boolean exist = esClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
            if(!exist) {
                CreateIndexRequest createIndexRequest = new CreateIndexRequest(esIndex);
                createIndexRequest.settings(Settings.builder().put("index.number_of_shards", esNumberShards)
                        .put("index.number_of_replicas", esNumberReplicas)
                        .put("index.blocks.read_only_allow_delete", "false"));
                createIndexRequest.mapping(json, XContentType.JSON);
                CreateIndexResponse createIndexResponse = esClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                boolean createSign = createIndexResponse.isAcknowledged();
                if(!createSign) throw new IllegalStateException("create index error");
            }
        }
    }

    @Override
    public int handle(SyncInfo syncInfo) throws Exception {
        initESIndex(esClient, esNumberShards, esNumberReplicas, syncInfo);
        List<Map<String, Object>> list = syncInfoDao.syncList(syncInfo);
        if(list.size() > 0) {
            List<ESField> esFields = syncInfo.getEsFields();
            String esid = syncInfo.getEsId();
            String esIndex = syncInfo.getEsIndex();
            String trackingColumn = syncInfo.getTrackingColumn();
            List<IndexRequest> indexRequests = Lists.newArrayList();
            for(Map<String, Object> map : list) {
                Map<String, Object> esMap = Maps.newLinkedHashMap();
                for(ESField esField : esFields) {
                    String key = esField.getName();
                    Object value = map.get(esField.getMysqlColumn());
                    esMap.put(key, value);
                }
                IndexRequest indexRequest = new IndexRequest(esIndex);
                indexRequest.id(String.valueOf(map.get(esid)));
                indexRequest.source(JsonUtils.toJson(esMap), XContentType.JSON);
                indexRequests.add(indexRequest);
            }
            BulkRequest bulkRequest = new BulkRequest();
            for (IndexRequest indexRequest : indexRequests) {
                bulkRequest.add(indexRequest);
            }
            BulkResponse bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            boolean sign = !bulkResponse.hasFailures();
            if(!sign) {
                throw new IllegalStateException("es insert error," + bulkResponse.buildFailureMessage());
            }
            long trackingValue = (long)list.get(list.size() - 1).get(trackingColumn);
            syncInfoDao.updateTrackingValue(syncInfo.getId(), trackingValue);
        }
        return list.size();
    }

    @Override
    public boolean insert(SyncInfo syncInfo) throws Exception {
        boolean res = syncInfoDao.insert(syncInfo);
        return res;
    }

    @Override
    public boolean updateTrackingValue(long id, long trackingValue) throws Exception {
        boolean res = syncInfoDao.updateTrackingValue(id, trackingValue);
        return res;
    }

    @Override
    public boolean delete(long id) throws Exception {
        boolean res = syncInfoDao.delete(id);
        return res;
    }

    @Override
    public List<SyncInfo> getSyncInfos() throws Exception {
        List<SyncInfo> res = syncInfoDao.getSyncInfos();
        return res;
    }
}
