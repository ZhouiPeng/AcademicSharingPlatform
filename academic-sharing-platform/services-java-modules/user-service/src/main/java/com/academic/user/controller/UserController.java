package com.academic.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academic.user.dto.UserDto;
import com.academic.user.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {


    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/normal/register")
    public ResponseEntity<String> registerNormal(@RequestBody UserDto dto) {
        userService.registerNormal(dto);
        return ResponseEntity.status(201).body("registered");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserDto dto) {
        userService.login(dto);
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        userService.logout();
        return ResponseEntity.ok("logged out");
    }
}
