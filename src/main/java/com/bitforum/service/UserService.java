package com.bitforum.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.dto.AdminUserResponse;
import com.bitforum.dto.PublicUserProfileResponse;
import com.bitforum.dto.UserProfileResponse;
import com.bitforum.dto.UserProfileUpdateRequest;
import com.bitforum.entity.Article;
import com.bitforum.entity.ArticleFavorite;
import com.bitforum.entity.User;
import com.bitforum.entity.UserFollow;
import com.bitforum.mapper.ArticleFavoriteMapper;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.mapper.UserFollowMapper;
import com.bitforum.mapper.UserMapper;

@Service
public class UserService {
    public static final String ROLE_ADMIN = "ADMIN";
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    private static final String DEFAULT_ROLE = "USER";

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private ArticleFavoriteMapper articleFavoriteMapper;
    @Autowired
    private UserFollowMapper userFollowMapper;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    // 注册：查重 → 设默认 role=USER、status=1 → BCrypt 加密密码 → 入库
    public User register(String username, String password) {
        log.info("注册请求：用户名={}", username);
        // 1. 查重：用 QueryWrapper 替代手写 SQL
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username); // WHERE 1.username=2.username
        User existing = userMapper.selectOne(wrapper);
        if (existing != null) {
            log.warn("注册失败：用户名已存在({})", username);
            return null;
        }
        // 2. 保存：insert 是 BaseMapper 自带的
        User user = new User();
        user.setUsername(username);
        user.setRole(DEFAULT_ROLE);
        user.setStatus(STATUS_ENABLED);
        // user.setPassword(password);
        // 存加密密码
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(password)); // 存加密后的密文
        userMapper.insert(user);
        log.info("注册成功：用户ID={}", user.getId());
        return user;
    }

    // 登录：查用户名 → 校验 status=1 → 校验 BCrypt 密码 → 通过返回 User，否则 null
    public User login(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User user = userMapper.selectOne(wrapper);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (user != null
                && Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())
                && encoder.matches(password, user.getPassword())) {
            log.info("登录成功！用户的名称是{}", username);
            return user;
        }
        log.info("登录失败，请重新确认用户名和密码！");
        return null;
    }

    // 按 ID 查用户：给 AdminInterceptor 回查 role 和 status 用的
    public User findById(Long userId) {
        return userMapper.selectById(userId);
    }

    public UserProfileResponse getCurrentProfile(Long userId) {
        User user = requireUser(userId);
        return UserProfileResponse.from(user, countPublishedArticles(userId), countFavorites(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = requireUser(userId);
        user.setAvatar(request.getAvatar());
        user.setNickname(request.getNickname());
        user.setBio(request.getBio());
        userMapper.updateById(user);

        User saved = userMapper.selectById(userId);
        if (saved == null) {
            saved = user;
        }
        return UserProfileResponse.from(saved, countPublishedArticles(userId), countFavorites(userId));
    }

    public PublicUserProfileResponse getPublicProfile(Long userId) {
        return getPublicProfile(userId, null);
    }

    public PublicUserProfileResponse getPublicProfile(Long userId, Long currentUserId) {
        User user = requireUser(userId);
        return PublicUserProfileResponse.from(
                user,
                countPublishedArticles(userId),
                countFavorites(userId),
                countFollowing(userId),
                countFollowers(userId),
                isFollowing(currentUserId, userId));
    }

    // 管理员查用户列表（分页）：查 User → 转 AdminUserResponse 脱敏 password
    public Page<AdminUserResponse> pageUsers(long pageNum, long pageSize) {
        Page<User> userPage = new Page<>(pageNum, pageSize);
        // 查出分页的 User 对象
        Page<User> pageResult = userMapper.selectPage(userPage, null);

        // MyBatis-Plus 的 Page 里原本装的是 User，这里转成 AdminUserResponse，保证 password 不会出现在响应里。
        Page<AdminUserResponse> responsePage = new Page<>(
                pageResult.getCurrent(), // 当前数据
                pageResult.getSize(), // 总条数
                pageResult.getTotal()); // 总页数
        responsePage.setRecords(pageResult.getRecords().stream()
                .map(AdminUserResponse::from)
                .toList());
        return responsePage;
    }

    // 管理员禁用用户：禁止禁自己 → 把目标用户 status 改成 0
    public UserStatusUpdateResult disableUser(Long currentAdminId, Long targetUserId) {
        if (currentAdminId.equals(targetUserId)) {
            return UserStatusUpdateResult.CANNOT_DISABLE_SELF; // ← 不能禁自己
        }
        return updateUserStatus(targetUserId, STATUS_DISABLED); // ← 把 status 改成 0
    }

    // 管理员启用用户：把目标用户 status 改回 1
    public UserStatusUpdateResult enableUser(Long targetUserId) {
        return updateUserStatus(targetUserId, STATUS_ENABLED); // ← 把 status 改成 1
    }

    // 通用修改 status：查用户 → 只改 status（不允许改 role）→ 更新入库
    private UserStatusUpdateResult updateUserStatus(Long targetUserId, Integer status) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            return UserStatusUpdateResult.USER_NOT_FOUND;
        }
        // 只改 status，不允许通过这个接口顺手改 role，避免普通用户被错误提升为管理员。
        user.setStatus(status);
        userMapper.updateById(user);
        return UserStatusUpdateResult.SUCCESS;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private Long countPublishedArticles(Long userId) {
        return articleMapper.selectCount(new QueryWrapper<Article>()
                .eq("user_id", userId)
                .eq("status", ArticleService.STATUS_PUBLISHED));
    }

    private Long countFavorites(Long userId) {
        return articleFavoriteMapper.selectCount(new QueryWrapper<ArticleFavorite>()
                .eq("user_id", userId));
    }

    private Long countFollowing(Long userId) {
        return userFollowMapper.selectCount(new QueryWrapper<UserFollow>()
                .eq("follower_id", userId));
    }

    private Long countFollowers(Long userId) {
        return userFollowMapper.selectCount(new QueryWrapper<UserFollow>()
                .eq("following_id", userId));
    }

    private boolean isFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null || followerId.equals(followingId)) {
            return false;
        }
        Long count = userFollowMapper.selectCount(new QueryWrapper<UserFollow>()
                .eq("follower_id", followerId)
                .eq("following_id", followingId));
        return count > 0;
    }

    public enum UserStatusUpdateResult {
        SUCCESS,            // 操作成功
        USER_NOT_FOUND,     // 目标用户不存在
        CANNOT_DISABLE_SELF // 管理员不能禁自己
    }
}
