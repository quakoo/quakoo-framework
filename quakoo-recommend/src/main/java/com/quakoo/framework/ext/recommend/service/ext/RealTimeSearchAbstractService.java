package com.quakoo.framework.ext.recommend.service.ext;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.property.PropertyLoader;
import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import com.quakoo.framework.ext.recommend.bean.SearchRes;
import com.quakoo.framework.ext.recommend.service.impl.RecommendServiceImpl;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class RealTimeSearchAbstractService implements RealTimeSearchService, InitializingBean {

    private PropertyLoader propertyLoader = PropertyLoader.getInstance("dao.properties");

    Logger logger = LoggerFactory.getLogger(RealTimeSearchAbstractService.class);

    @Resource
    private AbstractRecommendInfo recommendInfo;

    private RestHighLevelClient esClient;

    private JedisX cache;

    private String search_time_queue_key = "%s_search_time_all_queue";
    private String search_words_queue_key = "%s_search_words_all_queue_%s";

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

    public abstract String getSearchIndex();
    public abstract String getSearchColumn();
    public abstract String getSearchTime();
    public abstract int getSearchSize();
    public abstract List<String> getSearchResColumns();
    public abstract void handleFilter(List<SearchRes> list, long uid);

    private List<SearchRes> _searchByTime() throws Exception {
        String key = String.format(search_time_queue_key, recommendInfo.projectName);
        Set<Object> set = cache.zrevrangeByScoreObject(key, Double.MAX_VALUE, 0, null);
        if (set.size() > 0) {
            List<SearchRes> res = Lists.newArrayList();
            for (Object obj : set) {
                res.add((SearchRes) obj);
            }
            return res;
        } else {
            long startTime = System.currentTimeMillis();
            String time = getSearchTime();
            String index = getSearchIndex();
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.size(getSearchSize());
            sourceBuilder.sort(time, SortOrder.DESC);
            sourceBuilder.timeout(new TimeValue(3, TimeUnit.SECONDS));
            SearchRequest searchRequest = new SearchRequest(index); //索引
            searchRequest.source(sourceBuilder);
            List<String> includes = Lists.newArrayList();
            includes.add("id");
            if(getSearchResColumns() != null && getSearchResColumns().size() > 0)
                includes.addAll(getSearchResColumns());
            includes.add(time);
            sourceBuilder.fetchSource(includes.toArray(new String[0]), new String[] {});
            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
            logger.info("searchByTime time : " + (System.currentTimeMillis() - startTime));
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
            if (res.size() > 0) {
                Map<Object, Double> redisMap = Maps.newHashMap();
                for (SearchRes one : res) {
                    redisMap.put(one, (double) one.getTime());
                }
                cache.zaddMultiObject(key, redisMap);
                cache.expire(key, AbstractRecommendInfo.redis_search_overtime);
            }
            return res;
        }
    }

    @Override
    public List<SearchRes> searchByTime(long uid) throws Exception {
        List<SearchRes> res = _searchByTime();
        handleFilter(res, uid);
        return res;
    }

    public List<SearchRes> _search(List<String> words) throws Exception {
        String wordsKey = StringUtils.join(words, "_");
        String key = String.format(search_words_queue_key, recommendInfo.projectName, wordsKey);
        Set<Object> set = cache.zrevrangeByScoreObject(key, Double.MAX_VALUE, 0, null);
        if (set.size() > 0) {
            List<SearchRes> res = Lists.newArrayList();
            for (Object obj : set) {
                res.add((SearchRes) obj);
            }
            return res;
        } else {
            long startTime = System.currentTimeMillis();
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
            sourceBuilder.size(getSearchSize());
            sourceBuilder.sort("_score", SortOrder.DESC);
            sourceBuilder.sort("id", SortOrder.DESC);
            sourceBuilder.timeout(new TimeValue(3, TimeUnit.SECONDS));
            SearchRequest searchRequest = new SearchRequest(index); //索引
            searchRequest.source(sourceBuilder);
            List<String> includes = Lists.newArrayList();
            includes.add("id");
            if(getSearchResColumns() != null && getSearchResColumns().size() > 0)
                includes.addAll(getSearchResColumns());
            includes.add(time);
            sourceBuilder.fetchSource(includes.toArray(new String[0]), new String[] {});
            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
            logger.info("searchByWords time : " + (System.currentTimeMillis() - startTime));
            SearchHits hits = response.getHits();
            List<SearchRes> res = Lists.newArrayList();
            for(SearchHit hit : hits) {
                long id = Long.parseLong(hit.getSourceAsMap().get("id").toString());
                long timeValue = Long.parseLong(hit.getSourceAsMap().get(time).toString());
                float score = hit.getScore();
                SearchRes searchRes = new SearchRes(id, score, timeValue);
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
            if (res.size() > 0) {
                Map<Object, Double> redisMap = Maps.newHashMap();
                for (SearchRes one : res) {
                    redisMap.put(one, one.getScore());
                }
                cache.zaddMultiObject(key, redisMap);
                cache.expire(key, AbstractRecommendInfo.redis_search_overtime);
            }
            return res;
        }
    }

    @Override
    public List<SearchRes> search(List<String> words, long uid) throws Exception {
        List<SearchRes> res = _search(words);
        handleFilter(res, uid);
        return res;
    }

}
