package com.busagent.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.busagent.common.BusinessException;
import com.busagent.common.ErrorCode;
import com.busagent.dto.AdminUserView;
import com.busagent.dto.LoginRequest;
import com.busagent.dto.LoginResponse;
import com.busagent.entity.AdminUser;
import com.busagent.mapper.AdminUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AdminAuthService {

    private static final String PASSWORD_PREFIX = "bus-agent";

    private final AdminUserMapper adminUserMapper;

    public AdminAuthService(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.getUsername()));
        if (user == null || !hashPassword(request.getUsername(), request.getPassword()).equals(user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被停用");
        }

        user.setLastLoginTime(LocalDateTime.now());
        adminUserMapper.updateById(user);
        StpUtil.login(user.getId());
        return new LoginResponse(StpUtil.getTokenName(), StpUtil.getTokenValue(), AdminUserView.from(user));
    }

    public AdminUserView currentAdmin() {
        long adminId = StpUtil.getLoginIdAsLong();
        AdminUser user = adminUserMapper.selectById(adminId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录账号不存在");
        }
        return AdminUserView.from(user);
    }

    public void logout() {
        StpUtil.logout();
    }

    private String hashPassword(String username, String plainPassword) {
        String raw = PASSWORD_PREFIX + ":" + username + ":" + plainPassword;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
