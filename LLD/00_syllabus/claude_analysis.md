# The definitive LLD interview syllabus for senior Java engineers

**LLD interviews are the single most predictable round in Indian tech hiring — and the one senior candidates most often underestimate.** For a 6+ year Java/Spring Boot engineer targeting FAANG and Indian unicorns in 2024-2025, this guide covers exactly what's evaluated, what patterns to master, which problems each company asks, and a day-by-day preparation plan. The key insight: FAANG and Indian unicorns run fundamentally different LLD formats, and preparing for one does not prepare you for the other. FAANG companies favor 45-60 minute whiteboard OOD discussions emphasizing design thinking and trade-offs, while Indian unicorns run **90-120 minute machine coding rounds** demanding fully working, runnable Java code. Your preparation strategy must account for this split.

---

## What interviewers actually evaluate and how scoring works

The LLD round evaluates different skills depending on company type, and understanding this distinction is critical for targeted preparation.

**FAANG (Google, Amazon, Meta)** runs Object-Oriented Design rounds lasting **45-60 minutes**. The deliverable is class diagrams, interface definitions, and skeleton code — not runnable programs. Evaluation weights heavily toward design thinking, communication of trade-offs, and extensibility. Google's OOD round is embedded within coding rounds rather than standalone. Amazon sometimes includes machine coding. The interviewer spends significant time in back-and-forth discussion, probing your reasoning.

**Indian unicorns (Flipkart, Swiggy, Razorpay, PhonePe, CRED, Groww, Zepto)** run Machine Coding Rounds. Flipkart gives the longest window: **2 hours** (15 minutes pre-coding, 90 minutes coding, 15 minutes demo/review). Razorpay and PhonePe typically give 60 minutes. CRED runs 1.5-2 hours. You code independently in your IDE, then demo your working solution. **The code must compile, run, and produce correct output** — interviewers key in test cases and verify.

What separates a senior-level hire from a no-hire comes down to five signals. **Hire signals** include clear problem decomposition into entities with well-defined responsibilities, loose coupling between components, appropriate (not forced) use of design patterns, proactive identification of edge cases without prompting, and the ability to handle follow-up "what if we need to add X?" questions gracefully. **No-hire signals** include over-engineering (adding patterns without justification), God classes doing everything, inability to explain design decisions, starting to code without understanding requirements, and ignoring concurrency in systems that clearly need it.

At the senior level specifically, interviewers expect you to proactively discuss non-functional requirements (concurrency, error handling, extensibility) without being asked. You should drive the requirements conversation, make and defend trade-off decisions, and demonstrate that your design handles change without requiring a complete rewrite. A mid-level candidate produces working code; a senior candidate produces working code that is *obviously extensible* and can articulate exactly why each design decision was made.

---

## SOLID principles: how they're really tested

Interviewers at the senior level never ask "What is SRP?" directly. They give you a design problem and evaluate whether you **naturally apply** SOLID principles. The most common failure mode is over-engineering — introducing factories, builders, and decorators when simpler code would work. Senior candidates balance SOLID against KISS/YAGNI, saying things like: "I'll keep this simple for now; if requirements change, here's how I'd extend it."

**Single Responsibility Principle** is tested when interviewers see your class handling multiple concerns. A junior creates a `BookingService` that handles booking, payment, notification, and cancellation. A senior separates these into distinct classes with clear delegation. The interviewer's follow-up — "what if we change how fees are calculated?" — exposes SRP violations when that change requires modifying a God class. In Java, the fix is composition and delegation: `UserService` delegates to `EmailService` and `AuditLogger` rather than implementing email and logging directly.

**Open/Closed Principle** is the principle most directly tied to design patterns. It's tested through the "extensibility probe" — the interviewer asks you to add a new payment method, vehicle type, or notification channel. If your answer is "I'd modify the existing class," you've failed OCP. The correct approach uses interfaces and polymorphism: a `PaymentMethod` interface with `CreditCardPayment`, `UPIPayment` implementations, so adding `CryptoPayment` never touches existing code. The red flag interviewers watch for is `if/else` cascades or `instanceof` checks — these are OCP violations disguised as working code.

