package com.lzx.springcloud_05_es_testip.controller;

import com.lzx.springcloud_05_es_testip.conf.param.EsParam;
import lombok.SneakyThrows;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.*;

/**
 * @author 林子翔
 * @description 根据一段时间内的访问频次来判断危险主机
 * @since 2022/08/22
 */
public class DistinctByFrequent implements Runnable {

    //        创建客户端
    RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(new HttpHost(EsParam.ES_HOSTNAME, EsParam.ES_PORT, EsParam.SCHEME))
    );

    /**
     * @return {@link SearchSourceBuilder }
     * @author 林子翔
     * @description 构造查询条件，返回builder
     * @since 2022/08/18
     */
    public SearchSourceBuilder getBuilder() {
        return new SearchSourceBuilder()
                .query(QueryBuilders.boolQuery()
//                        统计当前30s内的流量。配置缓冲，防止遗漏
                        .must(QueryBuilders.rangeQuery("@timestamp")
                                .gte(new Date().getTime() - EsParam.LOOP_MILLISECOND - EsParam.BUFFER_TIME))
//                        忽略服务器向外输出的流量
                        .mustNot(QueryBuilders.matchQuery("client.ip.keyword", EsParam.HOSTNAME))
                        .mustNot(QueryBuilders.matchQuery("client.ip.keyword", EsParam.HOSTNAME_IPV6)))
                .aggregation(AggregationBuilders
//                        按照客户端ip分组，方便后续统计
                        .terms("clientGroup").field("client.ip.keyword"));
    }


    /**
     * @return {@link SearchRequest }
     * @author 林子翔
     * @description 获取builder查询条件，构造并返回请求
     * @since 2022/08/18
     */
    public SearchRequest getRequest() {
        //            获得builder
        SearchSourceBuilder builder = getBuilder();
        SearchRequest request = new SearchRequest();
        request.indices(EsParam.INDEX_NAME);
        request.source(builder);
        return request;
    }


    /**
     * @param response              获取回应
     * @param dangerousHost_history 获取一天的历史危险主机名单
     * @author 林子翔
     * @description 打印危险主机
     * @since 2022/08/18
     */
    public void getDangerousHost(SearchResponse response, HashMap<String, Integer> dangerousHost_history) {
        //        打印结果
        System.out.println("查询耗时 = " + response.getTook());

        SearchHits hits = response.getHits();
        System.out.println("总命中数 = " + hits.getTotalHits());

        Terms terms = response.getAggregations().get("clientGroup");
        String curIp;
        System.out.println("+------------------------------------------------------");
        System.out.println("|当前访问：");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            curIp = bucket.getKeyAsString();
            dangerousHost_history.put(curIp, dangerousHost_history.getOrDefault(curIp, 0) + 1);
            System.out.println("|ip = " + curIp + " 在本次查询内的命中数为 = " + bucket.getDocCount());
        }
        System.out.println("+------------------------------------------------------");
        System.out.println("|历史访问频次，每隔" + EsParam.LOOP_MILLISECOND / 1000 + "秒ip出现的次数：");
        for (Map.Entry<String, Integer> entry : dangerousHost_history.entrySet()) {
            System.out.println("|ip = " + entry.getKey() + " 的统计频次为 = " + entry.getValue());
        }
        System.out.println("+------------------------------------------------------");
    }


    @SneakyThrows
    @Override
    public void run() {
        HashMap<String, Integer> dangerousHost_history = new HashMap<>();
        while (true) {
//            创建请求
            SearchRequest request = getRequest();

//            发送请求，返回结果
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

//            打印危险主机
            getDangerousHost(response, dangerousHost_history);

//            间隔30s查询一次
            Thread.sleep(EsParam.LOOP_MILLISECOND);
        }
    }
}
