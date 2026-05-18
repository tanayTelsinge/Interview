# Spring AI — Java Developer Guide

---

## What is Spring AI?

Spring AI is a Spring Framework project that provides a standard, portable API for integrating AI/LLM capabilities into Java applications. It abstracts over multiple AI providers (OpenAI, Anthropic, Google Vertex, Azure OpenAI, Ollama, etc.) so you can switch models without rewriting application code — the same way Spring Data abstracts over databases.

**Current version**: 1.0.x (GA as of 2025)
**Minimum Java**: 17
**Minimum Spring Boot**: 3.2

---

## Why Spring AI Over Plain REST Calls?

| Concern | Plain HTTP | Spring AI |
|---|---|---|
| Provider switch | Rewrite client code | Change config + dependency |
| Prompt templating | Manual string concat | `PromptTemplate` with variables |
| Chat memory | Roll your own | `ChatMemory` / `MessageWindowChatMemory` |
| RAG pipeline | Build from scratch | `QuestionAnswerAdvisor` + `VectorStore` |
| Function calling / Tool use | Parse JSON manually | `@Tool` annotation |
| Streaming | SSE boilerplate | `Flux<ChatResponse>` out of the box |

---

## Core Concepts

### 1. ChatClient — Primary Entry Point
```java
@Service
public class MyAIService {

    private final ChatClient chatClient;

    public MyAIService(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("You are a helpful Java expert.")
            .build();
    }

    public String ask(String question) {
        return chatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
```
`ChatClient.Builder` is auto-configured by Spring Boot — inject and build.

---

### 2. ChatModel — Lower-Level API
`ChatModel` is the interface that `ChatClient` delegates to.
Direct use when you need full control over `Prompt` and `ChatResponse` objects:
```java
@Autowired
private ChatModel chatModel;

public String callDirectly(String userText) {
    Prompt prompt = new Prompt(new UserMessage(userText));
    ChatResponse response = chatModel.call(prompt);
    return response.getResult().getOutput().getContent();
}
```

---

### 3. Messages and Roles
```java
List<Message> messages = List.of(
    new SystemMessage("You are a concise assistant."),
    new UserMessage("Explain Spring AI in one sentence."),
    new AssistantMessage("Spring AI is...")   // inject prior assistant turn for multi-turn
);
Prompt prompt = new Prompt(messages);
```

| Role | Class | Use |
|---|---|---|
| system | `SystemMessage` | Persona, rules, context |
| user | `UserMessage` | The actual question |
| assistant | `AssistantMessage` | Prior model response (multi-turn) |
| tool | `ToolResponseMessage` | Tool/function call result |

---

### 4. PromptTemplate — Parameterized Prompts
```java
PromptTemplate template = new PromptTemplate("""
    You are a {role}.
    Answer the following question in {language}: {question}
    """);

Prompt prompt = template.create(Map.of(
    "role", "Java architect",
    "language", "English",
    "question", userQuestion
));

String answer = chatModel.call(prompt)
    .getResult().getOutput().getContent();
```

---

### 5. Structured Output — Map Response to Java Object
```java
record MovieRecommendation(String title, String genre, int year) {}

MovieRecommendation result = chatClient.prompt()
    .user("Recommend a sci-fi movie from the 90s")
    .call()
    .entity(MovieRecommendation.class);   // uses BeanOutputConverter internally
```
Spring AI generates the output format instruction automatically and deserializes the JSON response.

For lists:
```java
List<MovieRecommendation> results = chatClient.prompt()
    .user("Give me 5 sci-fi movies")
    .call()
    .entity(new ParameterizedTypeReference<List<MovieRecommendation>>() {});
```

---

### 6. Streaming Responses (Reactive)
```java
Flux<String> stream = chatClient.prompt()
    .user("Write a poem about Java")
    .stream()
    .content();

// In a Spring MVC controller (SSE):
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamResponse(@RequestParam String question) {
    return chatClient.prompt()
        .user(question)
        .stream()
        .content();
}
```