**Liskov Substitution Principle** catches senior candidates through the Bird-Penguin trap and the Rectangle-Square problem. If `Penguin extends Bird` and your `fly()` method throws `UnsupportedOperationException`, you've violated LSP. The fix is interface segregation — create a `FlyingBird` interface that only flying birds implement. Similarly, `ElectricVehicle` inheriting `refuel()` from `Vehicle` is a hierarchy problem, not a method-level problem. Senior candidates recognize when the inheritance tree itself is wrong and refactor using ISP.

**Interface Segregation Principle** prevents fat interfaces. The classic test: a `Worker` interface with `work()`, `eat()`, and `sleep()` methods forces `Robot` to implement empty `eat()` and `sleep()`. The fix is role-based interfaces: `Workable`, `Feedable`, `Restable`. But don't over-split — one-method-per-interface everywhere violates KISS. Aim for **cohesive, role-based** interfaces.

**Dependency Inversion Principle** is tested by looking for `new ConcreteClass()` inside service classes. If `ParkingLotService` directly creates `new InMemoryDatabase()`, you've violated DIP. Senior candidates always program to interfaces and use constructor injection. In Spring Boot context, this means `@Autowired` constructor injection of interface types, enabling unit testing with mocks and future implementation swaps.

---

## Design patterns ranked by interview frequency

Research across HelloInterview, AlgoMaster, and hundreds of interview experiences reveals a clear frequency hierarchy. **You need Tier 1 patterns cold; Tier 2 patterns well-practiced; Tier 3 only conceptually.**

**Tier 1 — appears in nearly every LLD interview:** Strategy is the single most tested pattern. Observer is #2. Factory Method is #3. Singleton is #4, primarily tested as a thread-safety discussion.

**Tier 2 — frequently tested:** State (vending machines, order workflows), Decorator (pizza toppings, stream wrappers), Builder (complex object construction), Composite (file systems), Command (undo/redo systems).

**Tier 3 — occasionally tested:** Chain of Responsibility, Adapter, Facade, Template Method, Proxy.

**Strategy** eliminates conditional logic by encapsulating algorithms behind an interface. In a parking lot, different parking strategies (nearest spot, cheapest spot) implement `ParkingStrategy`. In payment systems, `CreditCardPayment`, `UPIPayment`, `WalletPayment` all implement `PaymentMethod`. Spring Boot integration: inject strategies via `@Qualifier` or auto-wire a `Map<String, PricingStrategy>`. The senior signal is explaining that Strategy follows OCP — new algorithms mean new classes, zero modification to existing code.

**Observer** handles "when X changes, notify Y, Z, and W." Stock price tickers, notification systems, and order status changes all use Observer. In Spring Boot, the idiomatic implementation uses `ApplicationEventPublisher` and `@EventListener` rather than rolling your own Subject/Observer. Common mistakes include memory leaks from not unsubscribing, and `ConcurrentModificationException` from modifying the observer list during notification — use `CopyOnWriteArrayList` for thread-safe iteration.

**Factory Method** centralizes object creation. When the problem says "support different types of X" (notifications, vehicles, accounts), Factory is the answer. Don't confuse Simple Factory (static method returning objects) with GoF Factory Method (subclass-based creation). In interviews, Simple Factory is usually expected. Spring Boot's `BeanFactory` and `ApplicationContext` are factory implementations.

**State** is the centerpiece pattern for vending machines, order management, and traffic lights. Each state is a class implementing a common interface, and the Context object delegates behavior to its current State object. State transitions are managed within state classes themselves. The senior approach: draw a state transition diagram first — that diagram IS the design. Define invalid transitions explicitly (throw or log) rather than silently ignoring them.

**Builder** solves the telescoping constructor problem for objects with many optional parameters. The key implementation notes: make the outer class constructor private, validate in `build()`, and return an immutable object. Lombok's `@Builder` generates this automatically. Only use Builder for genuinely complex objects — using it for 2-3 field objects is over-engineering.

**Decorator** enables dynamic behavior composition without subclass explosion. The canonical Java example is `BufferedReader(new InputStreamReader(new FileInputStream()))`. For interviews, the coffee/pizza toppings problem is classic. Spring Boot uses decorator-like behavior through AOP proxies — `@Transactional`, `@Cacheable`, `@Async` all wrap the target method.

