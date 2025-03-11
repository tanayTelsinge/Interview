EPAM Systems - SSE - Fullstack Java + Angular + React
Tanay que-
SOLID principles
Clean code principles 
1. Extract Method Principle - function should do one thing and do well , should be small 20-30 lines, avoid deeply nested structures by early returns or helper functns.
2. Minimize Function Parameters (reduce complexity) - atmost 3, more params, hard to read and maintain, use Objects instead.
3. Dependency Inversion principle - use abstractions instead of concrete classes, more loose coupling, useful to swap implementations w/o affecting client code.
   
OOPs
Abstract how implemented - abstract class and interface
Partial vs full abstract - how can we implement. 
Full Abstraction - via Interface only, no concrete method.
Partial abstraction - via abstract classes, some concrete methods.

Multiple Inheritance.
What if 2 interfaces - print method(), why only one method to implement 
interface A {
    void print();
}
interface B {
    void print();
}
// Class implementing both interfaces
class MyClass implements A, B {
    public void print() { // Only one implementation required -> if default method, then must override here else compilation error.
        System.out.println("Printing from MyClass");
    }
}
public class Main {
    public static void main(String[] args) {
        MyClass obj = new MyClass();
        obj.print(); // Output: Printing from MyClass
    }
}
Both A and B have void print(); with the same signature (same name and parameters).
Java treats them as the same method in the implementing class.
**But if print has implementation (default method), then need to override.
To call one of interface's default method. We do interfaceName.super.method().
class MyClass implements A, B {
    public void print() {
        A.super.print(); // Call A's default method
    }
}

if return type is different, can we overload.
Serializable what is it?
-convert object --> byte stream -> can be saved to a file, sent over a network, or stored in a database. 
-The reverse process, deserialization, converts the byte stream --> object.
Marker Interface, why is it used?
- Tagging Mechanism – It marks a class to indicate special behavior to the JVM or frameworks.
- Runtime Identification – The JVM checks it using instanceof or reflection (e.g., Serializable).

example take serialization

```
//Checking for Serializable Implementation
//In the ObjectOutputStream.writeObject() method (part of the Java serialization mechanism),
//Java first checks if the object implements Serializable:, else throws NotSerializableException.

public final void writeObject(Object obj) throws IOException {
    if (obj == null) {
        writeNull(); // Handle null objects
        return;
    }
    
    if (!(obj instanceof Serializable)) {
        throw new NotSerializableException(obj.getClass().getName());
    }

    writeNonNullObject(obj);  // Continue serialization process
}
```

Externalizable vs Serializable
Feature		Serializable					Externalizable
Method Control	JVM handles serialization automatically		Developer must manually implement serialization
Performance	Can be slow due to default serialization	Faster, as only required fields are serialized
Methods to Implement	None (automatic)			writeExternal() and readExternal()
Customization	Limited (transient fields, writeObject())	Fully customizable serialization logic
Security	Risk of unintended field serialization		Full control over sensitive data
When to use     Need automatic serialization                    Have Complex objects, Performance issue, need to exclude sensitive fields.

