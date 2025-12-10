package com.academic.user.controller;

import com.academic.user.common.*;
import com.academic.user.dto.User;
import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


// ...existing code...
import com.academic.user.service.UserService;

import java.security.SignatureException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {


    private final UserService userService;

    Map<String, Object> data = new HashMap<>();
    public UserController(UserService userService) {
        this.userService = userService;
    }

    //注册
    @PostMapping("/normal/register")
    @ResponseBody
    public String registerNormal(@RequestBody User requestUser) {
        //生成User
        try
        {

            requestUser.setPasswordHash(Secure.sha256(requestUser.getPasswordHash()));
            String userId = userService.registerNormal(requestUser);
            data.put("userId", userId);
            return ApiResponse.success(
                    "注册成功", JSON.toJSONString(data)
            );
        }
        catch (ServiceError e)
        {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }
    }

    //登录
    @PostMapping("/login")
    @ResponseBody
    public String login(@RequestBody User requestUser) {
        try
        {
            requestUser.setPasswordHash(Secure.sha256(requestUser.getPasswordHash()));
            User user = userService.login(requestUser);
            String token = JwtUtil.generateToken(user.getUserId());

            data.put("token", token);
            data.put("expiresIn", JwtUtil.expirationTime);
            data.put("user", user);
            return ApiResponse.success("登录成功", JSON.toJSONString(data));
        }
        catch(ServiceError e)
        {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        }
        catch(Exception e)
        {
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }
    }

    //获取当前用户信息
    @GetMapping("/current")
    @ResponseBody
    public String getCurrent(@RequestHeader(name = "Authorization") String token) {
        try
        {
            String userId = JwtUtil.analyseToken(token);
            User user = userService.getCurrent(userId);
            return ApiResponse.success("获取成功",JSON.toJSONString(user));
        }
        catch(ExpiredJwtException e)
        {
            return ApiResponse.fail(-1, "登陆状态已过期");
        } catch (MalformedJwtException e) {
            return ApiResponse.fail(-1, "Token格式错误");
        } catch (UnsupportedJwtException e) {
            return ApiResponse.fail(-1, "Token不被支持");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(-1, "Token为空或无效");
        }
        catch(JwtException e)
        {
            return ApiResponse.fail(-1, "Token无效,请重新登录");
        }
        catch(ServiceError e)
        {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        }
        catch(Exception e)
        {
            return ApiResponse.fail(-1, "服务器繁忙");
        }


    }

    //获取特定用户信息
    @GetMapping("/{userId}")
    @ResponseBody
    public String getById(@PathVariable("userId") String userId) {
        try
        {
            User user = userService.getById(userId);
            return ApiResponse.success("获取成功",JSON.toJSONString(user));
        }
        catch(ServiceError e)
        {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        }
        catch(Exception e)
        {
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }

    }

    //修改当前用户信息
    @PutMapping("/current")
    @ResponseBody
    public String updateCurrent(@RequestHeader(name = "Authorization") String token,
                                @RequestBody User user) {
        try
        {
            String userId = JwtUtil.analyseToken(token);
            user.setUserId(userId);
            userService.updateCurrent(user);
            return ApiResponse.success("修改成功", null);
        }
        catch(ServiceError e)
        {
            return ApiResponse.fail(e.getCode(), e.getMsg());
        }
        catch(Exception e)
        {
            return ApiResponse.fail(-1, "服务器繁忙，请稍后再试");
        }
    }

    //修改用户密码
    @PostMapping("/password/reset")
    @ResponseBody
    public String passwordReset(@RequestBody RequestBody req) {
        userService.resetPassword(req);
        return ApiResponse.success("修改成功",null);
    }

    //确认验证码
    @GetMapping("/password/validation")
    @ResponseBody
    public String passwordValidation(@RequestParam("userId") String userId,
                                                             @RequestParam("code") String code) {
        boolean ok = userService.validateResetCode(userId, code);
        if (ok) return ApiResponse.success("确认成功",null);
        return ApiResponse.fail("验证码无效");
    }

    //关注用户
    @PostMapping("/follow/{scholarId}")
    @ResponseBody
    public String follow(@PathVariable("scholarId") String scholarId) {
        userService.follow(scholarId);
        return ApiResponse.success("关注成功",null);
    }

    //取消关注
    @DeleteMapping("/follow/{scholarId}")
    @ResponseBody
    public String unfollow(@PathVariable("scholarId") String scholarId) {
        userService.unfollow(scholarId);
        return ApiResponse.success("取消成功",null);
    }

    //查看关注用户
    @GetMapping("/follows")
    @ResponseBody
    public String getFollows(@RequestParam(value = "pageNum", required = false, defaultValue = "1") int pageNum,
                                                     @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        List<User> userList = userService.getFollows(pageNum, pageSize);

        return ApiResponse.success("获取成功",JSON.toJSONString(userList));
    }

    //查看粉丝
    @GetMapping("/fans")
    @ResponseBody
    public String getFans(@RequestParam(value = "pageNum", required = false, defaultValue = "1") int pageNum,
                                                  @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        List<User> userList = userService.getFans(pageNum, pageSize);
        return ApiResponse.success("获取成功",JSON.toJSONString(userList));
    }
}