The most common **pattern combinations** in interview problems are Strategy + Factory (Factory creates the right Strategy based on input), Strategy + Observer (Strategy for algorithms, Observer for notifications), State + Observer (State manages transitions, Observer notifies of changes), and Composite + Iterator (tree structure + traversal).

A critical interview tip: **name the pattern after designing, not before**. Design cleanly first, then say "this is essentially the Strategy pattern." This shows natural design thinking rather than textbook recitation. Indian interviewers tend to ask about patterns explicitly and expect you to name them; US interviewers evaluate design quality without necessarily asking for pattern names.

---

## OOP concepts and the interview traps that catch senior candidates

Four specific traps recur in senior LLD interviews.

**Composition vs. inheritance** is the most important design decision in any LLD problem. The senior rule: **favor composition over inheritance** unless the relationship is genuinely IS-A with shared stable implementation. Composition provides loose coupling (swap implementations at runtime), avoids the fragile base class problem (changes to parent don't ripple through children), and enables testability (mock composed objects). When a candidate writes `Car extends Engine`, the interview is effectively over. The correct model: `Car` has-a `Engine`, injected via constructor.

**Abstract class vs. interface** is a decision you'll make multiple times per problem. The practical heuristic: start with an interface for the contract since it's more flexible. If you find shared implementation later, add an abstract class as a middle layer. Java's JDK models this perfectly: `List` (interface) → `AbstractList` (skeletal abstract class) → `ArrayList` (concrete). Use abstract classes when you need shared state (instance variables) or the Template Method pattern. Use interfaces for capability contracts (`Drawable`, `Serializable`) and when multiple inheritance is needed.

**Encapsulation violations** that interviewers catch include returning mutable collections (fix: `Collections.unmodifiableList(items)` or defensive copies), breaking encapsulation through protected fields in inheritance hierarchies (fix: private fields with controlled methods), and Law of Demeter violations like `order.getCustomer().getAddress().getZipCode()` (fix: `order.getShippingZipCode()`). Senior candidates design immutable value objects (`Money`, `Address`) and mark fields `final` by default.

**The diamond problem** in Java arises with default methods in interfaces. If two interfaces define the same default method, the implementing class must explicitly override to resolve ambiguity: `A.super.hello()`. Know this mechanism — it occasionally surfaces in interview discussions about interface design.

---

## Concurrency knowledge required for senior LLD rounds

Concurrency shows up in two ways: a classic LLD problem gets a concurrency extension ("what if two users book the same seat simultaneously?"), or the problem is inherently concurrent (rate limiter, thread pool, task scheduler). **For fintech companies, concurrency is especially critical** — payment processing, wallet operations, and inventory management all involve shared mutable state.

**Thread-safe Singleton** is the most common concurrency question in LLD. The five approaches, ranked: **Enum Singleton** (JVM-guaranteed, recommended by Joshua Bloch), Bill Pugh/Inner Static Class (lazy + safe via class loading), Double-Checked Locking (requires `volatile` — broken without it pre-Java 5), Eager Initialization (safe but not lazy), and Synchronized `getInstance()` (safe but slow). In Spring Boot, singleton scope is the default — Spring's IoC container manages the lifecycle, making programmatic singleton patterns unnecessary for managed beans.

**The synchronization primitives** you must know form a progression. `synchronized` is simplest (monitor-based, no tryLock, no fairness). `ReentrantLock` adds `tryLock()` with timeout, `lockInterruptibly()`, and fairness policies — always use in try-finally blocks. `ReentrantReadWriteLock` separates read and write locks for read-heavy scenarios like caches — multiple readers can proceed simultaneously when no writer holds the lock. `StampedLock` (Java 8+) adds optimistic reads for maximum read throughput but isn't reentrant.

**Concurrent data structures** map directly to LLD patterns. `ConcurrentHashMap` (lock striping across segments, thread-safe reads without locking) is essential for caches and registries. `BlockingQueue` (producer-consumer handoff with `put()`/`take()` blocking semantics) powers message queue and task scheduler designs. `CopyOnWriteArrayList` (thread-safe iteration, expensive writes) is perfect for Observer pattern subscriber lists. `AtomicInteger` (lock-free CAS operations) handles counters like available parking spots.

**LLD problems that commonly require concurrency** include parking lot (concurrent entry/exit — `AtomicInteger` for spot counts), movie ticket booking (seat reservation races — optimistic locking), rate limiter (token bucket with `AtomicInteger`), task scheduler (`ScheduledThreadPoolExecutor` + `PriorityBlockingQueue`), and inventory management (concurrent order processing with optimistic locking). At the senior level, proactively identifying shared mutable state and discussing thread safety — even when not asked — demonstrates the depth interviewers expect.

---

## DB schema and API design: what's expected and when

**DB Schema design** appears primarily in Indian unicorn interviews. Flipkart, Swiggy, and Groww specifically expect schema design for SDE-2+ roles. The approach: derive tables from your class design (ORM-driven), normalize to **3NF** (sufficient for interviews), then discuss denormalization trade-offs for read-heavy scenarios. Index on frequently queried columns (WHERE, JOIN, ORDER BY), mention composite indexes for multi-column queries, and articulate the trade-off that indexes speed reads but slow writes. For fintech, know the **double-entry bookkeeping** schema: every transaction creates both a debit and credit ledger entry, enabling reconciliation through the invariant that sum of debits equals sum of credits.

**API design** matters most for fintech roles. The critical concept is **idempotency** — network timeouts and retries can cause duplicate payments. The solution: client-generated idempotency keys with a DB UNIQUE constraint on `(client_id, idempotency_key)`. The flow: check if key exists → if yes, return cached response → if no, create IN_PROGRESS record → process payment → mark SUCCESS or FAILED. Same key with different payload throws `IdempotencyConflictException`. Study Stripe's API as the gold standard for error handling, using structured error responses with application-specific error codes, request IDs, and field-level details. Use cursor-based pagination over offset-based for large datasets, and URI versioning (`/v1/users`) for API versioning.

---

## Most asked LLD problems by company (confirmed 2023-2025)

**Razorpay** heavily favors in-memory systems: **In-memory Search Engine** (confirmed Oct 2024, Feb 2024 — the most repeated question), In-memory SQL-like Database, Survey Management Service, and Parking Lot. For SSE roles, expect subscription platform design with recurring billing.

**Flipkart** is known for creative problems with extensions: **Online Auction System (SuperBidder)** with highest-unique-bid-wins logic (confirmed Nov 2024, SDE-2/3), Conference Room Booking, Digital Wallet (FkRupee), and Ride-Sharing App. SDE-3 extensions add constraints like buyer budget limits. The 2-hour format includes a code demo where the interviewer enters test inputs.

**Swiggy** asks **Splitwise/Expense Management** (confirmed multiple times), food delivery system LLD, chess board entity design, and a vaccine appointment scheduling system.

**Groww** focuses on fintech-specific problems: **Stock Exchange Platform** with order matching engine (confirmed Dec 2024, SDE-3), SIP system design, and Splitwise-like expense sharing.

**Zepto** strongly favors **Google Calendar** design (confirmed Jun, Sep, Oct 2024 — the most repeated question), quick commerce order management, rider management, and Air Traffic Controller with strategy pattern.

**PhonePe** asks **Customer Support/Issue Tracking System** with agent assignment strategies (confirmed on codezym.com), wallet system, and parking lot.

**CRED** tests **Payment Processing Package** design (confirmed 2024), Contact Management System with prefix search, and payment recommendation systems.

**Amazon** frequently asks **Parking Lot** (the single most common LLD question across all companies), Elevator System, Amazon Locker, movie ticket booking (BookMyShow), and e-commerce cart design.

**Google** runs OOD-style discussions on File System design, Calendar System, Chess/board games, Parking Lot, and Hotel Management — focusing on class hierarchies and design reasoning rather than working code.

---

## How to structure your LLD answer in 45-60 minutes

The framework interviewers expect follows a strict sequence. Deviating from this order — especially skipping requirements — is the #1 reason senior candidates fail.

**Minutes 0-5: Clarify requirements.** Ask about core features, users, actions, concurrency needs, and error handling. Split into functional and non-functional requirements. Confirm scope — what's in and what's out. Write requirements down visibly. Every LLD interview starts with a vague prompt; your job is turning it into something concrete before touching design.

**Minutes 5-10: Identify entities.** Scan requirements for meaningful nouns — these become classes. Apply the filter: does it maintain changing state or enforce rules? → Class. Is it just information attached to something else? → Field or enum. Identify the orchestrator entity that coordinates the workflow (ParkingLot, Game, OrderService).

**Minutes 10-25: Class design and relationships.** Translate entities into classes with attributes and core methods. Define interfaces for shared behaviors. Use enums for fixed types. Apply SOLID principles, especially SRP. Favor composition over inheritance. Define the central manager/facade class.

**Minutes 25-40: Core implementation.** Implement the most critical methods. For FAANG: skeleton code with key method bodies. For Indian unicorns: fully working code with I/O handling. Get core functionality working first — don't try to implement everything.

**Minutes 40-50: Patterns, DB, API.** Name the patterns you've used (or would use). If applicable, sketch the DB schema and API contracts. For machine coding rounds, this may be compressed or skipped in favor of more working code.

**Minutes 50-60: Review and extensibility.** Discuss how the design handles future changes. Address concurrency considerations. Handle the "what if" follow-up questions. This is where senior candidates shine — showing the design is extensible without having over-built it upfront.

---

## The ten mistakes that sink senior candidates

**Over-engineering is the #1 killer.** Adding Strategy patterns when there's only one implementation, building elaborate Factory hierarchies for two object types, and using every GoF pattern to "show off" all signal poor judgment. Start simple. When the interviewer asks "how would you extend this?", then explain how the design evolves.

**Cognitive overload from solving everything simultaneously** leads to paralysis. The fix is the sequential framework above — requirements first, then entities, then design, then code. Never try to think about classes, methods, edge cases, and patterns all at once.

**Skipping requirements clarification** wastes 10-15 minutes mid-interview when wrong assumptions surface. Never assume; if you do, share assumptions with the interviewer explicitly.

**Fake confidence from passive learning** is the most insidious trap. Watching YouTube solutions and nodding along does not translate to designing from a blank editor under time pressure. As one Staff Engineer wrote after multiple rejections: "Reading and watching gets you maybe 30% of the way there."

**Misusing inheritance** creates deep hierarchies for things that should be interfaces. Penguin extends Bird with `fly()` that throws an exception, ElectricVehicle inheriting `refuel()` — these are hierarchy problems that composition solves.

**Forcing design patterns** where they don't naturally fit signals textbook knowledge without practical judgment. Only **1-2 patterns per problem** (or none) is perfectly fine. Patterns should feel natural, not forced.

**Poor time management in machine coding rounds** means spending too long on design without writing code, or trying to implement all features instead of getting core functionality working first.

**Silent coding without explaining decisions** makes a good design indistinguishable from a bad one. Think aloud throughout. Verbalize trade-offs proactively.

**Treating testing as an afterthought** is a senior-level red flag. If your design relies on static methods or hardcoded dependencies, you'll struggle to answer "How would you unit test this?"

**Ignoring concurrency** in systems that obviously need it (payment systems, booking systems, shared resources) fails the senior bar. Identify shared state first, then discuss access patterns and synchronization.

---

## The 3-week preparation syllabus

### Week 1: Foundations (2-3 hours/day)

**Days 1-2** cover OOP and SOLID foundations. Day 1: review encapsulation, abstraction, polymorphism, inheritance, with Java-specific decisions (abstract class vs interface, composition vs inheritance). Day 2: deep-dive all 5 SOLID principles with Java code examples, focusing on SRP and OCP as the most frequently tested. Use Refactoring Guru and Head First Design Patterns.

**Days 3-5** cover design patterns. Day 3: Creational patterns — Factory, Singleton (all 5 thread-safe approaches), Builder. Day 4: Behavioral patterns — Strategy, Observer, State, Command (the four most critical). Day 5: Structural patterns — Decorator, Adapter, Facade, Composite. Implement each in Java. Use Refactoring Guru for visual explanations.

**Days 6-7** are first practice problems. Day 6: solve **Parking Lot** (the universal starter problem) in 45 minutes, then review. Day 7: solve **Tic Tac Toe** or **Snake & Ladder**, then watch 1-2 mock LLD interviews on YouTube.

### Week 2: Problem solving at speed (3 hours/day)

Solve one problem per day, timed. **Day 8:** BookMyShow/Movie Ticket Booking (entity design, concurrency for seat booking, DB schema). **Day 9:** Splitwise (expense sharing, settlement algorithms — common at Indian unicorns). **Day 10:** Digital Wallet/Payment System (directly relevant for fintech — transaction atomicity, concurrency, Strategy pattern for payment methods). **Day 11:** Ride-Sharing App (matching algorithm, pricing, real-time tracking — asked at Flipkart, Swiggy). **Day 12:** Rate Limiter or LRU Cache (technical system problems, concurrency-heavy — common at FAANG). **Day 13:** Full timed machine coding practice — pick any problem, complete working code in 90 minutes, then explain design decisions in 15 minutes. **Day 14:** Review all solved problems and do a mock interview on Pramp or interviewing.io.

### Week 3: Mastery and mock interviews (2-3 hours/day)

**Day 15:** Pub-Sub/Notification Service (Observer pattern, channel abstraction). **Day 16:** Elevator System or Vending Machine (State Machine pattern). **Day 17:** Concurrency deep-dive — synchronized, ReentrantLock, ConcurrentHashMap, ExecutorService. **Day 18:** DB Schema + API Design practice for 2-3 previously solved problems. **Days 19-20:** Full 60-minute mock interviews practicing the complete flow. **Day 21:** Final review — create a personal cheat sheet of your framework, common patterns with triggers, and Java idioms.

### Essential resources by category

**Primary study materials:** Head First Design Patterns (most recommended book), Effective Java by Joshua Bloch (Java-specific best practices), and Refactoring Guru website (visual pattern explanations). The GitHub repository **ashishps1/awesome-low-level-design** (22.1k stars) is the single best problem collection with Java solutions.

**Video courses:** Concept && Coding by Shrayansh Jain on YouTube/Udemy is the most recommended LLD course in India, covering SOLID, patterns, and full problems including Parking Lot, BookMyShow, Splitwise, and Snake & Ladder. Educative's Grokking the Low Level Design Interview covers 21 problems with UML and case studies.

**Practice platforms:** workat.tech/machine-coding for Indian company-specific machine coding practice; Codezym.com for LeetCode-style LLD practice; HelloInterview.com for structured LLD practice with delivery frameworks. For mock interviews: interviewing.io (anonymous mocks with FAANG engineers), Pramp (free peer-to-peer), and Preplaced.in (350+ mentors, India-focused).

**Company-specific preparation:** For Razorpay, practice in-memory data structure problems (search engine, SQL database). For Flipkart, practice auction/bidding systems and wallet design with extensions. For Zepto, practice Calendar design and schema design. For Groww, practice stock exchange matching engines. For Swiggy, practice Splitwise and food delivery systems. For CRED, practice payment processing packages with pattern-heavy design.

---

## Conclusion: the meta-strategy that ties it all together

The single most important insight across all research is that **LLD interviews test design judgment, not pattern knowledge**. Senior candidates who fail typically know more patterns than those who pass — they just apply them indiscriminately. The winning approach is starting simple, applying SOLID principles naturally, using composition over inheritance by default, and introducing patterns only when the problem demands them. When the interviewer asks "how would you extend this?", your design should accommodate the change with minimal modification — not because you over-built it, but because your abstractions were well-chosen.

For the 2-3 week timeline, the critical success factor is **active practice from a blank editor** rather than passive consumption. Solve 8-12 problems total across three categories: games (Tic Tac Toe, Snake & Ladder, Chess), products (BookMyShow, Splitwise, Parking Lot, Payment System), and technical systems (Rate Limiter, Cache, Pub-Sub). Time yourself — 45 minutes for FAANG-style, 90 minutes for machine coding. The gap between watching a solution and producing one under pressure is where most preparation fails. Bridge that gap, and the LLD round becomes the most reliable hire signal in your interview loop.