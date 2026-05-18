# Java Interview Questions (Based on EPAM Systems)

## 1. SOLID Principles & Clean Code
### Theory Questions
- Explain SOLID principles
- What are Clean Code principles?
- How do you ensure code maintainability?

### Practical Examples
```java
// Bad Code
public class OrderProcessor {
    public void process(Order order) {
        // Validate order
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order is empty");
        }
        if (order.getCustomer() == null) {
            throw new IllegalArgumentException("Customer is required");
        }
        
        // Calculate total
        double total = 0;
        for (Item item : order.getItems()) {
            total += item.getPrice() * item.getQuantity();
        }
        
        // Apply discount
        if (total > 100) {
            total = total * 0.9;
        }
        
        // Save order
        orderRepository.save(order);
        
        // Send notification
        emailService.sendConfirmation(order);
    }
}

// Clean Code with SOLID Principles
public class OrderProcessor {
    private final OrderValidator validator;
    private final PriceCalculator calculator;
    private final OrderRepository repository;
    private final NotificationService notificationService;

    public OrderProcessor(OrderValidator validator, 
                         PriceCalculator calculator,
                         OrderRepository repository,
                         NotificationService notificationService) {
        this.validator = validator;
        this.calculator = calculator;
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public void process(Order order) {
        validator.validate(order);
        double total = calculator.calculate(order);
        repository.save(order);
        notificationService.notify(order);
    }
}
```

## 2. OOP Concepts
### Abstract Classes vs Interfaces
```java
// Abstract Class Example
public abstract class PaymentProcessor {
    protected double amount;
    
    // Concrete method
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    // Abstract method
    public abstract void processPayment();
}

// Interface Example
public interface PaymentGateway {
    void processPayment(double amount);
    
    // Default method (Java 8+)
    default void validateAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }
    }
}
```

### Multiple Inheritance Scenarios
```java
interface A {
    default void print() {
        System.out.println("A");
    }
}

interface B {
    default void print() {
        System.out.println("B");
    }
}

// Must override when same default method exists
class MyClass implements A, B {
    @Override
    public void print() {
        A.super.print(); // Explicitly choose A's implementation
    }
}
```

## 3. Java Core Concepts
### Serialization
```java
// Serializable Example
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private transient String password; // Won't be serialized
    
    // Custom serialization if needed
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        // Custom serialization logic
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Custom deserialization logic
    }
}
```

### Marker Interfaces
```java
// Common Marker Interfaces
public interface Serializable {} // For serialization
public interface Cloneable {} // For object cloning
public interface Remote {} // For RMI

// Custom Marker Interface
public interface Auditable {}

@Audited // Custom annotation might be better in modern Java
public class Transaction implements Auditable {
    // Class implementation
}
```

## 4. Common Interview Tasks

### 1. Design a Thread-Safe Singleton
```java
public class DatabaseConnection {
    private static volatile DatabaseConnection instance;
    private Connection connection;
    
    private DatabaseConnection() {
        // Private constructor
    }
    
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }
}
```

### 2. Implement a Custom Collection
```java
public class CustomArrayList<T> implements Iterable<T> {
    private Object[] elements;
    private int size;
    
    public CustomArrayList() {
        elements = new Object[10];
        size = 0;
    }
    
    public void add(T element) {
        ensureCapacity();
        elements[size++] = element;
    }
    
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return (T) elements[index];
    }
    
    private void ensureCapacity() {
        if (size == elements.length) {
            elements = Arrays.copyOf(elements, size * 2);
        }
    }
    
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index = 0;
            
            @Override
            public boolean hasNext() {
                return index < size;
            }
            
            @Override
            public T next() {
                return get(index++);
            }
        };
    }
}
```

## Interview Tips

1. **Code Quality Focus**
   - EPAM emphasizes clean code and SOLID principles
   - Be prepared to explain your design decisions
   - Know how to refactor code for better maintainability

2. **Core Java Strength**
   - Deep understanding of OOP concepts
   - Interface vs Abstract class tradeoffs
   - Knowledge of Java 8+ features

3. **Practical Problem Solving**
   - Be ready to write code on whiteboard/IDE
   - Focus on writing maintainable solutions
   - Consider edge cases and error handling

4. **System Design Awareness**
   - Basic understanding of design patterns
   - Knowledge of enterprise application architecture
   - Experience with Spring/Spring Boot
