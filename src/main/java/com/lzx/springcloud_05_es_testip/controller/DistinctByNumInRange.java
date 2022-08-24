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

import java.util.Date;
import java.util.HashSet;

/**
 * @author 林子翔
 * @description 根据一定时间内的访问数量来判断是否为危险主机
 * @since 2022/08/22
 */
public class DistinctByNumInRange implements Runnable {

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
    public void getDangerousHost(SearchResponse response, HashSet<String> dangerousHost_history) {
        HashSet<String> dangerousHost = new HashSet<>();
        //        打印结果
        System.out.println("查询耗时 = " + response.getTook());

        SearchHits hits = response.getHits();
        System.out.println("总命中数 = " + hits.getTotalHits());

        Terms terms = response.getAggregations().get("clientGroup");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            if (bucket.getDocCount() > EsParam.DANGEROUS_NUM) {
                dangerousHost_history.add(bucket.getKeyAsString());
                dangerousHost.add(bucket.getKeyAsString());
            }
            System.out.println("ip = " + bucket.getKeyAsString() + " 的命中数为 = " + bucket.getDocCount());
        }

        System.out.println("+------------------------------------------------------");
        for (String s : dangerousHost) {
            System.out.println("|最近" + EsParam.LOOP_MILLISECOND / 1000 + "秒，危险ip = " + s);
        }
        for (String s : dangerousHost_history) {
            System.out.println("|当天的危险ip = " + s);
        }
        System.out.println("+------------------------------------------------------");
    }


    @SneakyThrows
    @Override
    public void run() {
        HashSet<String> dangerousHost_history = new HashSet<>();
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
