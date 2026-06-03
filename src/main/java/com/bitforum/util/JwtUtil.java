package com.bitforum.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtUtil {

    // 三个核心类的关系：
    // Jwts.builder()—造 Token（往里面塞数据→签名→打包）
    // Jwts.parser()—拆 Token（验证签名→解包→读数据）
    // Claims—Token 解析后的结果对象，.get("userId")取值、.getExpiration()查过期
    
    //配置————这两个值后面从application.yml里面读
    //密钥，至少256位
    private static final String SECRET = "bit-forum-secret-key-2026-must-256-bit!!";
    //7天(毫秒)
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000L;
    
    //把密钥字符串转成 SecreKey 对象(jjwt要求的格式)
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * 生成 Token
     * 
     * @param userId   用户ID，塞进 Payload
     * @param username 用户名，塞进 subject
     * @return JWT 字符串（三段 Base64 用 . 连起来）
    */

    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expireDate = new Date(System.currentTimeMillis()+ EXPIRATION);

        return Jwts.builder()
                .subject(username)          //主题 = 用户名
                .claim("userId", userId)    //自定义字段(key-value)
                .issuedAt(now)              //签发时间
                .expiration(expireDate)     //过期时间
                .signWith(KEY)              //用密钥签名
                .compact();                 //打包成字符串
    }
    
    /**
     * 解析 Token → 拿出里面的信息
     * 如果 Token 被篡改过或过期了，会自动抛异常
    */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)          //用同一把密钥验证签名
                .build()
                .parseSignedClaims(token) //解析
                .getPayload();            //拿到 Payload 部分
    }
    
    /**
     * 检查 Token 是否过期
    */
    public static boolean isTokenExpired(String token) {
        return parseToken(token)
                .getExpiration()        //拿到过期时间
                .before(new Date());
    }
   
    //从Token里取具体数据
    public static Long getUserId(String token) {
        //"userId"对应generateToken里Claim的key
        return parseToken(token).get("userId", Long.class);
    }

    public static String getUsername(String token) {
        return parseToken(token).getSubject();  //subject就是用户名
    }
}