---

### 7. Chat Memory — Multi-Turn Conversations
```java
@Bean
public ChatClient chatClient(ChatClient.Builder builder) {
    return builder
        .defaultAdvisors(
            new MessageChatMemoryAdvisor(new InMemoryChatMemory())
        )
        .build();
}

// Each call with the same conversationId retains history
String reply = chatClient.prompt()
    .user(userMessage)
    .advisors(a -> a.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId))
    .call()
    .content();
```

`InMemoryChatMemory` — for dev/testing.
`CassandraChatMemory` / `JdbcChatMemory` — for production persistence.

---

### 8. Tool Calling (Function Calling)
Let the model invoke Java methods when it decides it needs real data.

```java
// Step 1: Define a tool
@Component
public class WeatherService {

    @Tool(description = "Get current weather for a given city")
    public String getWeather(String city) {
        // call a real weather API
        return "Sunny, 32°C in " + city;
    }
}

// Step 2: Register with ChatClient
@Service
public class AssistantService {

    private final ChatClient chatClient;

    public AssistantService(ChatClient.Builder builder, WeatherService weatherService) {
        this.chatClient = builder
            .defaultTools(weatherService)
            .build();
    }

    public String ask(String question) {
        return chatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
```
When the user asks _"What's the weather in Mumbai?"_, the model calls `getWeather("Mumbai")` automatically and incorporates the result.

---

### 9. RAG — Retrieval-Augmented Generation

RAG = inject your own documents into context so the model answers from your data, not just its training.

**Pipeline: Ingest Documents → Embed → Store → Retrieve → Prompt**

#### Step 1: Ingest and store documents
```java
@Bean
public ApplicationRunner ingestDocuments(
        VectorStore vectorStore,
        ResourceLoader resourceLoader) {
    return args -> {
        Resource resource = resourceLoader.getResource("classpath:docs/company-policy.pdf");
        List<Document> docs = new TokenTextSplitter()
            .apply(new PdfDocumentReader(resource).get());
        vectorStore.add(docs);
    };
}
```

#### Step 2: Query with RAG advisor
```java
@Service
public class RagService {

    private final ChatClient chatClient;

    public RagService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
            .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
            .build();
    }

    public String query(String question) {
        return chatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
```
`QuestionAnswerAdvisor` embeds the question, retrieves top-k similar chunks from the vector store, and injects them as context before calling the model.

#### Supported Vector Stores
`PgVectorStore` (PostgreSQL + pgvector), `RedisVectorStore`, `ChromaVectorStore`, `WeaviateVectorStore`, `PineconeVectorStore`, `MilvusVectorStore`, `SimpleVectorStore` (in-memory, dev only)

---

## Configuration (application.yml)

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
          max-tokens: 2048

    # For Anthropic Claude
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-opus-4-6
          temperature: 0.5

    # For local models via Ollama
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.2
```

Switch providers by changing the dependency + YAML config — application code stays the same.

---

## Maven Dependencies

```xml
<!-- BOM — manages all Spring AI versions -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- OpenAI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Anthropic Claude -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>

<!-- Ollama (local models) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>

<!-- PgVector store -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
</dependency>
```

---

## Advisors — Cross-Cutting Concerns for AI Calls

Advisors are Spring AI's equivalent of AOP interceptors — they wrap the ChatClient call pipeline.

| Advisor | Purpose |
|---|---|
| `MessageChatMemoryAdvisor` | Injects conversation history into every prompt |
| `QuestionAnswerAdvisor` | RAG — retrieves relevant docs and injects as context |
| `SafeGuardAdvisor` | Blocks sensitive keywords from being sent to the model |
| `SimpleLoggerAdvisor` | Logs prompt + response (dev/debug) |

Custom advisor:
```java
public class MetricsAdvisor implements CallAroundAdvisor {

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        long start = System.currentTimeMillis();
        AdvisedResponse response = chain.nextAroundCall(request);
        long duration = System.currentTimeMillis() - start;
        // record metrics
        return response;
    }

    @Override
    public String getName() { return "MetricsAdvisor"; }

    @Override
    public int getOrder() { return 0; }
}
```

---

## Image Generation
```java
@Autowired
private ImageModel imageModel;

