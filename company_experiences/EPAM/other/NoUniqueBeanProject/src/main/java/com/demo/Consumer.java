package com.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

    // Spring finds 2 beans of type B → NoUniqueBeanDefinitionException
    //To resolve 
    //@Primary - either on B class or @Bean method in Consumer
    //@Qualifier("AppConfig") or @Qualifier("b") for B class
    @Autowired
    @Qualifier("beanBFromConfig")
    private B b;

    public void print() {
        System.out.println("Injected: " + b.name());
    }
}
