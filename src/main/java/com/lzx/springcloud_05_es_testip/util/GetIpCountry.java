package com.lzx.springcloud_05_es_testip.util;

import com.lzx.springcloud_05_es_testip.conf.param.EsParam;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * @author 林子翔
 * @since 2022 08 2022/8/19
 */
public class GetIpCountry {
    public static String getCounty(String ip) throws IOException, GeoIp2Exception {

        File database = new File(EsParam.GEOIP_MMDB_PATH);

// This reader object should be reused across lookups as creation of it is
// expensive.
        DatabaseReader reader = new DatabaseReader.Builder(database).build();

// If you want to use caching at the cost of a small (~2MB) memory overhead:
// new DatabaseReader.Builder(file).withCache(new CHMCache()).build();

        InetAddress ipAddress = InetAddress.getByName(ip);

        CountryResponse response = reader.country(ipAddress);

        return response.getCountry().getIsoCode();
    }
}
