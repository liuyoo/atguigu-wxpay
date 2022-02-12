package com.example.demo.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@MapperScan("com.example.demo.mapper")
@EnableTransactionManagement//启用事务管理
public class MyBatisPlusConfig {

}
