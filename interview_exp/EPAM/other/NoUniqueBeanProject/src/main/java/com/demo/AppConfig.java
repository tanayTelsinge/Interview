package com.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    // Also registers BeanB → bean id: "beanBFromConfig"
    // Now Spring has TWO beans of type Payment: "beanB" + "beanBFromConfig"
    @Bean
    public B beanBFromConfig() {
        return new B();
    }
}
