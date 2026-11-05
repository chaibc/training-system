package com.hanmo.calligraphy.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>注册分页插件（MySQL 方言），使 BaseMapper / IService 的 selectPage 等分页方法生效；
 * 逻辑删除字段由 application.yml 的 logic-delete-field=deleted 全局配置。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 创建 MyBatis-Plus 拦截器链。
     *
     * @return 只挂载了分页内部拦截器的拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
