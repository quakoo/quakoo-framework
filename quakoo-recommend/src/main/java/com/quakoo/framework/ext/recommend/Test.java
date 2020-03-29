package com.quakoo.framework.ext.recommend;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.recommend.bean.ESField;
import com.quakoo.framework.ext.recommend.bean.SearchRes;
import com.quakoo.framework.ext.recommend.util.ESUtils;
import com.sun.org.apache.xalan.internal.xsltc.dom.SAXImpl;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.GaussDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Test {

    private static List<SearchRes> search(RestHighLevelClient esClient, SearchRes lastOne) throws Exception {
        List<String> words = Lists.newArrayList("疫情","考试","钢铁","大排查","沈文荣",
                "不锈", "德龙", "质量指标", "传真", "汉钢");
        List<String> searchResColumns = Lists.newArrayList("title", "uid", "characteristicId");
        String column = "title";
        String index = "article";
        String time = "lastUpdateTime";

        BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        for (int i = 0; i < words.size(); i++) {
            int boost = words.size() - i;
            String word = words.get(i);
            QueryBuilder queryBuilder = QueryBuilders.termQuery(column, word).boost(boost);
            boolBuilder.should(queryBuilder);
        }
        long origin = System.currentTimeMillis();
        long offset = 1000 * 60 * 60 * 24 * 7;
        long scale =  1000 * 60 * 60 * 24 * 7;
        double decay = 0.5;
        GaussDecayFunctionBuilder gaussDecayFunctionBuilder = ScoreFunctionBuilders.gaussDecayFunction(time, origin, scale, offset, decay);
        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(boolBuilder, gaussDecayFunctionBuilder);

        sourceBuilder.query(functionScoreQueryBuilder);
        sourceBuilder.size(2000);
        sourceBuilder.sort("_score", SortOrder.DESC);
        sourceBuilder.sort("id", SortOrder.DESC);
//        QueryBuilder filter = QueryBuilders.rangeQuery(time).gte(1582128000000l).lt(1585412601456l);
//        sourceBuilder.postFilter(filter);
        if(lastOne != null) {
            sourceBuilder.searchAfter(new Object[]{lastOne.getScore(), lastOne.getId()});
        }

        sourceBuilder.timeout(new TimeValue(3, TimeUnit.SECONDS));
        SearchRequest searchRequest = new SearchRequest(index); //索引
        searchRequest.source(sourceBuilder);
        List<String> includes = Lists.newArrayList();
        includes.add("id");
        if (searchResColumns != null && searchResColumns.size() > 0)
            includes.addAll(searchResColumns);
        includes.add(time);
        sourceBuilder.fetchSource(includes.toArray(new String[0]), new String[]{});
        long startTime = System.currentTimeMillis();
        SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

        System.out.println("=== time : " + (System.currentTimeMillis() - startTime) + " ,took : " +
                response.getTook().toString());
        SearchHits hits = response.getHits();
        List<SearchRes> res = Lists.newArrayList();
        for (SearchHit hit : hits) {
            long id = Long.parseLong(hit.getSourceAsMap().get("id").toString());
            long timeValue = Long.parseLong(hit.getSourceAsMap().get(time).toString());
            float score = hit.getScore();
            SearchRes searchRes = new SearchRes(id, score, timeValue);
            Map<String, String> columns = Maps.newHashMap();
            if (searchResColumns != null && searchResColumns.size() > 0) {
                for (String columnName : searchResColumns) {
                    Object columnValueObj = hit.getSourceAsMap().get(columnName);
                    String columnValue = "";
                    if (columnValueObj != null) columnValue = columnValueObj.toString();
                    columns.put(columnName, columnValue);
                }
            }
            searchRes.setColumns(columns);
            res.add(searchRes);
        }
        return res;

//        BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
//        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
////        for(String queryWord : words) {
//            QueryBuilder nameQuery = QueryBuilders.wildcardQuery("name", "*河北九江*");
//            boolBuilder.should(nameQuery);
////            QueryBuilder phoneQuery = QueryBuilders.wildcardQuery("phone", "*河北九江集团*");
////            boolBuilder.should(phoneQuery);
////        }
//        sourceBuilder.query(boolBuilder);
//        sourceBuilder.size(400);
//        sourceBuilder.sort("_score", SortOrder.DESC);
//        sourceBuilder.sort("id", SortOrder.DESC);
//        sourceBuilder.timeout(new TimeValue(3, TimeUnit.SECONDS));
//        SearchRequest searchRequest = new SearchRequest("user"); //索引
//        searchRequest.source(sourceBuilder);
//        sourceBuilder.fetchSource(new String[] {"id"}, new String[] {});
//
//        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
//        SearchHits hits = response.getHits();
//        for(SearchHit hit : hits) {
//            long id = hit.getFields().get("id").getValue();
//            System.out.println("======= score : " + hit.getScore() + ", id : " + id + ", " + hit.getSourceAsString());
//        }

//        BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
//        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//
////        QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("type", 3)).must(QueryBuilders.termQuery("title", "恐怖").boost(1));
////        boolBuilder.should(queryBuilder);
////        QueryBuilder queryBuilder2 = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("type", 3)).must(QueryBuilders.termQuery("title", "石景山").boost(2));
////        boolBuilder.should(queryBuilder2);
//
//        sourceBuilder.query(boolBuilder);
//        sourceBuilder.size(100);
//        sourceBuilder.sort("_score", SortOrder.DESC);
////        sourceBuilder.sort("id", SortOrder.DESC);
////        sourceBuilder.searchAfter(new Object[]{0.10230917, 3 });
//
//        sourceBuilder.sort("lastUpdateTime", SortOrder.DESC);
//
//        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
//        SearchRequest searchRequest = new SearchRequest("user"); //索引
//        searchRequest.source(sourceBuilder);
//        sourceBuilder.fetchSource(new String[] {"id","title","content","lastUpdateTime"}, new String[] {});
//
//        System.out.println(searchRequest.toString());
//
//        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
//        SearchHits hits = response.getHits();
//        System.out.println(hits.getHits().length + "   ===== size");
//        for(SearchHit hit : hits) {
////            long id = hit.getFields().get("id").getValue();
//            long id = Long.parseLong(hit.getSourceAsMap().get("id").toString());
//            System.out.println("======= score : " + hit.getScore() + ", id : " + id + ", " + hit.getSourceAsString());
//        }
    }


    private static void changeIndex(RestHighLevelClient client) throws Exception {
        List<IndexRequest> indexRequests = Lists.newArrayList();
        for(int i = 1; i <= 3; i++) {
            Map<String, Object> map = Maps.newLinkedHashMap();
            map.put("id", i);
            map.put("title", "测试" + i);
            map.put("content", "这是个测试测试" + i);
//            Obj obj = new Obj(i, "测试" + i, "这是个测试测试" + i);
            IndexRequest indexRequest = new IndexRequest("quakoo_test");
            indexRequest.id(String.valueOf(i));
            indexRequest.source(JsonUtils.toJson(map), XContentType.JSON);
            indexRequests.add(indexRequest);
        }
        BulkRequest bulkRequest = new BulkRequest();
        for (IndexRequest indexRequest : indexRequests) {
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        boolean sign = !bulkResponse.hasFailures();
        System.out.println("change : " + sign);
        if(!sign) System.out.println("change error msg : " + bulkResponse.buildFailureMessage());
    }

    private static void createIndex(RestHighLevelClient client, String json) throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest("quakoo_test");
        boolean exist = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        if (!exist) {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest("quakoo_test");
            createIndexRequest.settings(Settings.builder()
                    .put("index.number_of_shards", 1)
                    .put("index.number_of_replicas", 0)
                    .put("refresh_interval", "30s")
                    .put("index.blocks.read_only_allow_delete", "false")
            );
//            XContentBuilder builder = XContentFactory.jsonBuilder()
//                    .startObject()
//                    .field("properties")
//                    .startObject()
//                    .field("id").startObject().field("index", "true").field("type", "long").endObject()
//                    .field("title").startObject().field("index", "true").field("type", "text").field("analyzer","jieba_index").endObject()
//                    .field("content").startObject().field("index", "true").field("type", "text").field("analyzer","jieba_index").endObject()
//                    .endObject()
//                    .endObject();
//            createIndexRequest.mapping(builder);
            createIndexRequest.mapping(json, XContentType.JSON);
            CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            boolean createSign = createIndexResponse.isAcknowledged();
            System.out.println("createSign : " + createSign);
        } else {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("quakoo_test");
            AcknowledgedResponse response = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            boolean deleteSign = response.isAcknowledged();
            System.out.println("deleteSign : " + deleteSign);
        }
    }

    public static void main(String[] args) throws Exception  {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", "steel123"));
        RestClientBuilder builder = RestClient.builder(new HttpHost[] { new HttpHost("47.92.109.133", 9200)})
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder)
            {
                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
        });
        RestHighLevelClient client = new RestHighLevelClient(builder);

        List<SearchRes> list = search(client, null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Set<String> set = Sets.newLinkedHashSet();
        for(SearchRes one : list) {
//            System.out.println(one.getId());
//            System.out.println(one.getScore());
//            System.out.println(sdf.format(one.getTime()));
            set.add(sdf.format(one.getTime()));
//            System.out.println(one.getColumns().toString());
//            System.out.println("=================================");
        }
        for(String one : set) {
            System.out.println(one);
        }
        System.out.println(list.size());

//        list = search(client, list.get(list.size() - 1));
//        for(SearchRes one : list) {
//            System.out.println(one.getId());
//            System.out.println(one.getScore());
//            System.out.println(one.getColumns().toString());
//            System.out.println("=================================");
//        }
//        System.out.println(list.size());

//        List<ESField> list = Lists.newArrayList();
//        ESField a = new ESField("id", "true", "long", null, null);
//        ESField b = new ESField("title", "true", "text", null,null);
//        ESField c = new ESField("content","true", "text", null, null);
//        list.add(a);
//        list.add(b);
//        list.add(c);
//        String json = ESUtils.toIndexJson(list);
//        createIndex(client, json);

//        changeIndex(client);

        client.close();
    }

}
