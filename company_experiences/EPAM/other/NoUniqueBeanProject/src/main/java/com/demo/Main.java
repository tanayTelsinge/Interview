package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        // ----- STEP 1: See the Exception -----
        // Run as-is → NoUniqueBeanDefinitionException on startup
        // because BeanB is registered twice:
        //   1. @Component on BeanB       → bean id: "beanB"
        //   2. @Bean in AppConfig        → bean id: "beanBFromConfig"

        ConfigurableApplicationContext ctx = SpringApplication.run(Main.class, args);

        // ----- STEP 2: After fixing, uncomment below -----
        // Consumer consumer = ctx.getBean(Consumer.class);
        // consumer.print();
    }
}
