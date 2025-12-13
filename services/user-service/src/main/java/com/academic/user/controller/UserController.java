package com.academic.user.controller;

import java.util.HashMap;
import java.util.Map;

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

    Map<String, Object> data = new HashMap<>();

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //注册
    @PostMapping("/normal/register/{validateId}")
    @ResponseBody
    public String registerNormal(@PathVariable(name="validateId") String validateId,
                                @RequestBody User requestUser) {
        //生成User
        try {

            requestUser.setPasswordHash(Secure.sha256(requestUser.getPasswordHash()));
            String userId = userService.registerNormal(requestUser);
            data.put("userId", userId);
            return ApiResponse.success(
                    "注册成功", JSON.toJSONString(data)
            );
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
            String token = JwtUtil.generateToken(user.getUserId());

            data.put("token", token);
            data.put("expiresIn", JwtUtil.expirationTime);
            data.put("user", user);
            return ApiResponse.success("登录成功", JSON.toJSONString(data));
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch (Exception e) {
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }
    }

    //获取当前用户信息
    @GetMapping("/current")
    @ResponseBody
    public String getCurrent(@RequestHeader(name = "Authorization") String token) {
        try {
            String userId = JwtUtil.analyseToken(token);
            User user = userService.getCurrent(userId);
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
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }

    }

    //修改当前用户信息
    @PutMapping("/current")
    @ResponseBody
    public String updateCurrent(@RequestHeader(name = "Authorization") String token,
            @RequestBody User user) {
        try {
            String userId = JwtUtil.analyseToken(token);
            user.setUserId(userId);
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
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }

    //发送验证码
    @PostMapping("/verification/send")
    @ResponseBody
    public String registerValidation(@RequestBody Map<String, String> requestBody) {
        try {
            String userId = userService.generateVerificationCode(null, requestBody.get("mail"));
            return ApiResponse.success("验证码已发送，请检查邮箱", userId);
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch (Exception e) {
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }
    }

    //发送验证码
    @PostMapping("/verification/send/{userId}")
    @ResponseBody
    public String resetPassValidation(@PathVariable("userId") String userId) {
        try {
            userId = userService.generateVerificationCode(userId, null);
            return ApiResponse.success("验证码已发送，请检查邮箱", userId);
        } catch (ServiceError e) {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        } catch (Exception e) {
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }
    }

//    //确认验证码
//    @PostMapping("/verification/validate/{userId}")
//    @ResponseBody
//    public String validateCode(@PathVariable("userId") String userId, @RequestBody Map<String, String> requestBody) {
//        boolean ok = userService.validateVerificationCode(userId, requestBody.get("code"));
//        if (ok) {
//            return ApiResponse.success("确认成功", null);
//        }
//        return ApiResponse.fail("验证码无效");
//    }

    //关注用户
    @PostMapping("/follow/{userId}")
    @ResponseBody
    public String follow(@RequestHeader(name = "Authorization") String token,
            @PathVariable("userId") String targetId) {
        try {
            String userId = JwtUtil.analyseToken(token);
            userService.follow(targetId, userId);
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
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }

    //取消关注
    @DeleteMapping("/follow/{userId}")
    @ResponseBody
    public String unfollow(@RequestHeader(name = "Authorization") String token,
            @PathVariable("userId") String targetId) {
        try {
            String userId = JwtUtil.analyseToken(token);
            userService.follow(targetId, userId);
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
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }

    //查看关注用户
    @GetMapping("/follows")
    @ResponseBody
    public String getFollows(@RequestHeader(name = "Authorization") String token,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        try {
            String userId = JwtUtil.analyseToken(token);
            IPage<User> userPage = userService.getFollows(userId, pageNum, pageSize);
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
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }

    //查看粉丝
    @GetMapping("/fans")
    @ResponseBody
    public String getFans(@RequestHeader(name = "Authorization") String token,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        try {
            String userId = JwtUtil.analyseToken(token);
            IPage<User> userPage = userService.getFans(userId, pageNum, pageSize);
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
            return ApiResponse.fail(-1, "服务器繁忙");
        }
    }
}
