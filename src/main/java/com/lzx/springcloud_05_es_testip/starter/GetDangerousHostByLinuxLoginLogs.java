package com.lzx.springcloud_05_es_testip.starter;

import com.lzx.springcloud_05_es_testip.controller.DistinctByFrequent;
import com.lzx.springcloud_05_es_testip.controller.DistinctByLinuxLoginLogs;

/**
 * @author 林子翔
 * @since 2022 08 2022/8/18
 */
public class GetDangerousHostByLinuxLoginLogs {
    public static void main(String[] args) {
        DistinctByLinuxLoginLogs distinctByLinuxLoginLogs = new DistinctByLinuxLoginLogs();
        Thread thread = new Thread(distinctByLinuxLoginLogs);
        thread.start();
    }
}
