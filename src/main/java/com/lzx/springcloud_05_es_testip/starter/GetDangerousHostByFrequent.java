package com.lzx.springcloud_05_es_testip.starter;

import com.lzx.springcloud_05_es_testip.controller.DistinctByFrequent;
import com.lzx.springcloud_05_es_testip.controller.DistinctByNumInRange;

/**
 * @author 林子翔
 * @since 2022 08 2022/8/18
 */
public class GetDangerousHostByFrequent {
    public static void main(String[] args) {
        DistinctByFrequent distinctByFrequent = new DistinctByFrequent();
        Thread thread = new Thread(distinctByFrequent);
        thread.start();
    }
}
