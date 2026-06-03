package com.bitforum.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;

    //文章浏览量+1，返回最新浏览量
    public Long increaseViews(Long articleId) {
        String key = "article:" + articleId + ":views";
        return redisTemplate.opsForValue().increment(key);
    }

    //查当前浏览量
    public Long getViews(Long articleId) {
        String value = redisTemplate.opsForValue().get("article:" + articleId + ":views");
        return value == null ? 0L : Long.parseLong(value);
    }

    //文章点赞
    public void like(Long articleId, Long userId) {
        redisTemplate.opsForSet().add("article:" + articleId + ":likes", userId.toString());
    }

    //取消文章点赞
    public void unlike(Long articleId, Long userId) {
        redisTemplate.opsForSet().remove("article:" + articleId + ":likes", userId.toString());
    }

    //查询用户是否已点赞该文章
    public boolean hasLiked(Long articleId, Long userId) {
        Boolean b = redisTemplate.opsForSet().isMember("article:" + articleId + ":likes", userId.toString());
        return b != null && b;
    }

    //获取点赞数量
    public Long getLikeCount(Long articleId) {
        Long size = redisTemplate.opsForSet().size("article:" + articleId + ":likes");
        return size == null ? 0L : size;
    }

    //增加热度，浏览一次增加1热度
    public void incrHot(Long articleId) {
        redisTemplate.opsForZSet().incrementScore("article:hot", articleId.toString(), 1);
    }

    //获取文章热度集合
    public Set<ZSetOperations.TypedTuple<String>> getHotList(int topN) {
        return redisTemplate.opsForZSet().reverseRangeWithScores("article:hot", 0, topN - 1);
    }
}
