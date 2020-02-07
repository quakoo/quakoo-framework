package com.quakoo.framework.ext.recommend;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.framework.ext.recommend.bean.ESField;
import com.quakoo.framework.ext.recommend.util.ESUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Test {

    private static void search(RestHighLevelClient client) throws Exception {
        BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

//        QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("type", 3)).must(QueryBuilders.termQuery("title", "恐怖").boost(1));
//        boolBuilder.should(queryBuilder);
//        QueryBuilder queryBuilder2 = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("type", 3)).must(QueryBuilders.termQuery("title", "石景山").boost(2));
//        boolBuilder.should(queryBuilder2);

        sourceBuilder.query(boolBuilder);
        sourceBuilder.size(100);
        sourceBuilder.sort("_score", SortOrder.DESC);
//        sourceBuilder.sort("id", SortOrder.DESC);
//        sourceBuilder.searchAfter(new Object[]{0.10230917, 3 });

        sourceBuilder.sort("lastUpdateTime", SortOrder.DESC);

        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        SearchRequest searchRequest = new SearchRequest("article"); //索引
        searchRequest.source(sourceBuilder);
        sourceBuilder.fetchSource(new String[] {"id","title","content","lastUpdateTime"}, new String[] {});

        System.out.println(searchRequest.toString());

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = response.getHits();
        System.out.println(hits.getHits().length + "   ===== size");
        for(SearchHit hit : hits) {
//            long id = hit.getFields().get("id").getValue();
            long id = Long.parseLong(hit.getSourceAsMap().get("id").toString());
            System.out.println("======= score : " + hit.getScore() + ", id : " + id + ", " + hit.getSourceAsString());
        }
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
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("47.92.109.133", 9200, "http")));

//        search(client);
//"jieba_index"
        List<ESField> list = Lists.newArrayList();
        ESField a = new ESField("id", "true", "long", null, null);
        ESField b = new ESField("title", "true", "text", null,null);
        ESField c = new ESField("content","true", "text", null, null);
        list.add(a);
        list.add(b);
        list.add(c);
        String json = ESUtils.toIndexJson(list);
        createIndex(client, json);

//        changeIndex(client);

        client.close();
    }

}