```
import java.io.*;

class Person implements Externalizable {
    String name;
    int age;

    public Person() {}  // Mandatory default constructor
    public Person(String name, int age) { this.name = name; this.age = age; }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(age);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        name = in.readUTF();
        age = in.readInt();
    }
}

public class Main {
    public static void main(String[] args) throws Exception {
        Person p = new Person("Alice", 25);

        // Serialize
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("data.ser"));
        oos.writeObject(p);
        oos.close();

        // Deserialize
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("data.ser"));
        Person deserialized = (Person) ois.readObject();
        ois.close();

        System.out.println(deserialized.name + ", " + deserialized.age);
    }
}

```
volatile - Internal working and why used?
![image](https://github.com/user-attachments/assets/e0a7bebd-acda-47d2-a3cd-5a01b134ff73)
By Default, thread first sets value in cache, and then updates it in RAM.
Now suppose there is shared variable,
Thread2 updates value to FALSE in cache2, but it will take some time to update RAM.
Thread1 reads value from cache1 or RAM, it gets TRUE, hence stale value.
Solution - private static volatile Employee employee;
Now, it is written and read directly from RAM. 
![image](https://github.com/user-attachments/assets/bdddada6-c5a6-4d4e-b61d-9403870533a7)

ExecutorService - what is it?
methods of executorService
submit() → Executes a single task.
invokeAll() & invokeAny() → Execute multiple tasks.
shutdown() & shutdownNow() → Stop the executor service.

Callable vs Runnable.
Feature			Runnable						Callable<T>
Package			java.lang						java.util.concurrent
Return Type		void (no result)					Returns a value (T)
Exception Handling	Cannot throw checked exceptions				Can throw checked exceptions
Used with		Thread, ExecutorService.submit(Runnable)		ExecutorService.submit(Callable<T>)

```java
import java.util.concurrent.*;

public class RunnableVsCallable {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Runnable Task (No Return Value)
        Runnable runnableTask = () -> System.out.println("Runnable executed by " + Thread.currentThread().getName());
        executor.execute(runnableTask);  // No return value

        // Callable Task (Returns a Value)
        Callable<Integer> callableTask = () -> {
            Thread.sleep(1000); // Simulate some computation
            return 42;
        };
        Future<Integer> future = executor.submit(callableTask);  // Returns Future

        System.out.println("Callable Result: " + future.get()); // Blocks until result is ready

        executor.shutdown();
    }
}

```
Future vs CompletableFuture
Feature			Future			CompletableFuture
Result Retrieval	get() (Blocking)	thenApply(), thenAccept() (Non-Blocking)
Exception Handling	Requires try-catch	exceptionally(), handle()
Chaining Tasks		Not supported		Supports chaining (thenApply(), thenCompose())
Multiple Tasks Exectn   Use ExecutorService.invokeAll()	CompletableFuture.allOf()
Parallel Execution	Requires explicit ExecutorService  Built-in support (supplyAsync(), runAsync())

Future code snippet:
```java
import java.util.concurrent.*;

public class FutureExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Integer> task = () -> {
            Thread.sleep(1000);
            return 42;
        };
        Future<Integer> future = executor.submit(task);
        // Blocking call (waits for result)
        Integer result = future.get();
        System.out.println("Future Result: " + result);
        executor.shutdown();
    }
}

```

```java
import java.util.concurrent.*;

public class CompletableFutureExample {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            return 42;
        }, executor);

        future.thenAccept(result -> System.out.println("CompletableFuture Result: " + result));

        System.out.println("Doing something else...");

        executor.shutdown();
    }
}
```
How did u use CompletableFuture and ExecutorService.
answered above.
Java 8 Features:
Predicate vs BiPredicate
- both return boolean, but Predicate takes 1 argument, BiPredicate takes 2.
- Predicate<Integer> isEven = n -> n % 2 == 0;
- BiPredicate<Integer, Integer> isSumEven = (a, b) -> (a + b) % 2 == 0;

 Function<T, R> → Takes input, returns output (Use for transformations).
   Function<Integer, String> intToString = num -> "Number: " + num;
   System.out.println(intToString.apply(10));
 Consumer<T> → Takes input, no return (Use for actions like printing/logging).
 
 Consumer<String> printMessage = message -> System.out.println("Message: " + message);
 printMessage.accept("Hello, World!");
 
 Supplier<R> → No input, returns output (Use for providing/generating values). eg. random number.
Supplier<Double> randomNumber = () -> Math.random();
randomNumber.get().

Functional Interface 
✔ A functional interface has exactly one abstract method.
✔ It enables lambda expressions for concise code.
✔ Java provides built-in functional interfaces in java.util.function.
✔ The @FunctionalInterface annotation is optional but recommended for safety.

Return types of these. - function -T takes one arg returns R(result), predicate (one T,boolean R), consumer (one T, no R), supplier (no T, one R)
HashSet working.
 Stores unique elements (No duplicates)
✔ Uses HashMap internally (Elements are keys, values are a dummy object)
✔ Unordered (Does not maintain insertion order)
✔ Allows null value
✔ Operations (add(), remove(), contains()) are O(1) average, O(n) worst case
✔ Uses hashCode() for bucket placement
✔ Collision handling: if collision, adds a new node in linkedlist

Working of hashCode() and equals() in HashSet
hashCode(): Determines the bucket where the element will be stored.
equals(): If multiple elements have the same hashCode(), equals() is used to check for actual equality.
Scenario:
If hashCode() is different → Element goes to a different bucket.
If hashCode() is the same → equals() is checked.
If equals() returns true → Duplicate, not added.
If equals() returns false → Collision, element stored in the same bucket (Linked List or Balanced Tree).

Before Java 8 → Linked List
Java 8+ → Balanced Tree (Red-Black Tree) for large collision

Covariant return type.
- We can return Child as Type to Parent Type
-  no need to explicit casting. (earlier if not typecasted, it threw ClassCastException at runtime).
-Introduced in Java 5.

Before Covariant return type support
```java
class Animal {
    Animal getAnimal() {  // Parent class method
        return new Animal();
    }
}

class Dog extends Animal {
    @Override
    Animal getAnimal() {  // Cannot return `Dog`, must return `Animal` (check return type)
        return new Dog();
    }
}

public class Main {
    public static void main(String[] args) {
        Dog dog = (Dog) new Dog().getAnimal();  // Explicit cast needed ❌
        System.out.println("Dog object: " + dog);
    }
}

```

```java
class Animal {
    Animal getAnimal() {  // Parent method
        return new Animal();
    }
}

class Dog extends Animal {
    @Override
    Dog getAnimal() {  // Covariant return type: returning `Dog`
        return new Dog();
    }
}

public class Main {
    public static void main(String[] args) {
        Dog dog = new Dog().getAnimal();  // No explicit cast needed ✅
        System.out.println("Dog object: " + dog);
    }
}


```
Find second non repeating character in stress.
s = "stress";
solution - r

  String s = "stress";

LinkedHashMap<Character, Long> freqMap = s.chars()
	.mapToObj(c -> (char) c)
	.collect(
		Collectors.groupingBy(
			Function.identity(), //as character would be key itself
			LinkedHashMap::new,
			Collectors.counting()) // counting returns Long
	);
Character res = freqMap
	.entrySet()
	.stream()
	.filter(word -> word.getValue() == 1)
	.skip(1)
	.findFirst() //first first returns Optional hence use get
	.get()
	.getKey(); //as get() will return entry
System.out.println(res);


Aditya que-
Core Java
1. Given 3 payment providers Gpay, AmazonPe, PhonePe implement payment service using loose coupling in Java
------------------------------
Spring boot
3. 
@Scope("prototype")
AbcService{
updateTemperature(){
	//like update temp 20degree etc
}
} 
XYZService{
	@Autowired
	AbcService abcService
} 
How will ABCService behave in XYZService as ABCService is prototype
12. How to create a bean conditionally in Spring boot
13. LifeCycle of Bean in Spring boot and print sout when each lifecycle method is called

@Component
public class MyBean {
    public MyBean() {
        System.out.println("1. Constructor: MyBean instance created");
    }
    @PostConstruct
    public void init() {
        System.out.println("2. @PostConstruct: Bean initialization logic");
    }
    @PreDestroy
    public void destroy() {
        System.out.println("3. @PreDestroy: Cleanup before bean destruction");
    }
}
@Configuration
class BeanConfig {
    @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
    public CustomBean customBean() {
        return new CustomBean();
    }
}
class CustomBean {
    public CustomBean() {
        System.out.println("CustomBean: Constructor called");
    }
    public void customInit() {
        System.out.println("CustomBean: Init method called");
    }
    public void customDestroy() {
        System.out.println("CustomBean: Destroy method called");
    }
}
Expected Output
1. Constructor: MyBean instance created
2. @PostConstruct: Bean initialization logic
CustomBean: Constructor called
CustomBean: Init method called

// When application is shutting down
3. @PreDestroy: Cleanup before bean destruction
CustomBean: Destroy method called

14. How Spring behaves differently when we use constructorInjection and SetterInjections
15. What are Spring AOP Design Patterns
-------------------------------
AWS
4. What is partitionKey and sortKey in DynamoDB
5. How to purge SQS in Amazon
--------------------------------
HIbernate

Hibernate - ORM framework

Mappings:
1. OneToOne
class Employee {

 private int employeeId;
@OneToOne(targetEntity=Department.class)
private Department department;
}

2. ManyToOne
many students have one address
class Student {

 private int employeeId;
@ManyToOne(cascade=CascadeType.ALL)
private Address address;
}

@Entity
public class Address {
    @Id
    private int addressId;
    private String street;
    private String city;

    @OneToMany(mappedBy = "address", cascade = CascadeType.ALL)
    private Set<Student> students;  // A Set to store all associated students
}
-------------------*** This is good when primary key is associates as foreign key to 2nd table, but if not, then we have to use @JoinColumn and specify column
//owning side
public class Student {
    @Id
    private int studentId;  // Primary key
    @ManyToOne
    @JoinColumn(name = "address_id")  // Foreign key referencing Address table's primary key
    private Address address;
}

owned or referenced side
public class Address {
    private int addressId;  // Primary key (Not a foreign key)
    @OneToMany(mappedBy = "address")
    private Set<Student> students;  // No foreign key here, just a collection of students, This is optional if we don't want students here.
}
Address has a primary key (addressId), but it does not directly act as a foreign key.

3. ManyToMany
many students can have many degrees
//DegreeStudentThirdTable will get created
join COlumn and inverseColumn will be fields in third table.
class Student {

 private int employeeId;

@ManyToMany(targetEntity=Degree.class, cascade = {CascadeType.ALL})
@JoinTable(name="DegreeStudentThirdTable",
	  joinColumns = { @JoinColumn(name="StudentId")},
	  inverseColumns = { @JoinColumn(name = "CertificateId")} )
private List<Degree> degreess;
}


2. Types of sessions in hibernate
Session: A short-lived object used for CRUD operations and interacting with the database.
SessionFactory: A long-lived object that is responsible for creating sessions and managing the Hibernate configuration.

Session Lifecycle:

Open a session: sessionFactory.openSession().
Begin a transaction: session.beginTransaction().
Commit the transaction: session.getTransaction().commit().
Close the session: session.close().

7. Types of caches in Hibernate
Why cache = hibernate caches query data for faster processing.
3 types
1st level (Session cache) - enabled by default, can't disable, part of Session object, objects part of this session not visible to other session.
When session is closed, cache is lost.** to resolve this 2nd level cache.

2nd level (SessionFactory cache) - disabled by default, need to enable by config, 
implementation - EHCache, Infinispan. eg. add hibernate-ehcache dependency & add use-second-cache in config.
Used when u need to access data across session (in same SessionFactory).

Query level cache - used to cache results of queries. (HQL, Criteria or Native queries).
It stores result set of queries and reuse.
Stores entity info in 2nd level cache. To use query cache, enable 2nd level and query cache.

String hql = "FROM User WHERE age > 30";
Query query = session.createQuery(hql);
query.setCacheable(true); // Enable caching for this query
List<User> users = query.list(); //result is cache.

8. What is EntityManager?
EntityManager is an interface in JPA that manages database operations for entities.

9. WHat is PersistenceContext?

What is @PersistenceContext?
@PersistenceContext is a JPA annotation used in Spring and Java EE to inject an EntityManager and manage the persistence context.
It is the cache of entity instances maintained by JPA.
Ensures that entities are managed and tracked within a session.
Provides automatic change detection (any change to a managed entity is automatically synchronized with the database when the transaction commits).
@Repository
public class UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveUser(User user) {
        entityManager.persist(user); // Saves user to the database
    }
}
---------------------------------
SQL
10. SQL - >Find employee whose salary is greater than average salary where employees are grouped by entry_date.

select e.* from employees e
JOIN 
(
select entry_date, avg(salary) as avg_salary
from employees
 group by entry_date
 ) as avg_table
 ON avg_table.entry_date = e.entry_date
 where e.salary > avg_table.avg_salary;

--------------------------------
REST API

11. How to manage API versioning
API versioning ensures backward compatibility while introducing new features. Common strategies include:

1️⃣ URL Path Versioning (Most Common) – Version in the URL (e.g., /api/v1/employees).
2️⃣ Query Parameter Versioning – Pass version as a query param (/api/employees?version=1).
3️⃣ Header-Based Versioning – Use custom headers (X-API-Version: 1) or content negotiation (Accept: application/vnd.company.v1+json).
4️⃣ Subdomain Versioning – Different subdomains (v1.api.company.com).
5️⃣ API Gateway-Based Versioning – API gateways (Kong, AWS API Gateway) route requests based on URL, headers, or query params.
✅ Best Approach?
For public APIs: URL path versioning is preferred (simple & cacheable).
For enterprise APIs: Header-based versioning keeps URLs clean.

14. WHat is RMM in REST API.
 "The Richardson Maturity Model (RMM) defines 4 levels of REST API maturity: 
Level 0 (single endpoint), eg. only POST
Level 1 (resource-based endpoints),
Level 2 (proper HTTP methods & status codes),
and Level 3 (HATEOAS, where responses include hypermedia links to guide clients). A Level 3 API is considered fully RESTful."
{
  "id": 123,
  "name": "Alice",
  "role": "Engineer",
  "_links": {
    "self": { "href": "/employees/123" },
    "update": { "href": "/employees/123", "method": "PUT" },
    "delete": { "href": "/employees/123", "method": "DELETE" }
  }
}

--------------------------------
Kafka
----------------------------------
6. Working of Kafka
Kafka is a distributed event streaming platform 
-where producers publish messages to topics, 
-brokers store them in partitions, and 
-consumers pull messages for processing, 
ensuring high scalability, durability, and real-time data streaming.

Zookeeper managed it, but since Kafka 3.0, they are using KRaft, distributed manager.

✅ RabbitMQ → Message Broker (Push-based)

Best for real-time messaging (e.g., microservices, task queues).
Messages are removed after consumption.
Low latency, good for small messages.
✅ Kafka → Event Streaming Platform (Pull-based)

Best for big data & real-time analytics (e.g., log processing, event-driven systems).
Messages are stored for a fixed time (even after reading).
High throughput, good for large data streams.
When to Use?
✔ Use RabbitMQ → For quick messaging between services.
✔ Use Kafka → For processing large-scale event data.


1. find avg using stream java 8 (use mapToInt as avg function can be used, its specific to mapToInt)
2. Design payment system, how can u select different payment on runtime based on param.
(use strategy + factory pattern) - as interviewer said no if else.
3. SAGA Microservice pattern.
4. Coding problem (Simplec
5. Diff setter and constructor injection. (which is better) 
-> Constructor because error related to dependency is thrown as compile time).
GlobalException how to achieve?


Glassdoor ques:

**Scenario based questions JAVA. (prepare).
Backend: 
given maps, sort using comparators. (coding)
Spring boot - annotation based, autowired, use of qualifiers.
how microservices communicate?
lambda expressions and FI.
Solid principle code given. (OPEN CLOSE PRINCIPLE was breaking).
solve java problem using filter and map.
**Transaction mgmt (how propogation works, if parent calls child method, if child has issues, will parent stop or how?
How to put optional path variables and request params.
problem - solved using LEFT JOIN SQL.
STORED PROCEDURES.
ThreadLocal what is it? Executor Framework, CompletableFutures.
Aware Interfaces? How to inject Prototype bean in singleton bean?
Different architecture in microservices? SAGA design pattern.
Java 8 coding que - comparator.
Multithreading que = using executor service, print odd even numbers 
What improvement in java 8 for memory management.
Client round:
How to port diff ports for diff envs?
Suppose game, diff players, print highest score of each player if he 
@Value @Qualifier
Java 8 terminal operators. How diff operations are performed.
How to monitor multiple microservices? If one breaks, how to identify?

**Unit test cases
Test-cases(Junit & Automation)
Isolation levels
java 8+ features
Transaction mgmt
Concurrency how handle?
Multithreading and locking.
Why hibernate and jpa
Mysql queries.
**Do completableFture
Spring bean life cycle .
How spring is initialised 
How dependency is injected .
Java 8 lamba expression ?
Java 8 function interfaces ?
Various questions on Spring Cloud Projects, their usage and implementation. Project discussion and why I used those technologies. Few questions on Microservices design and how to maintain Data consistently.
Core Java questions on Multithreading & Collection framework along with stream.
Hashmap implementation generic algorithm ?
1. Shallow Copy Vs Deep Copy & Implementation 2. Co-varient Return Types 
3. Collection - For maintaining order in map -> LinkedHashMap - Concurrent HashMap - Problem where we need sort for custom object --> Override Equals And HashCode 
4. try-with-resources with implementation
5. SOLID principles with example # Java 8 - Parallel Stream & its pros and cons # Spring Boot - Profiling - Saga Pattern for distributed transactions -- cache concept # Microservices - On Gateways, 
Discovery server ---> We use Eureka - Hsytrix etc.. --> Fault tolerance # Program - Find Cube of number then filter total > 100 & then find its average

Single Responsibility Principle (SRP) – A class should have only one reason to change.
✅ Example: Separate User and AuthService classes for user data and authentication.
Open/Closed Principle (OCP) – Open for extension, closed for modification.
✅ Example: Use interfaces (Payment) and extend them (CreditCardPayment, PayPalPayment).
Liskov Substitution Principle (LSP) – Subclasses should replace the base class without breaking functionality.
✅ Example: Bird should have only behaviors common to all birds (avoid making Penguin extend Bird if it can't fly).
Interface Segregation Principle (ISP) – No unnecessary dependencies; use specific interfaces.
✅ Example: Printer and Scanner interfaces instead of one large interface.
Dependency Inversion Principle (DIP) – High-level modules should depend on abstractions, not concrete implementations.
✅ Example: Inject NotificationService into UserService instead of hardcoding EmailService.

ACID Properties (Short Explanation for Interviews)

Atomicity – A transaction is all or nothing.
✅ Example: Money transfer: If debit succeeds but credit fails, rollback.
Consistency – Database moves from one valid state to another.
✅ Example: Foreign keys, constraints ensure valid data.
Isolation – Concurrent transactions don’t interfere.
✅ Example: Using isolation levels (READ COMMITTED, SERIALIZABLE).
Durability – Committed data is permanently saved.
✅ Example: Data written to disk and logs to survive crashes.

DB Isolation Levels : comes in picture in case of concurrency txs to prevent dirty reads.
Imagine a banking system where multiple users are transferring money at the same time:

Read Uncommitted: A user sees another user's unfinished transaction (risky).
Read Committed: A user only sees committed transactions (safer).
Repeatable Read: A user's balance remains the same throughout a transaction.
if a transaction reads the same row multiple times, it will always see the same data throughout the transaction.

Serializable: Only one user at a time can make changes (slowest but safest), locks entire table during tx.

1. What is optional class and how to use it? (do java 8 features and coding)
2. Java 8 features.
Java life cycle??
2. How does hashset working internally? 
3. How do you find the most repeated element in an array and it's count using Java8 (do some stream coding probs).
Map<Integer, Long> frequencyMap = Arrays.stream(arr)
                .boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        // Print the frequency of each element
        frequencyMap.forEach((key, value) -> System.out.println(key + " -> " + value));
4. Collections - types, differences. eg. Treeset/Hashset. ArrayList vs linkedlist
5. Collection traversals short programs.
6. How to make class immutable.
7. Lazy keyword
What does the specific annotations in Spring mean?
@Component	Generic Spring bean
@Service	Marks a service layer component
@Repository	Marks a DAO/repository component
@Controller	Marks a Spring MVC controller
@RestController	Combines @Controller and @ResponseBody
@Autowired	Dependency injection
@Qualifier	Specifies a particular bean when multiple exist
@Primary	Marks a bean as the default
@Value	Injects property values
@Configuration	Defines configuration class
@Bean	Defines a bean
@Scope	Defines bean scope
@SpringBootApplication	Main Spring Boot application class
@RequestMapping	Maps HTTP requests
@GetMapping, @PostMapping	Maps specific HTTP methods
@PathVariable	Extracts values from URL
@RequestParam	Extracts query parameters
@RequestBody	Binds JSON/XML request bodies
@ResponseBody	Converts return values to JSON/XML
@Transactional	Manages transactions
@PreAuthorize	Restricts access to methods
DSA Problem: Given an array of integers, find the longest consecutive sequence of numbers and return its length. (Time Complexity: O(n))
Java 8 Problem: Given a list of strings, write a Java 8 program to filter out strings that start with "A", convert them to uppercase, and sort them
java 8 releted question Hashing Question
Abstraction vs Interface Collections Stream API Spring Security Microservices REST API Code to find LCM and GCD
Threads, Interfaces
JPA & Hibernate, Microservices concepts.
Data Governance Explain in detail?
search and count the mentioned character in an array, filter the array based on an object using Java 8 features
String grouping of its frequency using stream api
 java internals, spring boot internals
1. ArrayList vs LinkedList 2.Response code 3. Junit code snippets 4. Spring 5.Hibernate- criteria
Specification
 asked to write code for Singleton, Immutable class, Stream API implementation.
How to reverse an array and save it into the same array?
Pollfill of bind, prototype, this, js engine, microtask queue, flatten array, implement singleton pattern
finalize vs dispose


DSA
Max sum subarray

Rotate LL
Graph BFS
Reverse a LL in 1 traversal
find max product of two numbers, sum of odd factors of numbers in array
First nth element in linked list
Anagram program in a set of string
**. 1 Group of anagram from array of String 2 ChocolateDistributionProblem
LCS (least common subsequence)
Hashing and sorting related problems 
Group Anagrams (Leetcode 49) → Use sorting + HashMap
Top K Frequent Elements (Leetcode 347) → Use HashMap + Sorting or MinHeap
Two Sum Variations → Using HashMap for O(N) lookup
Sort Characters by Frequency → HashMap for frequency count + Sorting
Subarray Sum Equals K (Leetcode 560) → HashMap + Prefix 
arrays,string related problems
Problem solving questions on strings, array
Merge two sorted array
Given list of tickets and the tickets are not cyclic. Find the itenary of a given list. we have the same question present in geeks for geeks.
Code on stacks

find the sum of 2 largest odd no from list
There were 2 live coding questions one using dp and another using java8 in the first round followed by some basic java and spring boot concepts.
Also do sorting codes.

Project self:
1. Microservice architecture in your project.

1. Find middle of linked list using pointers.

    public ListNode middleNode(ListNode head) {
        ListNode slow = head;
        ListNode fast = head;
        if(head == null) return head;
        while(fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }

        return slow;
    }

***Need to stream on list of StudentsGrade class which has 2 attribute: studentName and Map of student grades.
Requirement: 1. get average marks map for reach student 
        Map<String, Double> averageMarks = students.stream()
            .collect(Collectors.toMap(
                StudentsGrade::getStudentName,
                student -> student.getStudentGrades().values().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0)
            ));
2. a map of students having student name who scored highest and in which subject.
LRU cache.
- House Robber and Sort Colors
Course schedule 2.
Leetcode Two Sum 
 Medium level 2 questions on Custom object Java 8 stream and aggregation 

Other
 design patterns, SOLID patterns, OWASP 10 (asked).
What are the various security techniques used to authenticate web services?

git, cloud.
git pull vs git merge?
what factors u consider while designing API?
API optimizations, AWS Services, Design Patterns, Architectural Patterns
AWS Cloud general questions 
1st Technical Round :- 
SOLID Principle , Design pattern, Exceptions, Multithreading , JAVA 8 features and Project related things. 
Coding :- Stream API 
2nd Round :- Microservices , Spring Boot , CI/CD pipeline Asked to draw a microservices architecture
SOLID Principles and ACID properties with implementation 
Spring Boot, JPA and Hibernate general question 

Difference between null and undefined in JavaScript
JWT Token - Three important parts (Header, Payload, Signature) and all are in encoded form
How to create a JWT Token
What are Prototype methods in JavaScript
How to detect changes made to all public properties in JavaScript
ViewChild and ViewChildren in Angular
Behavioral Subject in Angular vs Observable
Difference between let and var in JavaScript

Frontend 
CSS Positioning
Local storage/session storage/cookies and its related questions SetTimeout code snippets Event loop concept/microtask queue object cloning/Object.assign() promises simple program with 
Array.reduce function Prototypal inheritance Object,create() and its related questions Arrow functions with 'this' keyword
React: Reconcilliation Context api virtual dom keys in react Coding - URL will be given.. Create a component and get the results from url and show it in the component
JavaScript Questions
Hoisting, closures.
Promises.
Hoisting, call, bind, apply, flatten array. Questions were directly asked from the courses of will sentence and kyle Simpson frontend masters.
JS Object assignment questions. (coding).
What is the difference between null and undefined in JavaScript?
What are prototype methods in JavaScript?
How can we detect changes made to all public properties in JavaScript?
What is the difference between let and var in JavaScript?
Things about cors in javascript
Name latest version of Javascript es6 features

Asking javascript, serverless functions, step functions, nodejs
 context,this call apply bind polyfills js or react?
Closure, Hoc, Arrays Redux Security features
2. Flatten a multidimensional array without using inbuilt function 
Flatten a nested array I/P: [1,2,[3],[[4]],[[[5]]]] O/P: [1,2,3,4,5]
[{name:'a',salary:20000,age:25}, {name:'b',salary:25000,age:23}, {name:'c',salary:34000,age:25} {name:'d',salary:13000,age:30}] Sort it first on the basis of age then on the basis of salary.
Half reverse the string: i/p: microsoft o/p: orcimtfos
Explain promise and write code for promises & its related methods.
3. Convert class based life cycle methods to functional life cycle hooks. 
2nd round:
1. Multiple questions on this object like what is this, they asked me find the value of this on from the JS snippets.
2. Merge a two sorted arrays 
3. Filter based on the id and sum the no of quantities from the array of objects 4. Create basic timer component using functional component

SYstem design
LLD - hotel booking system
Design Patterns with implementation
Difference between abstract factory and factory design pattern
LRU Cache
How is the database distributed in a microservice architecture?
What are the best practices for caching and logging in microservices?
How does the retry mechanism work in Spring Boot?
What are the different scaling strategies in microservices, and when should each be used?
What is an API Gateway, and how does a circuit breaker work in microservices?
How does data processing work in Kafka, and how do producers and consumers interact?
System design for Outlook meeting schedule

JWT (JSON Web Token) Questions
What are the three important parts of a JWT token?
Header
Payload (Subject)
Signature
(All are in encoded form)
How to create a JWT token?

jQuery Questions
How to find odd rows in a table using jQuery and apply CSS to the background?

React
1.What is closure. 2.Difference between useffect and useLayoutEffect. 3.What is useRef,usecallback,useMemo

Angular & RxJS Questions
, Typescript code
What is the difference between ViewChild and ViewChildren in Angular?
What is a BehaviorSubject in Angular?
Difference between BehaviorSubject and Observable in Angular?
Observables.
What is ngZone in angular?
What are pipes in Angular

SQL & LINQ Questions
materialized views, collections and triggers.
Best practices on trigger
Experience on Trigger frameworks - Explain with example
Use of the Type class
Retrieving all child record by querying on the master object (SOQL)
Database query optimization techniques 
Write an SQL to LINQ query that joins two tables and applies GROUP BY.

Web API Questions
How to download huge files using Web API?
Some info, what is web api

Other
 Git branching strategies
AWS Docker, K8s, Terraform, Ansile, Java

Interviewer started with theoretical Questions based on Java

Q. Internal working of @Transactional annotation in spring boot
Q. Spring bean scope
Q. Database Isolation levels
Q. How does Red Black Tree work
Q. REST API best practices
Q. Disadvantages of Stream API
Q. Questions based on Set and Map
Q. So many others but I don't remember..

Two Practical Questions were asked in the end based on Streams and Problem Solving (DSA)

Given a String array String[] str = ["This is spring boot @autowired", "I don't remember @correctly", "but it was like this only @qualifier annotation", "@autowired @qualifier"];

Q.1 - Find all the unique words that starts with '@'. I was asked to solve this using Java Streams. EPAM is extremely serious about streams :) .
  // Stream pipeline to extract unique words starting with '@'
        Set<String> uniqueWords = Arrays.stream(str)  // Convert array to stream
            .flatMap(sentence -> Arrays.stream(sentence.split(" "))) // Split on a single space
            .filter(word -> word.startsWith("@")) // Filter words that start with '@'
            .collect(Collectors.toSet()); // Collect unique words into a Set

Output = [@autowired,@qualifier,@correctly];

Q.2 - DSA Question - https://leetcode.com/problems/group-anagrams/description/

Solution 1

Set set = Arrays.stream(str)
.map(s -> s.split(" "))
.flatMap(Arrays::stream)
.filter(s -> s.startsWith("@"))
.collect(Collectors.toSet());


Java-Related Questions:

JVM, Heap memory, and Stack memory
Runtime polymorphism
Hierarchy of the collection framework
Difference between a Map and TreeSet, internal working of HashMap
Immutability of strings in Java, difference between StringBuffer and StringBuilder
Streams and filters (though no further questions as I wasn't familiar with them)
Functional interfaces
SOLID Principles, particularly the Liskov substitution principle
Data structures in used in code
Multi-threading and ExecutorService
Adapter pattern
Builder pattern
Asked about why we were using Camunda workflows and if we had evaluted other options like Netflix conductor
Reason for using Cosmos DB
DSA Questions:

Combination Sum-like problem: Instead of candidates, the number K was given, and all numbers from 1 through K were candidates. Similar to: Combination Sum
All Nodes Distance K in Binary Tree: Provided a graph-based approach which was correct, but the interviewer wanted another approach. Most time was spent on the first question, so didn't get to code this

Task 1 :
Q1) Get the Noida city employees from EmployeeList and sort the Noida city employees in the reverse alphabetically order by name of the employee using Java 8

Employee{
id,
name,
city;}

Couple of more questions on Java 8 features and stream APIs.

Task 2 :
Q1) Input: N = 5, array[] = {1,2,3,4,5} Output: 120 60 40 30 24 Find the product of array except of itself and you should not use / operator For 0th index, excluding 1 from product of whole array will give 120 For 1th index, excluding 2 from product of whole array will give 60 For 2nd index , excluding 3 from product of whole array will give 40 For 3rd index, excluding 4 from product of whole array will give 30 For 4th index , excluding 5 from product of whole array will give 24.

Q2) Input: lists = [[1,4,5],[1,3,4],[2,6]] Output: [1,1,2,3,4,4,5,6].
You are given an array of k linked-lists lists, each linked-list is sorted Merge all the linked-lists into one sorted linked-list and return it.

Round 2 - HLD/ Technical Round

HLD for a food delivery app
Java 8 features
Design patterns
Solid principles
Microservice Patterns

**Round 3 - Manageral Round **

Discussion around my previous project and some behavioural questions.

Compensation - https://leetcode.com/discuss/compensation/4854493/EPAM-or-SSE-or-Offer-or-Gurgaon-or-Declined

2. Missing Numbers
Easy
28m average time
85% success
0/40
Asked in companies
You are given an array 'ARR' of distinct positive integers. You need to find all numbers that are in the range of the elements of the array, but not in the array. The missing elements should be printed in sorted order.
Example:
If the given array is [4, 2, 9] then you should print "3 5 6 7 8". As all these elements lie in the range but not present in the array