public String generateImage(String description) {
    ImageResponse response = imageModel.call(
        new ImagePrompt(description,
            OpenAiImageOptions.builder()
                .withModel("dall-e-3")
                .withSize("1024x1024")
                .build())
    );
    return response.getResult().getOutput().getUrl();
}
```

---

## Embeddings
```java
@Autowired
private EmbeddingModel embeddingModel;

public float[] embed(String text) {
    EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
    return response.getResults().get(0).getOutput();
}
```
Used for semantic search, similarity scoring, and populating vector stores manually.

---

## Interview Questions

**Q. How does Spring AI differ from calling the OpenAI REST API directly?**
Spring AI provides provider-agnostic abstractions (`ChatModel`, `ChatClient`), prompt templating, structured output conversion, built-in RAG support, chat memory, and tool calling — all with Spring Boot auto-configuration. Switching from OpenAI to Claude requires changing one dependency and one YAML key.

**Q. What is the difference between `ChatModel` and `ChatClient`?**
`ChatModel` is the low-level interface that maps directly to the provider's API (takes `Prompt`, returns `ChatResponse`). `ChatClient` is a fluent, higher-level API built on top of `ChatModel` — it supports advisors, tool injection, memory, structured output, and streaming with a cleaner builder DSL.

**Q. How does RAG work in Spring AI?**
Documents are split into chunks, embedded into vectors, and stored in a `VectorStore`. At query time, `QuestionAnswerAdvisor` embeds the user's question, performs a similarity search in the vector store, retrieves the top-k relevant chunks, and injects them as context into the prompt before calling the model.

**Q. How does tool calling work?**
Annotate a method with `@Tool(description = "...")`. Register the containing bean with `ChatClient` via `.defaultTools(bean)`. Spring AI sends the tool schemas to the model. When the model decides to call a tool, Spring AI invokes the Java method automatically and feeds the result back to the model for final answer generation.

**Q. How do you handle multi-turn conversations?**
Use `MessageChatMemoryAdvisor` with a `ChatMemory` implementation (e.g., `InMemoryChatMemory`). Pass a `conversationId` as an advisor parameter — Spring AI automatically injects prior turns into each prompt for that session.

**Q. What are Advisors and when would you write a custom one?**
Advisors are interceptors around the ChatClient call pipeline (similar to Spring AOP). Built-in advisors handle memory, RAG, logging. Write a custom advisor when you need cross-cutting behavior like metrics recording, PII redaction, rate limiting, or request/response transformation.

---

## Key Classes / Interfaces Quick Reference

| Class / Interface | Package | Purpose |
|---|---|---|
| `ChatClient` | `org.springframework.ai.chat.client` | Fluent API for chat interactions |
| `ChatModel` | `org.springframework.ai.chat.model` | Provider-level interface |
| `ChatResponse` | `org.springframework.ai.chat.model` | Full response including metadata |
| `Prompt` | `org.springframework.ai.chat.prompt` | Wraps list of messages + options |
| `PromptTemplate` | `org.springframework.ai.chat.prompt` | Parameterized prompt builder |
| `VectorStore` | `org.springframework.ai.vectorstore` | Abstraction over vector databases |
| `EmbeddingModel` | `org.springframework.ai.embedding` | Text → vector conversion |
| `ImageModel` | `org.springframework.ai.image` | Image generation |
| `Document` | `org.springframework.ai.document` | Chunk of text with metadata |
| `QuestionAnswerAdvisor` | `org.springframework.ai.chat.client.advisor` | RAG advisor |
| `MessageChatMemoryAdvisor` | `org.springframework.ai.chat.client.advisor` | Conversation memory |
