package com.lzx.springcloud_05_es_testip.controller;

import com.lzx.springcloud_05_es_testip.conf.param.EsParam;
import lombok.SneakyThrows;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;


/**
 * @author 林子翔
 * @description 根据一定时间内的访问数量来判断是否为危险主机
 * @since 2022/08/22
 */
public class DistinctByLinuxLoginLogs implements Runnable {

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
                .aggregation(AggregationBuilders
                        .composite("buckets", Arrays.asList(
                                new TermsValuesSourceBuilder("clientGroup").field("clientIp.keyword"),
                                new TermsValuesSourceBuilder("accountGroup").field("account.keyword"),
                                new TermsValuesSourceBuilder("statusGroup").field("status.keyword")
                        )));
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
        request.indices(EsParam.LINUX_LOGS_INDEX_NAME);
        request.source(builder);
        return request;
    }


    /**
     * @param response 获取回应
     * @author 林子翔
     * @description 打印危险主机
     * @since 2022/08/18
     */
    public void getDangerousHost(SearchResponse response) throws IOException {
        HashSet<String> dangerousIp = new HashSet<>();
        HashMap<String, HashMap<String, Integer>> failedLoginIp = new HashMap<>();
        HashMap<String, HashSet<String>> acceptedLoginIp = new HashMap<>();
        //        打印结果
        System.out.println("查询耗时 = " + response.getTook());

        SearchHits hits = response.getHits();
        System.out.println("总命中数 = " + hits.getTotalHits());

        ParsedComposite parsedComposite = response.getAggregations().get("buckets");
        System.out.println("+---------------------------------------------------------------------");
        System.out.println("|记录：");
        String ip;
        String account;
        String status;
        int frequent;
        for (ParsedComposite.ParsedBucket bucket : parsedComposite.getBuckets()) {
            ip = bucket.getKey().get("clientGroup").toString();
            account = bucket.getKey().get("accountGroup").toString();
            status = bucket.getKey().get("statusGroup").toString();
            frequent = (int) bucket.getDocCount();
            System.out.println("|ip = " + ip
                    + " 的主机尝试登录账号 " + account
                    + " " + status
                    + " " + frequent + " 次");

//            将登录成功的主机加入acceptedLoginIp，失败的加入failedLoginIp
            if (status.equals("Accepted")){
                HashSet<String> accounts = acceptedLoginIp.getOrDefault(ip, new HashSet<>());
                accounts.add(account);
                acceptedLoginIp.put(ip, accounts);
            } else {
                HashMap<String, Integer> accountsWithNum = failedLoginIp.getOrDefault(ip, new HashMap<>());
                accountsWithNum.put(account, frequent);
                failedLoginIp.put(ip, accountsWithNum);
            }
        }
        System.out.println("+---------------------------------------------------------------------");
        System.out.println("|风险管理：");
//        首先取出某ip 尝试某主机的记录
        for (Map.Entry<String, HashMap<String, Integer>> entry : failedLoginIp.entrySet()) {
//            再取出某主机失败的次数
            for (Map.Entry<String, Integer> integerEntry : entry.getValue().entrySet()) {
//                如果此主机失败次数超过限制，会加以提示
                if (integerEntry.getValue() > EsParam.LINUX_LOGIN_FREQUENT){
                    ip = entry.getKey();
                    account = integerEntry.getKey();
                    System.out.println("|ip = " + ip
                            + " 的主机尝试登录账号 " + account
                            + " 失败次数过多");
//                    如果此主机失败次数超过限制，并且某一次还登录成功了，我们将其认定为具有危险性
                    if (acceptedLoginIp.get(entry.getKey()).contains(integerEntry.getKey())){
                        dangerousIp.add(ip);
                        System.out.println("|ip = " + ip
                                + " 的主机尝试登录账号 " + account
                                + " 失败多次后成功登录，系统认为其具备很大的危险，请尽快排查");
                    }
                }
            }
        }
        System.out.println("+---------------------------------------------------------------------");
        System.out.println("|系统认为的危险ip：");
        for (String s : dangerousIp) {
            System.out.println("|ip = " + s);
        }
        System.out.println("+---------------------------------------------------------------------");
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
