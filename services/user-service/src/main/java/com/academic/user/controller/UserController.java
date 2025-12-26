package com.academic.user.controller;

import java.util.HashMap;
import java.util.Map;
import com.academic.user.common.*;
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

    Map<String, Object> response = new HashMap<>();

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
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
            String token = jwtUtil.generateToken(user.getUserId(), user.getRole().toString());

            response.put("token", token);
            response.put("expiresIn", jwtUtil.getExpirationTime());
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
    public ResponseEntity<ApiResponse> getCurrent(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        try {
            User user = userService.getCurrent(userIdHeader);
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
    public ResponseEntity<ApiResponse> getById(@PathVariable String userId) {
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
    public ResponseEntity<ApiResponse> updateCurrent(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestBody User user) {
        try {
            user.setUserId(userIdHeader);
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
    public ResponseEntity<ApiResponse> registerValidation(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                String validateId = userService.generateVerificationCode(userIdHeader, null);
                response.put("validateId", validateId);
                return ResponseEntity.ok().body(
                        ApiResponse.success("验证码已发送，请检查邮箱", JSON.toJSONString(response)));
            }
            else if(requestBody == null) {
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
        } catch(MailException e)
        {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ResultCode.SERVICE_NOT_COMPLETTE, e.getMessage()));
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //重置密码验证验证码
    @PostMapping("/password/reset/{validateId}")
    @ResponseBody
    public ResponseEntity<ApiResponse> resetPassword(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @PathVariable String validateId, @RequestBody Map<String, String> requestBody) {
        try {
            userService.validateVerificationCode(validateId, requestBody.get("code"));
            userService.resetPassword(userIdHeader, requestBody.get("password"));
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
    public ResponseEntity<ApiResponse> follow(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @PathVariable("userId") String targetId) {
        try {
            userService.follow(targetId, userIdHeader);
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
    public ResponseEntity<ApiResponse> unfollow(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @PathVariable("userId") String targetId) {
        try {
            userService.unfollow(targetId, userIdHeader);
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
    public ResponseEntity<ApiResponse> getFollows(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(required = false, defaultValue = "1") int pageNum,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        try {
            IPage<User> userPage = userService.getFollows(userIdHeader, pageNum, pageSize);
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
    public ResponseEntity<ApiResponse> getFans(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(required = false, defaultValue = "1") int pageNum,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        try {
            IPage<User> userPage = userService.getFans(userIdHeader, pageNum, pageSize);
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
            @RequestParam(required = false, defaultValue = "1") int pageNum,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
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
    public ResponseEntity<ApiResponse> getUsersByRole(@PathVariable String role,
            @RequestParam(required = false, defaultValue = "1") int pageNum,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
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

    @GetMapping("/users/all")
    @ResponseBody
    public ResponseEntity<ApiResponse> getAllUsers() {
        try {
            int num = userService.getUsersNum();
            response.put("userNum", num);
            return ResponseEntity.ok().body(
                    ApiResponse.success("获取成功", JSON.toJSONString(response)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    ApiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }
}
