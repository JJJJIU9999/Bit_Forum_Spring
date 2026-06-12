package com.bitforum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.dto.AdminUserResponse;
import com.bitforum.entity.User;
import com.bitforum.mapper.UserMapper;
import com.bitforum.service.UserService.UserStatusUpdateResult;

// @ExtendWith(MockitoExtension.class) → 告诉 JUnit 这次测试要用 Mockito 框架
// @Mock       → 造一个假 UserMapper（不连数据库，纯内存假货）
// @InjectMocks → 把假 UserMapper 自动塞进真 UserService 里（替代 @Autowired）
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;    // 假的 Mapper，不连数据库

    @InjectMocks
    private UserService userService;  // 真的 Service，但里面的 Mapper 是假的

    // ==================== 测试1：注册时自动设默认 role 和 status ====================
    @Test
    void registerShouldSetDefaultRoleAndStatus() {
        // ---- Arrange：准备 ----
        // when(调某个方法).thenReturn(就返回这个值)
        // any(QueryWrapper.class) → "不管传的什么查询条件"
        // thenReturn(null)        → "都返回 null，假装没查到重名用户"
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        // ---- Act：执行 ----
        User registered = userService.register("newuser", "123456");

        // ---- Assert：验证 ----
        // ArgumentCaptor = 抓包器：抓住传给 insert() 的 User 参数，好检查它的字段
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture()); // 确认 insert() 真的被调了一次
        User inserted = userCaptor.getValue();            // 取出被抓的 User 对象

        // assertEquals(期望值, 实际值)    → 不相等就测试失败
        // assertNotEquals(不期望的值, 实际值) → 相等就测试失败
        assertEquals("USER", inserted.getRole());              // 期望 role 默认是 "USER"
        assertEquals(1, inserted.getStatus());                 // 期望 status 默认是 1（正常）
        assertNotEquals("123456", inserted.getPassword());     // 期望密码不是明文（已 BCrypt 加密）
        assertEquals("newuser", registered.getUsername());     // 期望返回的用户名正确
    }

    // ==================== 测试2：禁用用户不能登录 ====================
    @Test
    void loginShouldFailWhenUserIsDisabled() {
        // ---- Arrange：准备 ----
        // 造一个 status=0（禁用）的用户
        User user = new User();
        user.setUsername("disabled");
        user.setPassword(new BCryptPasswordEncoder().encode("123456")); // 加密后的密码
        user.setRole("USER");
        user.setStatus(0);  // ← 0 = 禁用

        // 假装数据库查到了这个用户
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        // ---- Act：执行 ----
        User result = userService.login("disabled", "123456");

        // ---- Assert：验证 ----
        // assertNull(值) → 值不是 null 就测试失败
        // 期望登录返回 null，因为 status=0 的用户不允许登录
        assertNull(result);
    }

    // ==================== 测试3：后台用户列表不暴露 password ====================
    @Test
    void pageUsersShouldReturnAdminUserResponseWithoutPassword() {
        User user = new User();
        user.setId(1L);
        user.setUsername("normal-user");
        user.setPassword("password-hash");
        user.setRole("USER");
        user.setStatus(1);

        Page<User> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(user));
        when(userMapper.selectPage(any(Page.class), eq(null))).thenReturn(page);

        Page<AdminUserResponse> result = userService.pageUsers(1, 10);

        assertEquals(1, result.getRecords().size());
        assertEquals("normal-user", result.getRecords().get(0).getUsername());
        assertEquals("USER", result.getRecords().get(0).getRole());
    }

    // ==================== 测试4：管理员不能禁用自己 ====================
    @Test
    void disableUserShouldRejectCurrentAdmin() {
        UserStatusUpdateResult result = userService.disableUser(1L, 1L);

        assertEquals(UserStatusUpdateResult.CANNOT_DISABLE_SELF, result);
    }

    // ==================== 测试5：管理员禁用用户只修改 status ====================
    @Test
    void disableUserShouldSetStatusToDisabled() {
        User user = new User();
        user.setId(2L);
        user.setRole("USER");
        user.setStatus(UserService.STATUS_ENABLED);
        when(userMapper.selectById(2L)).thenReturn(user);

        UserStatusUpdateResult result = userService.disableUser(1L, 2L);

        assertEquals(UserStatusUpdateResult.SUCCESS, result);
        assertEquals(UserService.STATUS_DISABLED, user.getStatus());
        verify(userMapper).updateById(user);
    }

    // ==================== 测试6：管理员启用用户只修改 status ====================
    @Test
    void enableUserShouldSetStatusToEnabled() {
        User user = new User();
        user.setId(2L);
        user.setRole("USER");
        user.setStatus(UserService.STATUS_DISABLED);
        when(userMapper.selectById(2L)).thenReturn(user);

        UserStatusUpdateResult result = userService.enableUser(2L);

        assertEquals(UserStatusUpdateResult.SUCCESS, result);
        assertEquals(UserService.STATUS_ENABLED, user.getStatus());
        verify(userMapper).updateById(user);
    }
}
