package com.bitforum.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class ThreadPoolService {
    
    private final ExecutorService executor = new ThreadPoolExecutor(
        2,      //核心线程数（平常常驻2个）
        5,      //最大线程数（忙时最多扩到5个）
        60,     //空闲线程存活时间（秒）
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(100),  //任务队列（最多积压100个任务等待）
        new ThreadPoolExecutor.CallerRunsPolicy()   //拒绝策略：队列满了让主线程自己执行
    );

    //提交一个任务给线程池处理
    public void submit(Runnable task) {
        executor.execute(task);
    }
}
