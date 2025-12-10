package com.academic.user.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfig {
    // 已移除手动创建的 SqlSessionFactoryBean 和 MapperScannerConfigurer。
    // MyBatis-Plus 的 starter 会自动配置 SqlSessionFactory 和 Mapper 扫描。
    // 之前的实现使用了错误的 package(eg. org.weicengbie.*) 导致 Mapper/SqlSessionFactory 未正确注册。
}
