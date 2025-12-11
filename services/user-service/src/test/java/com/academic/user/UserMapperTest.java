package com.academic.user;

import com.academic.user.common.Secure;
import com.academic.user.common.ServiceError;
import com.academic.user.dto.User;
import com.academic.user.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
public class UserMapperTest {

    @Autowired
    private UserService userService;
    @Test
    public void testAdd() throws Exception
    {
        User user = new User("T6","1@1.cn", Secure.sha256("123"));
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
    @Test
    public void testAddFollow() throws Exception
    {
        userService.follow("a28a8628-6d98-416b-a5b0-9ba7e8c28215",
                "68a9e369-5fbf-40d1-b14b-b01ddf2893ef");
    }
    @Test
    public void testDeleteFollow() throws Exception
    {
        userService.unfollow("53384ce1-2887-4f89-82c4-5082b5e1a20f",
                "68a9e369-5fbf-40d1-b14b-b01ddf2893ef");
    }
    @Test
    public void testSelectPage()
    {
        IPage<User> userList = userService.getFollows("68a9e369-5fbf-40d1-b14b-b01ddf2893ef",
                1, 2);
        System.out.println(userList);
    }
    @Test
    public void testSelectOneByUserId() throws ServiceError
    {
        System.out.println(userService.getById("68a9e369-5fbf-40d1-b14b-b01ddf2893ef"));
    }
}

