package com.lzx.springcloud_05_es_testip.controller;

import com.lzx.springcloud_05_es_testip.conf.param.EsParam;
import com.lzx.springcloud_05_es_testip.util.GetIpCountry;
import com.maxmind.geoip2.exception.GeoIp2Exception;
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

import java.io.IOException;

/**
 * @author 林子翔
 * @description 根据访问量百分比来判断是否为危险主机
 * @since 2022/08/22
 */
public class DistinctByPercentageAndGeo implements Runnable {

    //        创建客户端
    RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(new HttpHost(EsParam.ES_HOSTNAME, EsParam.ES_PORT, EsParam.SCHEME))
    );

    /**
     * @return {@link SearchSourceBuilder }
     * @author 林子翔
     * @description 构造查询条件，返回builder
     * @since 2022/08/22
     */
    public SearchSourceBuilder getBuilder() {
        return new SearchSourceBuilder()
                .query(QueryBuilders.boolQuery()
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
     * @since 2022/08/22
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
     * @param response 获取回应
     * @author 林子翔
     * @description 打印危险主机
     * @since 2022/08/22
     */
    public void getDangerousHost(SearchResponse response) throws IOException, GeoIp2Exception {
//        打印结果
        System.out.println("查询耗时 = " + response.getTook());

        SearchHits hits = response.getHits();
        System.out.println("总命中数 = " + hits.getTotalHits());


        Terms terms = response.getAggregations().get("clientGroup");
        double sum = 0;
        System.out.println("+------------------------------------------------------");
        System.out.println("|各ip 的访问数量");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            System.out.println("|ip = " + bucket.getKeyAsString() + " 访问数量为：" + bucket.getDocCount());
            sum += bucket.getDocCount();
        }

        System.out.println("+------------------------------------------------------");
        System.out.println("|各ip 的访问占比 与 地理位置");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            System.out.println(
                    "|ip = " + bucket.getKeyAsString() +
                            " 访问占比为：" + String.format("%.3f", (bucket.getDocCount() / sum) * 100) + "%"
//                    " 地域为：" + GetIpCountry.getCounty(bucket.getKeyAsString())
            );
        }
        System.out.println("+------------------------------------------------------");
        client.close();
    }

    @SneakyThrows
    @Override
    public void run() {
        //            创建请求
        SearchRequest request = getRequest();

//            发送请求，返回结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

//            打印危险主机
        getDangerousHost(response);
    }
}
