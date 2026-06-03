package com.bitforum.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    
    //监听article.publish.queue队列
    @RabbitListener(queues = "article.publish.queue")
    public void handlePublish(String message) {
        log.info("收到消息：{}", message);
        //处理更新积分，发通知等耗时操作
        //异步任务：发布后的慢活扔给线程池（不阻塞用户）
        try{
        Thread.sleep(2000); //模拟耗时操作
        }catch (InterruptedException e){
        Thread.currentThread().interrupt();
        }     
        log.info("文章发布完成，主线程已返回（异步任务还在后台跑）");
    }
}
