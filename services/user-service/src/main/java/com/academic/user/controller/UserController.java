package com.academic.user.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.academic.user.common.ApiResponse;
import com.academic.user.common.JwtUtil;
import com.academic.user.common.Secure;
import com.academic.user.common.ServiceError;
import com.academic.user.dto.User;
import com.academic.user.service.UserService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    Map<String, Object> data = new HashMap<>();

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    //注册
    @PostMapping("/normal/register/{validateId}")
    @ResponseBody
    public String registerNormal(@RequestBody Map<String, Object> requestMap, @PathVariable("validateId") String validateId) {
        //生成User
        try {
            String verificationCode = (String) requestMap.get("verificationCode");
            User requestUser = JSON.parseObject(JSON.toJSONString(requestMap), User.class);
            userService.validateVerificationCode(validateId, verificationCode);
            requestUser.setPasswordHash(Secure.sha256(requestUser.getPasswordHash()));
            String userId = userService.registerNormal(requestUser);
            data.put("userId", userId);
            return ApiResponse.success(
                    "注册成功", JSON.toJSONString(data));
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }
    }

    //登录
    @PostMapping("/login")
    @ResponseBody
    public String login(@RequestBody User requestUser) {
        try {
            requestUser.setPasswordHash(Secure.sha256(requestUser.getPasswordHash()));
            User user = userService.login(requestUser);
            String token = jwtUtil.generateToken(user.getUserId(), user.getRole().toString());

            data.put("token", token);
            data.put("expiresIn", jwtUtil.getExpirationTime());
            data.put("user", user);
            return ApiResponse.success("登录成功", JSON.toJSONString(data));
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }
    }

    //获取当前用户信息
    @GetMapping("/current")
    @ResponseBody
    public String getCurrent(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        try {
            User user = userService.getCurrent(userIdHeader);
            return ApiResponse.success("获取成功", JSON.toJSONString(user));
        } catch (ExpiredJwtException e) {
            return ApiResponse.fail(-1, "登陆状态已过期");
        } catch (MalformedJwtException e) {
            return ApiResponse.fail(-1, "Token格式错误");
        } catch (UnsupportedJwtException e) {
            return ApiResponse.fail(-1, "Token不被支持");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(-1, "Token为空或无效");
        } catch (JwtException e) {
            return ApiResponse.fail(-1, "Token无效,请重新登录");
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙");
        }

    }

    //获取特定用户信息
    @GetMapping("/{userId}")
    @ResponseBody
    public String getById(@PathVariable("userId") String userId) {
        try {
            User user = userService.getById(userId);
            return ApiResponse.success("获取成功", JSON.toJSONString(user));
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }

    }

    //修改当前用户信息
    @PutMapping("/current")
    @ResponseBody
    public String updateCurrent(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestBody User user) {
        try {
            user.setUserId(userIdHeader);
            userService.updateCurrent(user);
            return ApiResponse.success("修改成功", null);
        } catch (ExpiredJwtException e) {
            return ApiResponse.fail(-1, "登陆状态已过期");
        } catch (MalformedJwtException e) {
            return ApiResponse.fail(-1, "Token格式错误");
        } catch (UnsupportedJwtException e) {
            return ApiResponse.fail(-1, "Token不被支持");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(-1, "Token为空或无效");
        } catch (JwtException e) {
            return ApiResponse.fail(-1, "Token无效,请重新登录");
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }

    //发送验证码
    @PostMapping("/verification/send")
    @ResponseBody
    public String registerValidation(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
                                     @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                String validateId = userService.generateVerificationCode(userIdHeader, null);
                data.put("validateId", validateId);
                return ApiResponse.success("验证码已发送，请检查邮箱", JSON.toJSONString(data));
            }
            if (requestBody.get("mail") == null || requestBody.get("mail").isEmpty()) {
                return ApiResponse.fail(-1, "邮箱不能为空");
            }
            String validateId = userService.generateVerificationCode(null, requestBody.get("mail"));
            return ApiResponse.success("验证码已发送，请检查邮箱", validateId);
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch(MailException e)
        {
            return ApiResponse.fail(0, e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }
    }

    //重置密码验证验证码
    @PostMapping("/password/reset/{validateId}")
    @ResponseBody
    public String resetPassword(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @PathVariable("validateId") String validateId, @RequestBody Map<String, String> requestBody) {
        try {
            userService.validateVerificationCode(validateId, requestBody.get("code"));
            userService.resetPassword(userIdHeader, requestBody.get("password"));
            return ApiResponse.success("修改成功", null);
        } catch (ExpiredJwtException e) {
            return ApiResponse.fail(-1, "登陆状态已过期");
        } catch (MalformedJwtException e) {
            return ApiResponse.fail(-1, "Token格式错误");
        } catch (UnsupportedJwtException e) {
            return ApiResponse.fail(-1, "Token不被支持");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(-1, "Token为空或无效");
        } catch (JwtException e) {
            return ApiResponse.fail(-1, "Token无效,请重新登录");
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }
    //关注用户
    @PostMapping("/follow/{userId}")
    @ResponseBody
    public String follow(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @PathVariable("userId") String targetId) {
        try {
            userService.follow(targetId, userIdHeader);
            return ApiResponse.success("关注成功", null);
        } catch (ExpiredJwtException e) {
            return ApiResponse.fail(-1, "登陆状态已过期");
        } catch (MalformedJwtException e) {
            return ApiResponse.fail(-1, "Token格式错误");
        } catch (UnsupportedJwtException e) {
            return ApiResponse.fail(-1, "Token不被支持");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(-1, "Token为空或无效");
        } catch (JwtException e) {
            return ApiResponse.fail(-1, "Token无效,请重新登录");
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }

    //取消关注
    @DeleteMapping("/follow/{userId}")
    @ResponseBody
    public String unfollow(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @PathVariable("userId") String targetId) {
        try {
            userService.unfollow(targetId, userIdHeader);
            return ApiResponse.success("取消成功", null);
        } catch (ExpiredJwtException e) {
            return ApiResponse.fail(-1, "登陆状态已过期");
        } catch (MalformedJwtException e) {
            return ApiResponse.fail(-1, "Token格式错误");
        } catch (UnsupportedJwtException e) {
            return ApiResponse.fail(-1, "Token不被支持");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(-1, "Token为空或无效");
        } catch (JwtException e) {
            return ApiResponse.fail(-1, "Token无效,请重新登录");
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }

    //查看关注用户
    @GetMapping("/follows")
    @ResponseBody
    public String getFollows(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        try {
            IPage<User> userPage = userService.getFollows(userIdHeader, pageNum, pageSize);
            return ApiResponse.success("获取成功", JSON.toJSONString(userPage));
        } catch (ExpiredJwtException e) {
            return ApiResponse.fail(-1, "登陆状态已过期");
        } catch (MalformedJwtException e) {
            return ApiResponse.fail(-1, "Token格式错误");
        } catch (UnsupportedJwtException e) {
            return ApiResponse.fail(-1, "Token不被支持");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(-1, "Token为空或无效");
        } catch (JwtException e) {
            return ApiResponse.fail(-1, "Token无效,请重新登录");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }

    //查看粉丝
    @GetMapping("/fans")
    @ResponseBody
    public String getFans(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        try {
            IPage<User> userPage = userService.getFans(userIdHeader, pageNum, pageSize);
            return ApiResponse.success("获取成功", JSON.toJSONString(userPage));
        } catch (ExpiredJwtException e) {
            return ApiResponse.fail(-1, "登陆状态已过期");
        } catch (MalformedJwtException e) {
            return ApiResponse.fail(-1, "Token格式错误");
        } catch (UnsupportedJwtException e) {
            return ApiResponse.fail(-1, "Token不被支持");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(-1, "Token为空或无效");
        } catch (JwtException e) {
            return ApiResponse.fail(-1, "Token无效,请重新登录");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }

    //查看用户
    @GetMapping("")
    @ResponseBody
    public String getUsers(
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        try {
            IPage<User> userPage = userService.getUsers(pageNum, pageSize);
            return ApiResponse.success("获取成功", JSON.toJSONString(userPage));
        } catch (ExpiredJwtException e) {
            return ApiResponse.fail(-1, "登陆状态已过期");
        } catch (MalformedJwtException e) {
            return ApiResponse.fail(-1, "Token格式错误");
        } catch (UnsupportedJwtException e) {
            return ApiResponse.fail(-1, "Token不被支持");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(-1, "Token为空或无效");
        } catch (JwtException e) {
            return ApiResponse.fail(-1, "Token无效,请重新登录");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }
}
