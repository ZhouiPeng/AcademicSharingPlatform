package com.example.user.controller;

import com.example.user.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Value("${spring.application.name:users}")
    private String appName;

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable String userId) {
        UserDto u = new UserDto();
        u.setUserId(userId);
        u.setDisplayName("User " + userId);
        u.setInstitution("示例机构");
        return ResponseEntity.ok(u);
    }

    @GetMapping("/current")
    public ResponseEntity<UserDto> getCurrent() {
        UserDto u = new UserDto();
        u.setUserId("current-123");
        u.setDisplayName("当前用户");
        return ResponseEntity.ok(u);
    }
}
