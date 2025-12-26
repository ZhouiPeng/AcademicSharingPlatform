package com.academic.user.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
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
import com.academic.user.common.ResultCode;
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

    Map<String, Object> response = new HashMap<>();

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //注册
    @PostMapping("/normal/register/{validateId}")
    @ResponseBody
    public ResponseEntity<ApiResponse> registerNormal(@RequestBody Map<String, Object> requestMap, @PathVariable("validateId") String validateId) {
        //生成User
        try {
            String verificationCode = (String) requestMap.get("verificationCode");
            User requestUser = JSON.parseObject(JSON.toJSONString(requestMap), User.class);
            userService.validateVerificationCode(validateId, verificationCode);
            requestUser.setPasswordHash(Secure.sha256(requestUser.getPasswordHash()));
            String userId = userService.registerNormal(requestUser);
            response.put("userId", userId);
            return ResponseEntity.ok().body(
                    ApiResponse.success("注册成功", JSON.toJSONString(response)));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //登录
    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<ApiResponse> login(@RequestBody User requestUser) {
        try {
            requestUser.setPasswordHash(Secure.sha256(requestUser.getPasswordHash()));
            User user = userService.login(requestUser);
            String token = JwtUtil.generateToken(user.getUserId());

            response.put("token", token);
            response.put("expiresIn", JwtUtil.expirationTime);
            response.put("user", user);
            return ResponseEntity.ok().body(
                    ApiResponse.success("登录成功", JSON.toJSONString(response)));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //获取当前用户信息
    @GetMapping("/current")
    @ResponseBody
    public ResponseEntity<ApiResponse> getCurrent(@RequestHeader(name = "Authorization") String token) {
        try {
            String userId = JwtUtil.analyseToken(token);
            User user = userService.getCurrent(userId);
            return ResponseEntity.ok().body(
                    ApiResponse.success("获取成功", JSON.toJSONString(user)));
        } catch (ExpiredJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_EXPIRED, "登陆状态已过期"));
        } catch (MalformedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.WRONG_FORMAT_TOKEN, "Token格式错误"));
        } catch (UnsupportedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NOT_SUPPORT, "Token不被支持"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NULL, "Token为空或无效"));
        } catch (JwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_INVALID, "Token无效,请重新登录"));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }

    }

    //获取特定用户信息
    @GetMapping("/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse> getById(@PathVariable("userId") String userId) {
        try {
            User user = userService.getById(userId);
            return ResponseEntity.ok().body(
                    ApiResponse.success("获取成功", JSON.toJSONString(user)));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //修改当前用户信息
    @PutMapping("/current")
    @ResponseBody
    public ResponseEntity<ApiResponse> updateCurrent(@RequestHeader(name = "Authorization") String token,
            @RequestBody User user) {
        try {
            String userId = JwtUtil.analyseToken(token);
            user.setUserId(userId);
            userService.updateCurrent(user);
            return ResponseEntity.ok().body(
                    ApiResponse.success("修改成功", null));
        } catch (ExpiredJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_EXPIRED, "登陆状态已过期"));
        } catch (MalformedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.WRONG_FORMAT_TOKEN, "Token格式错误"));
        } catch (UnsupportedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NOT_SUPPORT, "Token不被支持"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NULL, "Token为空或无效"));
        } catch (JwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_INVALID, "Token无效,请重新登录"));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //发送验证码
    @PostMapping("/verification/send")
    @ResponseBody
    public ResponseEntity<ApiResponse> registerValidation(@RequestHeader(name = "Authorization", required = false) String token,
            @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            if (token != null && !token.isEmpty()) {
                String userId = JwtUtil.analyseToken(token);
                String validateId = userService.generateVerificationCode(userId, null);
                response.put("validateId", validateId);
                return ResponseEntity.ok().body(
                        ApiResponse.success("验证码已发送，请检查邮箱", JSON.toJSONString(response)));
            } else if (requestBody == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.fail(ResultCode.SERVICE_NOT_COMPLETTE, "请求体不能为空"));
            }
            if (requestBody.get("email") == null || requestBody.get("email").isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.fail(ResultCode.SERVICE_NOT_COMPLETTE, "邮箱不能为空"));
            }
            String validateId = userService.generateVerificationCode(null, requestBody.get("email"));
            response.put("validateId", validateId);
            return ResponseEntity.ok().body(
                    ApiResponse.success("验证码已发送，请检查邮箱", JSON.toJSONString(response)));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(e.getCode(), e.getMsg()));
        } catch (MailException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.SERVICE_NOT_COMPLETTE, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //重置密码验证验证码
    @PostMapping("/password/reset/{validateId}")
    @ResponseBody
    public ResponseEntity<ApiResponse> resetPassword(@RequestHeader(name = "Authorization") String token,
            @PathVariable("validateId") String validateId, @RequestBody Map<String, String> requestBody) {
        try {
            String userId = JwtUtil.analyseToken(token);
            userService.validateVerificationCode(validateId, requestBody.get("code"));
            userService.resetPassword(userId, requestBody.get("password"));
            return ResponseEntity.ok().body(
                    ApiResponse.success("修改成功", null));
        } catch (ExpiredJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_EXPIRED, "登陆状态已过期"));
        } catch (MalformedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.WRONG_FORMAT_TOKEN, "Token格式错误"));
        } catch (UnsupportedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NOT_SUPPORT, "Token不被支持"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NULL, "Token为空或无效"));
        } catch (JwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_INVALID, "Token无效,请重新登录"));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //关注用户
    @PostMapping("/follow/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse> follow(@RequestHeader(name = "Authorization") String token,
            @PathVariable("userId") String targetId) {
        try {
            String userId = JwtUtil.analyseToken(token);
            userService.follow(targetId, userId);
            return ResponseEntity.ok().body(
                    ApiResponse.success("关注成功", null));
        } catch (ExpiredJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_EXPIRED, "登陆状态已过期"));
        } catch (MalformedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.WRONG_FORMAT_TOKEN, "Token格式错误"));
        } catch (UnsupportedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NOT_SUPPORT, "Token不被支持"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NULL, "Token为空或无效"));
        } catch (JwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_INVALID, "Token无效,请重新登录"));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //取消关注
    @DeleteMapping("/follow/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse> unfollow(@RequestHeader(name = "Authorization") String token,
            @PathVariable("userId") String targetId) {
        try {
            String userId = JwtUtil.analyseToken(token);
            userService.follow(targetId, userId);
            return ResponseEntity.ok(ApiResponse.success("取消成功", null));
        } catch (ExpiredJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_EXPIRED, "登陆状态已过期"));
        } catch (MalformedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.WRONG_FORMAT_TOKEN, "Token格式错误"));
        } catch (UnsupportedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NOT_SUPPORT, "Token不被支持"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NULL, "Token为空或无效"));
        } catch (JwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_INVALID, "Token无效,请重新登录"));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //查看关注用户
    @GetMapping("/follows")
    @ResponseBody
    public ResponseEntity<ApiResponse> getFollows(@RequestHeader(name = "Authorization") String token,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        try {
            String userId = JwtUtil.analyseToken(token);
            IPage<User> userPage = userService.getFollows(userId, pageNum, pageSize);
            return ResponseEntity.ok().body(
                    ApiResponse.success("获取成功", JSON.toJSONString(userPage)));
        } catch (ExpiredJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_EXPIRED, "登陆状态已过期"));
        } catch (MalformedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.WRONG_FORMAT_TOKEN, "Token格式错误"));
        } catch (UnsupportedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NOT_SUPPORT, "Token不被支持"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NULL, "Token为空或无效"));
        } catch (JwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_INVALID, "Token无效,请重新登录"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //查看粉丝
    @GetMapping("/fans")
    @ResponseBody
    public ResponseEntity<ApiResponse> getFans(@RequestHeader(name = "Authorization") String token,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        try {
            String userId = JwtUtil.analyseToken(token);
            IPage<User> userPage = userService.getFans(userId, pageNum, pageSize);
            return ResponseEntity.ok().body(
                    ApiResponse.success("获取成功", JSON.toJSONString(userPage)));
        } catch (ExpiredJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_EXPIRED, "登陆状态已过期"));
        } catch (MalformedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.WRONG_FORMAT_TOKEN, "Token格式错误"));
        } catch (UnsupportedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NOT_SUPPORT, "Token不被支持"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NULL, "Token为空或无效"));
        } catch (JwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_INVALID, "Token无效,请重新登录"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //查看用户
    @GetMapping("")
    @ResponseBody
    public ResponseEntity<ApiResponse> getUsers(
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        try {
            IPage<User> userPage = userService.getUsers(pageNum, pageSize);
            return ResponseEntity.ok().body(
                    ApiResponse.success("获取成功", JSON.toJSONString(userPage)));
        } catch (ExpiredJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_EXPIRED, "登陆状态已过期"));
        } catch (MalformedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.WRONG_FORMAT_TOKEN, "Token格式错误"));
        } catch (UnsupportedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NOT_SUPPORT, "Token不被支持"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NULL, "Token为空或无效"));
        } catch (JwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_INVALID, "Token无效,请重新登录"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //根据Role查看用户列表
    @GetMapping("/role/{role}")
    @ResponseBody
    public ResponseEntity<ApiResponse> getUsersByRole(@PathVariable("role") String role,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        try {
            IPage<User> userPage = userService.getUsersByRole(pageNum, pageSize, role);
            return ResponseEntity.ok().body(
                    ApiResponse.success("获取成功", JSON.toJSONString(userPage)));
        } catch (ExpiredJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_EXPIRED, "登陆状态已过期"));
        } catch (MalformedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.WRONG_FORMAT_TOKEN, "Token格式错误"));
        } catch (UnsupportedJwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NOT_SUPPORT, "Token不被支持"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_NULL, "Token为空或无效"));
        } catch (JwtException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.TOKEN_IS_INVALID, "Token无效,请重新登录"));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }
}
