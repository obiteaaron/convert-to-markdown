package com.nomadic.discuz.to.markdown;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAutoConfiguration
@ComponentScan({"com.nomadic.discuz.to.markdown"})
@MapperScan({"com.nomadic.discuz.to.markdown.mapper"})
@EnableTransactionManagement
@Configuration
public class Bootstrap {
    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
        SpringContextUtil.getBean(DiscuzToMarkdown.class).convert();
    }
}
