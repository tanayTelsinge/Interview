# System Design Examples for Java Developers

## 1. E-commerce System Design

### Architecture Overview
```
[Client] → [Load Balancer] → [API Gateway]
                               ↓
[Auth Service] ← [API Gateway] → [Product Service] → [Product DB]
                               ↓
                          [Order Service] → [Order DB]
                               ↓
                        [Payment Service] → [Payment Gateway]
```

### Microservices Implementation

#### 1. Product Service
```java
@Service
public class ProductService {
    private final ProductRepository repository;
    private final CacheManager cacheManager;
    
    public Product getProduct(String id) {
        // Check cache first
        String cacheKey = "product:" + id;
        Product product = cacheManager.get(cacheKey);
        if (product != null) {
            return product;
        }
        
        // Get from DB and cache
        product = repository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        cacheManager.put(cacheKey, product);
        return product;
    }
}
```

#### 2. Order Service
```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    @Transactional
    public Order createOrder(OrderRequest request) {
        // Validate products
        validateProducts(request.getProducts());
        
        // Create order
        Order order = new Order(request);
        orderRepository.save(order);
        
        // Publish event
        kafkaTemplate.send("order-events", new OrderEvent(order));
        
        return order;
    }
}
```

### Scaling Considerations

1. **Database Scaling**
```java
@Configuration
public class DatabaseConfig {
    @Bean
    public DataSource dataSource() {
        return new ShardingDataSource(
            createShardingRule(),
            createDataSourceMap()
        );
    }
    
    private ShardingRule createShardingRule() {
        // Define sharding rules based on customer_id
    }
}
```

2. **Caching Strategy**
```java
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10));
            
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

3. **Rate Limiting**
```java
@Component
public class RateLimiter {
    private final RedisTemplate<String, String> redisTemplate;
    
    public boolean allowRequest(String userId) {
        String key = "rate:" + userId;
        Long count = redisTemplate.opsForValue()
            .increment(key);
            
        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        
        return count <= 100; // 100 requests per minute
    }
}
```

## 2. Chat Application Design

### Architecture
```
[WebSocket Client] → [WebSocket Server] → [Chat Service]
                                            ↓
[Redis PubSub] ← [Message Broker] ← [Chat Service] → [MongoDB]
```

### Implementation

#### 1. WebSocket Configuration
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler(), "/chat")
               .setAllowedOrigins("*");
    }
}
```

#### 2. Chat Handler
```java
@Component
public class ChatHandler extends TextWebSocketHandler {
    private final ChatService chatService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        ChatMessage chatMessage = parseMessage(message.getPayload());
        chatService.processMessage(chatMessage);
    }
}
```

#### 3. Message Broker
```java
@Service
public class MessageBroker {
    private final RedisTemplate<String, String> redisTemplate;
    
    public void publish(String channel, ChatMessage message) {
        redisTemplate.convertAndSend(
            channel,
            JsonUtils.toJson(message)
        );
    }
}
```

## 3. Real-time Analytics System

### Architecture
```
[Data Source] → [Kafka] → [Stream Processor] → [Cassandra]
                   ↓
              [Spark Jobs] → [Analytics DB]
```

### Implementation

#### 1. Kafka Producer
```java
@Service
public class EventProducer {
    private final KafkaTemplate<String, Event> kafkaTemplate;
    
    public void sendEvent(Event event) {
        kafkaTemplate.send(
            "analytics-events",
            event.getId(),
            event
        );
    }
}
```

#### 2. Stream Processor
```java
@Service
public class AnalyticsProcessor {
    @StreamListener(KafkaProcessor.INPUT)
    public void processEvent(Event event) {
        // Process event
        AnalyticsData data = processEventData(event);
        
        // Store processed data
        analyticsRepository.save(data);
        
        // Update real-time dashboard
        dashboardService.updateMetrics(data);
    }
}
```

## Best Practices

1. **Fault Tolerance**
   - Circuit Breaker pattern
   - Fallback mechanisms
   - Retry policies

2. **Scalability**
   - Horizontal scaling
   - Data partitioning
   - Caching strategies

3. **Monitoring**
   - Health checks
   - Metrics collection
   - Logging and tracing

4. **Security**
   - Authentication/Authorization
   - Data encryption
   - Rate limiting
