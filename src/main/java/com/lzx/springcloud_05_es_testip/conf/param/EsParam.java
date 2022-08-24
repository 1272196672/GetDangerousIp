package com.lzx.springcloud_05_es_testip.conf.param;

/**
 * @author 林子翔
 * @since 2022 08 2022/8/18
 */
public class EsParam {
    //    ##一般不变##
    //    ES ip
    public static final String ES_HOSTNAME = "192.168.10.120";
    //    服务器ip
    public static final String HOSTNAME = "192.168.10.120";
    //    服务器ipv6
    public static final String HOSTNAME_IPV6 = "fe80::20c:29ff:fe4b:763a";
    //    ES端口
    public static final Integer ES_PORT = 9200;
    //    传输协议
    public static final String SCHEME = "http";

    //    ##建议配置##
    //    索引名称
    public static final String INDEX_NAME = "pb-7.15.1-2022.08.22";
    //    linux登录日志索引名称
    public static final String LINUX_LOGS_INDEX_NAME = "fb-linux-login-logs-7.15.1-2022.08.24";
    //    linux登录失败多少次判断为危险主机
    public static final Integer LINUX_LOGIN_FREQUENT = 5;
    //    多少ms循环一次
    public static final Integer LOOP_MILLISECOND = 10000;
    //    缓冲时间，预留出采集+传输+分析+显示的时间，5s
    public static final Integer BUFFER_TIME = 5000;
    //    超过多少条算危险ip
    public static final Long DANGEROUS_NUM = 4000L;
    //    ip文件位置
    public static final String GEOIP_MMDB_PATH = "D:\\Download_Code\\GeoLite2-Country_20220819\\GeoLite2-Country.mmdb";

}
