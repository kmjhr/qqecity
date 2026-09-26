package com.icbc.qingqi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 青启e城 演示系统 启动类
 */
@SpringBootApplication
@MapperScan("com.icbc.qingqi.module")
public class QingqiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QingqiApplication.class, args);
    }
}
