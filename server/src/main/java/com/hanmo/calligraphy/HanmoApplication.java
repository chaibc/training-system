package com.hanmo.calligraphy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hanmo.calligraphy.**.mapper")
public class HanmoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HanmoApplication.class, args);
    }
}
