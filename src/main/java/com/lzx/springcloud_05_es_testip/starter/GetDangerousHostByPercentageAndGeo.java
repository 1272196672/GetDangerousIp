package com.lzx.springcloud_05_es_testip.starter;

import com.lzx.springcloud_05_es_testip.controller.DistinctByPercentageAndGeo;


/**
 * @author 林子翔
 * @since 2022 08 2022/8/19
 */
public class GetDangerousHostByPercentageAndGeo {
    public static void main(String[] args) {
        DistinctByPercentageAndGeo distinctByPercentageAndGeo = new DistinctByPercentageAndGeo();
        Thread thread = new Thread(distinctByPercentageAndGeo);
        thread.start();
    }
}
