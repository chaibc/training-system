package com.hanmo.calligraphy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 翰墨学堂书法培训管理系统启动类。
 *
 * <p>@MapperScan 扫描各业务包下的 mapper 接口（含 domain.mapper 与 system.mapper）；
 * 启动后通过 server.port（默认 8080）提供 REST 接口。</p>
 */
@SpringBootApplication
@MapperScan("com.hanmo.calligraphy.**.mapper")
public class HanmoApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(HanmoApplication.class, args);
    }
}
