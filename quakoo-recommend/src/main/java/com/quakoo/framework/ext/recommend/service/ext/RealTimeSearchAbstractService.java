package com.quakoo.framework.ext.recommend.service.ext;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.property.PropertyLoader;
import com.quakoo.framework.ext.recommend.bean.SearchRes;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class RealTimeSearchAbstractService implements RealTimeSearchService, InitializingBean {

    private PropertyLoader propertyLoader = PropertyLoader.getInstance("dao.properties");

    private RestHighLevelClient esClient;

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
    }

    public abstract String getSearchIndex();
    public abstract String getSearchColumn();
    public abstract String getSearchTime();
    public abstract List<String> getSearchResColumns();
    public abstract void handleFilter(List<SearchRes> list);

    @Override
    public List<SearchRes> searchByTime() throws Exception {
        String time = getSearchTime();
        String index = getSearchIndex();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(5000);
        sourceBuilder.sort(time, SortOrder.DESC);
        sourceBuilder.timeout(new TimeValue(3, TimeUnit.SECONDS));
        SearchRequest searchRequest = new SearchRequest(index); //索引
        searchRequest.source(sourceBuilder);
        sourceBuilder.fetchSource(new String[] {"id", time}, new String[] {});
        SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = response.getHits();
        List<SearchRes> res = Lists.newArrayList();
        for(SearchHit hit : hits) {
            long id = Long.parseLong(hit.getSourceAsMap().get("id").toString());
            long timeValue = Long.parseLong(hit.getSourceAsMap().get(time).toString());
            SearchRes searchRes = new SearchRes(id, 0, timeValue);
            Map<String, String> columns = Maps.newHashMap();
            if(getSearchResColumns() != null && getSearchResColumns().size() > 0) {
                for(String columnName : getSearchResColumns()) {
                    Object columnValueObj = hit.getSourceAsMap().get(columnName);
                    String columnValue = "";
                    if(columnValueObj != null) columnValue = columnValueObj.toString();
                    columns.put(columnName, columnValue);
                }
            }
            searchRes.setColumns(columns);
            res.add(searchRes);
        }
        handleFilter(res);
        return res;
    }

    @Override
    public List<SearchRes> search(List<String> words) throws Exception {
        String column = getSearchColumn();
        String index = getSearchIndex();
        String time = getSearchTime();
        BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        for(int i = 0; i < words.size(); i++) {
            int boost = words.size() - i;
            String word = words.get(i);
            QueryBuilder queryBuilder = QueryBuilders.termQuery(column, word).boost(boost);
            boolBuilder.should(queryBuilder);
        }
        sourceBuilder.query(boolBuilder);
        sourceBuilder.size(5000);
        sourceBuilder.sort("_score", SortOrder.DESC);
        sourceBuilder.sort("id", SortOrder.DESC);
        sourceBuilder.timeout(new TimeValue(3, TimeUnit.SECONDS));
        SearchRequest searchRequest = new SearchRequest(index); //索引
        searchRequest.source(sourceBuilder);
        sourceBuilder.fetchSource(new String[] {"id", time}, new String[] {});
        SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = response.getHits();
        List<SearchRes> res = Lists.newArrayList();
        for(SearchHit hit : hits) {
            long id = Long.parseLong(hit.getSourceAsMap().get("id").toString());
            long timeValue = Long.parseLong(hit.getSourceAsMap().get(time).toString());
            float score = hit.getScore();
            SearchRes searchRes = new SearchRes(id, score, timeValue);
            res.add(searchRes);
        }
        handleFilter(res);
        return res;
    }

}
