package com.academic.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.academic.user.common.*;
import com.academic.user.dto.request.*;
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

import com.academic.user.dto.service.User;
import com.academic.user.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    //注册
    @Operation(summary = "用户注册", description = "使用邮箱验证码注册普通用户")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"注册成功\",\"data\":{\"userId\":\"u_123\",\"username\":\"zhangsan\",\"email\":\"zhangsan@example.com\",\"displayName\":\"张三\"},\"timestamp\":1735286400000}")))
    })
    @PostMapping("/normal/register/{validateId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<User>> registerNormal(
            @RequestBody RegisterRequestModel registerRequestModel,
            @PathVariable String validateId) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        //生成User
        try {
            String verificationCode = registerRequestModel.getVerificationCode();
            User requestUser = new User(registerRequestModel.getUsername(),
                    registerRequestModel.getEmail(),
                    Secure.sha256(registerRequestModel.getPassword()),
                    registerRequestModel.getDisplayName());

            userService.validateVerificationCode(validateId, verificationCode);
            String userId = userService.registerNormal(requestUser);
            requestUser.setUserId(userId);
            return ResponseEntity.ok().body(
                    apiResponse.success("注册成功", requestUser));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            logger.error("注册失败", e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //登录
    @Operation(summary = "用户登录", description = "用户使用用户名和密码登录系统")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "登录成功",
                content = @Content(
                        mediaType = "application/json",
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                value = "{\"code\":1,\"msg\":\"登录成功\",\"data\":{\"token\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\",\"expiresIn\":\"3600\",\"user\":{\"userId\":\"123\",\"username\":\"zhangsan\"}},\"timestamp\":1735286400000}"
                        )
                )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "用户名或密码错误",
                content = @Content(
                        mediaType = "application/json",
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                value = "{\"code\":-201,\"msg\":\"用户名或密码错误\",\"data\":null,\"timestamp\":1735286400000}"
                        )
                )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "服务器内部错误",
                content = @Content(
                        mediaType = "application/json",
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                value = "{\"code\":-302,\"msg\":\"服务器繁忙，请稍后再试\",\"data\":null,\"timestamp\":1735286400000}"
                        )
                )
        )
    })
    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<ApiResponse<LoginResponseModel>> login(
            @RequestBody LoginRequestModel loginRequestModel) {
        ApiResponse<LoginResponseModel> apiResponse = new ApiResponse<>();
        try {
            User requestUser = new User(loginRequestModel.getUsername(),
                    Secure.sha256(loginRequestModel.getPassword()));
            User user = userService.login(requestUser);
            String token = jwtUtil.generateToken(user.getUserId(), user.getRole().toString());
            LoginResponseModel loginResponseModel
                    = new LoginResponseModel(token, JwtUtil.getExpirationTime(), user);
            return ResponseEntity.ok().body(
                    apiResponse.success("登录成功", loginResponseModel));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            logger.error("登录失败", e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //获取当前用户信息
    @Operation(summary = "获取当前用户信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"获取成功\",\"data\":{\"userId\":\"u_123\",\"username\":\"zhangsan\",\"email\":\"zhangsan@example.com\",\"displayName\":\"张三\"},\"timestamp\":1735286400000}")))
    })
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
            logger.error("获取当前用户信息失败", e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }

    }

    //获取特定用户信息
    @Operation(summary = "获取特定用户信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"获取成功\",\"data\":{\"userId\":\"u_456\",\"username\":\"lisi\"},\"timestamp\":1735286400000}")))
    })
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
            logger.error("获取用户信息失败, userId: {}", userId, e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //修改当前用户信息
    @Operation(summary = "修改当前用户信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "修改成功",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"修改成功\",\"data\":null,\"timestamp\":1735286400000}")))
    })
    @PutMapping("/current")
    @ResponseBody
    public ResponseEntity<ApiResponse<User>> updateCurrent(
            @RequestHeader(name = "X-User-Id") String userIdHeader,
            @RequestBody UpdateRequestModel updateRequestModel) {
        ApiResponse<User> apiResponse = new ApiResponse<>();
        try {
            User user = new User();
            user.setAvatarUrl(updateRequestModel.getAvatarFileId());
            user.setDisplayName(updateRequestModel.getDisplayName());
            user.setEmail(updateRequestModel.getEmail());
            user.setUserId(userIdHeader);
            userService.updateCurrent(user);
            return ResponseEntity.ok().body(
                    apiResponse.success("修改成功", null));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            logger.error("修改用户信息失败, userId: {}", userIdHeader, e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //发送验证码
    @Operation(summary = "发送验证码")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "验证码已发送",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"验证码已发送，请检查邮箱\",\"data\":{\"validateId\":\"v_abc123\"},\"timestamp\":1735286400000}")))
    })
    @PostMapping("/verification/send")
    @ResponseBody
    public ResponseEntity<ApiResponse<VerificationResponseModel>> registerValidation(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestBody(required = false) VerificationRequestModel verificationRequestModel) {
        ApiResponse<VerificationResponseModel> apiResponse = new ApiResponse<>();
        VerificationResponseModel verificationResponseModel = new VerificationResponseModel();
        try {
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                String validateId = userService.generateVerificationCode(userIdHeader, null);
                verificationResponseModel.setValidateId(validateId);
                return ResponseEntity.ok().body(
                        apiResponse.success("验证码已发送，请检查邮箱", verificationResponseModel));
            } else if (verificationRequestModel == null) {
                return ResponseEntity.badRequest().body(
                        apiResponse.fail(ResultCode.SERVICE_NOT_COMPLETTE, "请求体不能为空"));
            }
            if (verificationRequestModel.getEmail() == null
                    || verificationRequestModel.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        apiResponse.fail(ResultCode.SERVICE_NOT_COMPLETTE, "邮箱不能为空"));
            }
            String validateId = userService.generateVerificationCode(null,
                    verificationRequestModel.getEmail());
            verificationResponseModel.setValidateId(validateId);
            return ResponseEntity.ok().body(
                    apiResponse.success("验证码已发送，请检查邮箱", verificationResponseModel));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (MailException e) {
            logger.error("发送验证码邮件失败", e);
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(ResultCode.SERVICE_NOT_COMPLETTE, e.getMessage()));
        } catch (Exception e) {

            logger.error("发送验证码失败", e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙，请稍后再试"));
        }
    }

    //重置密码验证验证码
    @Operation(summary = "重置密码")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "修改成功",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"修改成功\",\"data\":null,\"timestamp\":1735286400000}")))
    })
    @PostMapping("/password/reset/{validateId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> resetPassword(
            @RequestHeader(name = "X-User-Id") String userIdHeader,
            @PathVariable String validateId, @RequestBody ResetRequestModel resetRequestModel) {
        ApiResponse<Object> apiResponse = new ApiResponse<>();
        try {
            userService.validateVerificationCode(validateId, resetRequestModel.getCode());
            userService.resetPassword(userIdHeader, Secure.sha256(resetRequestModel.getPassword()));
            return ResponseEntity.ok().body(
                    apiResponse.success("修改成功", null));
        } catch (ServiceError e) {
            return ResponseEntity.badRequest().body(
                    apiResponse.fail(e.getCode(), e.getMsg()));
        } catch (Exception e) {
            logger.error("重置密码失败, userId: {}", userIdHeader, e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //关注用户
    @Operation(summary = "关注用户")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "关注成功",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"关注成功\",\"data\":null,\"timestamp\":1735286400000}")))
    })
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
            logger.error("关注用户失败, userId: {}, targetId: {}", userIdHeader, targetId, e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //取消关注
    @Operation(summary = "取消关注")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取消成功",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"取消成功\",\"data\":null,\"timestamp\":1735286400000}")))
    })
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
            logger.error("取消关注失败, userId: {}, targetId: {}", userIdHeader, targetId, e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //查看关注用户
    @Operation(summary = "查看关注用户")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"获取成功\",\"data\":{\"records\":[{\"userId\":\"u_123\",\"username\":\"zhangsan\"}],\"current\":1,\"size\":10,\"total\":1},\"timestamp\":1735286400000}")))
    })
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
            logger.error("获取关注列表失败, userId: {}", userIdHeader, e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //查看粉丝
    @Operation(summary = "查看粉丝")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"获取成功\",\"data\":{\"records\":[{\"userId\":\"u_789\",\"username\":\"wangwu\"}],\"current\":1,\"size\":10,\"total\":1},\"timestamp\":1735286400000}")))
    })
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
            logger.error("获取粉丝列表失败, userId: {}", userIdHeader, e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //查看用户
    @Operation(summary = "查看用户列表")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"获取成功\",\"data\":{\"records\":[{\"userId\":\"u_101\",\"username\":\"demo\"}],\"current\":1,\"size\":10,\"total\":1},\"timestamp\":1735286400000}")))
    })
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
            logger.error("获取用户列表失败", e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    //根据Role查看用户列表
    @Operation(summary = "根据角色查看用户列表")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"code\":1,\"msg\":\"获取成功\",\"data\":{\"records\":[{\"userId\":\"u_201\",\"username\":\"roleUser\"}],\"current\":1,\"size\":10,\"total\":1},\"timestamp\":1735286400000}")))
    })
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
            logger.error("根据角色获取用户列表失败, role: {}", role, e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }

    @Operation(summary = "获取用户总数", description = "获取系统中所有用户的总数")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "获取成功",
                content = @Content(
                        mediaType = "application/json",
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                value = "{\"code\":1,\"msg\":\"获取成功\",\"data\":{\"total\":100},\"timestamp\":1735286400000}"
                        )
                )
        )
    })
    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<ApiResponse<TotalResponseModel>> getAllUsers() {
        ApiResponse<TotalResponseModel> apiResponse = new ApiResponse<>();
        try {
            int num = userService.getUsersNum();
            TotalResponseModel totalResponseModel = new TotalResponseModel(num);
            return ResponseEntity.ok().body(
                    apiResponse.success("获取成功", totalResponseModel));
        } catch (Exception e) {
            logger.error("获取用户总数失败", e);
            return ResponseEntity.internalServerError().body(
                    apiResponse.fail(ResultCode.UNKNOWN_ERROR, "服务器繁忙"));
        }
    }
}
