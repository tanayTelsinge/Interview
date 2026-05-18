package com.demo;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// Registered via component scan → bean id: "beanB"
@Component
public class B {

    public String name() {
        return "BeanB via @Component";
    }
}
