- What is IOC Container?
  - Inversion of Control (IOC) Container is a framework that manages the creation and lifecycle of objects in a software application. It allows developers to define how objects are created, configured, and managed, promoting loose coupling and separation of concerns.

- Benefits of IOC Container:
    - Decoupling: IOC Container promotes loose coupling between components, making it easier to change or replace implementations without affecting the overall system.
    - Dependency Injection: It allows for dependency injection, where dependencies are provided to objects rather than being created by the objects themselves. This enhances testability and maintainability.
    - Configuration Management: IOC Container provides a centralized way to manage configuration settings and dependencies, making it easier to maintain and update the application.
    - Lifecycle Management: It manages the lifecycle of objects, ensuring that they are created, initialized, and destroyed properly, which can help prevent memory leaks and improve performance.

- Popular IOC Containers in Java:
    - Spring Framework: One of the most widely used IOC containers in Java, providing a comprehensive set of features for building enterprise applications.
    - Google Guice: A lightweight IOC container that focuses on simplicity and ease of use, often used in smaller projects or when a full-featured framework is not needed.
    - PicoContainer: A small and lightweight IOC container that emphasizes simplicity and minimal configuration, suitable for small to medium-sized applications.
    - Dagger: A compile-time dependency injection framework that generates code to manage dependencies, often used in Android development for its performance benefits.

- Example of using Spring IOC Container:
```java
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
public class Main {
    public static void main(String[] args) {
        // Load the Spring configuration file
        ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        
        // Retrieve a bean from the IOC container
        MyService myService = context.getBean("myService", MyService.class);

        // Use the service
        myService.performAction();
    }}
```

In this example, we load the Spring configuration file (beans.xml) to initialize the IOC container. We then retrieve a bean named "myService" from the container and call a method on it. The IOC container manages the creation and lifecycle of the MyService object, allowing for loose coupling and easier maintenance.

- Types of IOC Containers:
    - BeanFactory: The basic IOC container that provides basic support for dependency injection. It is lazy-loading, meaning it creates beans only when requested.
    - ApplicationContext: A more advanced IOC container that extends BeanFactory and provides additional features such as internationalization, event propagation, and support for various configuration formats. It is eager-loading, meaning it creates all beans at startup.

- How Spring AutoConfiguration works with IOC Container:
    - Spring AutoConfiguration is a feature that automatically configures Spring applications based on the dependencies present in the classpath. It uses the IOC container to manage the beans and their dependencies, allowing developers to focus on writing business logic rather than configuration. When a Spring Boot application starts, it scans the classpath for available beans and automatically configures them based on the application's needs, reducing the amount of boilerplate code required for setup.
    - What is classpath?
    - The classpath is a parameter in the Java Virtual Machine (JVM) that specifies the location of user-defined classes and packages. It is used by the JVM to locate and load classes at runtime. The classpath can include directories, JAR files, and ZIP files that contain compiled Java classes. When a Java application is executed, the JVM searches the classpath for the required classes and loads them into memory for execution.

eg. ```java
java -cp myapp.jar com.example.Main
```

- By Default, the IOC Container used is ApplicationContext, which is eager-loading. However, developers can choose to use BeanFactory if they prefer lazy-loading behavior.
- In Spring Boot, we change the default IOC container by using the @EnableAutoConfiguration annotation and specifying the desired container implementation. For example, to use BeanFactory instead of ApplicationContext, we can add the following annotation to our main application class:

```java
@EnableAutoConfiguration(exclude = {ApplicationContext.class})
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```
In this example, we exclude the default ApplicationContext and allow Spring Boot to use BeanFactory as the IOC container for our application.

- When to do Prototype scope?
- when we need new instance, eg. for each request, we hold user specific info.
- When to do Singleton scope?
- when we need only one instance, eg. for shared resources, we hold application specific info.
- When to do Request scope?
- when we need new instance for each HTTP request, eg. for web applications, we hold request specific info.
- When to do Session scope?
- when we need new instance for each HTTP session, eg. for web applications, we hold session specific info.
- When to do Application scope?
- when we need new instance for the entire application, eg. for web applications, we hold application specific info that is shared across all users and sessions.