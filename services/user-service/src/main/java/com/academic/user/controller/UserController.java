package com.academic.user.controller;

import java.util.HashMap;
import java.util.Map;
import com.academic.user.common.*;
import com.academic.user.dto.request.RegisterRequestModel;
import com.academic.user.dto.response.LoginResponseModel;
import com.academic.user.dto.response.TotalResponseModel;
import com.academic.user.dto.response.VerificationResponseModel;
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
    public ResponseEntity<ApiResponse<User>> registerNormal(
            @RequestBody RegisterRequestModel registerRequestModel, 
            @PathVariable String validateId) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        //生成User
        try {
            String verificationCode = registerRequestModel.getVerificationCode();
            User requestUser = registerRequestModel.getUser();
            userService.validateVerificationCode(validateId, verificationCode);
            requestUser.setPasswordHash(Secure.sha256(requestUser.getPasswordHash()));
            String userId = userService.registerNormal(requestUser);
            requestUser.setUserId(userId);
            return ResponseEntity.ok().body(
                    apiResponse.success("注册成功", requestUser));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //登录
    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<ApiResponse<LoginResponseModel>> login(@RequestBody User requestUser) {
        ApiResponse<LoginResponseModel> apiResponse = new ApiResponse<>();
        try {
            requestUser.setPasswordHash(Secure.sha256(requestUser.getPasswordHash()));
            User user = userService.login(requestUser);
            String token = jwtUtil.generateToken(user.getUserId(), user.getRole().toString());
            LoginResponseModel loginResponseModel =
                    new LoginResponseModel(token, JwtUtil.getExpirationTime(), user);
            return ResponseEntity.ok().body(
                    apiResponse.success("登录成功", loginResponseModel));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //获取当前用户信息
    @GetMapping("/current")
    @ResponseBody
    public ResponseEntity<ApiResponse<User>> getCurrent(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        try {
            User user = userService.getCurrent(userIdHeader);
            return ResponseEntity.ok().body(
                    apiResponse.success("获取成功", user));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }

    }

    //获取特定用户信息
    @GetMapping("/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<User>> getById(@PathVariable String userId) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        try {
            User user = userService.getById(userId);
            return ResponseEntity.ok().body(
                    apiResponse.success("获取成功", user));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //修改当前用户信息
    @PutMapping("/current")
    @ResponseBody
    public ResponseEntity<ApiResponse<User>> updateCurrent(
            @RequestHeader(name = "X-User-Id") String userIdHeader,
            @RequestBody User user) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        try {
            user.setUserId(userIdHeader);
            userService.updateCurrent(user);
            return ResponseEntity.ok().body(
                    apiResponse.success("修改成功", null));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //发送验证码
    @PostMapping("/verification/send")
    @ResponseBody
    public ResponseEntity<ApiResponse<VerificationResponseModel>> registerValidation(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestBody(required = false) Map<String, String> requestBody) {
        ApiResponse<VerificationResponseModel> apiResponse = new ApiResponse<>();
        VerificationResponseModel verificationResponseModel = new VerificationResponseModel();
        try {
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                String validateId = userService.generateVerificationCode(userIdHeader, null);
                verificationResponseModel.setValidateId(validateId);
                return ResponseEntity.ok().body(
                        apiResponse.success("验证码已发送，请检查邮箱", verificationResponseModel));
            }
            else if(requestBody == null) {
                return ResponseEntity.badRequest().body(
                        apiResponse.fail(ResultCode.SERVICE_NOT_COMPLETTE, "请求体不能为空"));
            }
            if (requestBody.get("email") == null || requestBody.get("email").isEmpty()) {
                return ResponseEntity.badRequest().body(
                        apiResponse.fail(ResultCode.SERVICE_NOT_COMPLETTE, "邮箱不能为空"));
            }
            String validateId = userService.generateVerificationCode(null, requestBody.get("email"));
            verificationResponseModel.setValidateId(validateId);
            return ResponseEntity.ok().body(
                    apiResponse.success("验证码已发送，请检查邮箱", verificationResponseModel));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(e.getCode(), e.getMsg()));
        } catch(MailException e)
        {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(ResultCode.SERVICE_NOT_COMPLETTE, e.getMessage()));
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //重置密码验证验证码
    @PostMapping("/password/reset/{validateId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> resetPassword(
            @RequestHeader(name = "X-User-Id") String userIdHeader,
            @PathVariable String validateId, @RequestBody Map<String, String> requestBody) {
        ApiResponse<Object> apiResponse = new ApiResponse<>();
        try {
            userService.validateVerificationCode(validateId, requestBody.get("code"));
            userService.resetPassword(userIdHeader, requestBody.get("password"));
            return ResponseEntity.ok().body(
                    apiResponse.success("修改成功", null));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //关注用户
    @PostMapping("/follow/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> follow(
            @RequestHeader(name = "X-User-Id") String userIdHeader,
            @PathVariable("userId") String targetId) {
        ApiResponse<Object> apiResponse = new ApiResponse<>();
        try {
            userService.follow(targetId, userIdHeader);
            return ResponseEntity.ok().body(
                    apiResponse.success("关注成功", null));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //取消关注
    @DeleteMapping("/follow/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> unfollow(
            @RequestHeader(name = "X-User-Id") String userIdHeader,
            @PathVariable("userId") String targetId) {
        ApiResponse<Object> apiResponse = new ApiResponse<>();
        try {
            userService.unfollow(targetId, userIdHeader);
            return ResponseEntity.ok(apiResponse.success("取消成功", null));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //查看关注用户
    @GetMapping("/follows")
    @ResponseBody
    public ResponseEntity<ApiResponse<IPage<User>>> getFollows(
            @RequestHeader(name = "X-User-Id") String userIdHeader,
            @RequestParam(required = false, defaultValue = "1") int pageNum,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        ApiResponse<IPage<User>> apiResponse = new ApiResponse<>();
        try {
            IPage<User> userPage = userService.getFollows(userIdHeader, pageNum, pageSize);
            return ResponseEntity.ok().body(
                    apiResponse.success("获取成功", userPage));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //查看粉丝
    @GetMapping("/fans")
    @ResponseBody
    public ResponseEntity<ApiResponse<IPage<User>>> getFans(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(required = false, defaultValue = "1") int pageNum,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        ApiResponse<IPage<User>> apiResponse = new ApiResponse<>();
        try {
            IPage<User> userPage = userService.getFans(userIdHeader, pageNum, pageSize);
            return ResponseEntity.ok().body(
                    apiResponse.success("获取成功", userPage));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //查看用户
    @GetMapping("")
    @ResponseBody
    public ResponseEntity<ApiResponse<IPage<User>>> getUsers(
            @RequestParam(required = false, defaultValue = "1") int pageNum,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        ApiResponse<IPage<User>> apiResponse = new ApiResponse<>();
        try {
            IPage<User> userPage = userService.getUsers(pageNum, pageSize);
            return ResponseEntity.ok().body(
                    apiResponse.success("获取成功", userPage));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //根据Role查看用户列表
    @GetMapping("/role/{role}")
    @ResponseBody
    public ResponseEntity<ApiResponse<IPage<User>>> getUsersByRole(@PathVariable String role,
            @RequestParam(required = false, defaultValue = "1") int pageNum,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        ApiResponse<IPage<User>> apiResponse = new ApiResponse<>();
        try {
            IPage<User> userPage = userService.getUsersByRole(pageNum, pageSize, role);
            return ResponseEntity.ok().body(
                    apiResponse.success("获取成功", userPage));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    @GetMapping("/users/all")
    @ResponseBody
    public ResponseEntity<ApiResponse<TotalResponseModel>> getAllUsers() {
        ApiResponse<TotalResponseModel> apiResponse = new ApiResponse<>();
        try {
            int num = userService.getUsersNum();
            TotalResponseModel totalResponseModel = new TotalResponseModel(num);
            return ResponseEntity.ok().body(
                    apiResponse.success("获取成功", totalResponseModel));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }
}
