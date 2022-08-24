package com.lzx.springcloud_05_es_testip;

import com.lzx.springcloud_05_es_testip.conf.param.EsParam;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
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
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashSet;

@SpringBootTest
class Springcloud05EsTestIpApplicationTests {

    //        创建客户端
    RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(new HttpHost(EsParam.ES_HOSTNAME, EsParam.ES_PORT, EsParam.SCHEME))
    );

    @Test
    public void run() throws IOException {
        //        构造builder
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .query(QueryBuilders.boolQuery()
//                        忽略服务器向外输出的流量
                        .mustNot(QueryBuilders.matchQuery("client.ip.keyword", EsParam.HOSTNAME))
                        .mustNot(QueryBuilders.matchQuery("client.ip.keyword", EsParam.HOSTNAME_IPV6)))
                .aggregation(AggregationBuilders
//                        按照客户端ip分组，方便后续统计
                        .terms("clientGroup").field("client.ip.keyword"));

//        创建请求
        SearchRequest request = new SearchRequest();
        request.indices(EsParam.INDEX_NAME);
        request.source(builder);

//        发送请求，返回结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

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
        System.out.println("|各ip 的访问占比");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            System.out.println("|ip = " + bucket.getKeyAsString() + " 访问占比为：" + String.format("%.3f", (bucket.getDocCount() / sum) * 100) + "%");
        }
        System.out.println("+------------------------------------------------------");


//        System.out.println("+------------------------------------------------------");
//        for (String s : dangerousHost) {
//            System.out.println("|最近" + EsParam.LOOP_MILLISECOND / 1000 + "秒，危险ip = " + s);
//        }
//        for (String s : dangerousHost_history) {
//            System.out.println("|当天危险ip = " + s);
//        }
//        System.out.println("+------------------------------------------------------");

    }

    @Test
    void getCounty() throws IOException, GeoIp2Exception {
        File database = new File(EsParam.GEOIP_MMDB_PATH);

// This reader object should be reused across lookups as creation of it is
// expensive.
        DatabaseReader reader = new DatabaseReader.Builder(database).build();

// If you want to use caching at the cost of a small (~2MB) memory overhead:
// new DatabaseReader.Builder(file).withCache(new CHMCache()).build();

        InetAddress ipAddress = InetAddress.getByName("47.75.18.66");

        CountryResponse response = reader.country(ipAddress);

        Country country = response.getCountry();
        System.out.println(country.getIsoCode());
    }
}
