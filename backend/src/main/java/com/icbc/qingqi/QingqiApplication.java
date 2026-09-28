package com.icbc.qingqi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 青启e城后端启动类
 * <p>
 * 架构说明：
 * - 单后端服务，同时支撑【用户端网页】【管理端网页】【微信小程序】
 * - 通过 JWT + 角色（USER / ADMIN）区分权限
 * - 接口路径规范：/api/v1/**（用户端） /api/admin/v1/**（管理端）
 * - 业务模块统一放在 module/ 子包下
 * <p>
 * 模块划分（对齐《02-项目目录结构.md》）：
 * - module/user      公共支撑：用户注册/登录/授权/个人信息
 * - module/message   公共支撑：消息中心
 * - module/guarantee 模块1：安居金融风控
 * - module/loan      模块2：轻创业智能授信
 * - module/bookkeeping 模块3：创业经营赋能
 * - module/budget    模块4：碎片消费治理
 * - module/safety    模块5：青年金融安全
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.icbc.qingqi.**.mapper")
public class QingqiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QingqiApplication.class, args);
    }
}
