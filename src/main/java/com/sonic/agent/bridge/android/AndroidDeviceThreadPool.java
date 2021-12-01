package com.sonic.agent.bridge.android;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ZhouYiXun
 * @des 所有安卓相关线程都放这个线程池
 * @date 2021/08/16 19:26
 * todo 可以去掉了
 */
@Configuration
@ConditionalOnProperty(value = "modules.android.enable", havingValue = "true")
public class AndroidDeviceThreadPool {
    public static ExecutorService cachedThreadPool;

    @Bean
    public void androidThreadPoolInit() {
        // todo 线程池无线塞任务（Integer.MAX_VALUE）
        cachedThreadPool = Executors.newCachedThreadPool();
    }
}