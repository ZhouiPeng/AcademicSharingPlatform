package com.academic.user;

import com.academic.user.dto.User;
import com.academic.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class UserMapperTest {

    @Autowired
    private UserService userService;
    @Test
    public void testAdd() throws Exception
    {
        User user = new User("Tom","1@1.cn","123");
        String userId = userService.registerNormal(user);
        System.out.println(userId);
    }
    @Test
    public void testUpdate() throws Exception
    {
        User user = new User();
        user.setUserId("53384ce1-2887-4f89-82c4-5082b5e1a20f");
        user.setDisplayName("Tim");
        user.setAvatarUrl("http://1.cn");
        user.setEmail("321@321.com");
        userService.updateCurrent(user);
    }
}

