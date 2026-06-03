package com.bitforum.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)        //自增主键是id    
    private Long id;
    private String content;
    private Long userId;
    private Long articleId;
    private Long parentCommentId;       //回复给谁
    private LocalDateTime createTime;
}
