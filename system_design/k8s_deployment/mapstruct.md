-- Active: 1779127130444@@127.0.0.1@5432@employeedb
# MapStruct & the Mapper Layer

## The Flow You're Using

```
HTTP Request
     ↓
 Controller
     ↓  mapper.toEntity(requestDto)
  Service       ← operates only on domain/entity objects
     ↓  mapper.toResponseDto(entity)
 Controller
     ↓
HTTP Response
```

This is the **DTO pattern** — the mapper acts as a translation layer between
the outside world (DTOs) and the internal domain (entities).

---

## What is MapStruct?

MapStruct is an **annotation processor** that generates type-safe mapper
implementation classes at **compile time** (not reflection at runtime).

You write an interface, MapStruct generates the implementation.

```java
// You write this
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponseDto(User user);
    User toEntity(CreateUserRequest request);
}

// MapStruct generates this (in target/generated-sources)
@Component
public class UserMapperImpl implements UserMapper {
    @Override
    public UserResponse toResponseDto(User user) {
        if (user == null) return null;
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        return response;
    }
    // ...
}
```

---

## Why Use It?

### Without MapStruct (manual mapping)
```java
// Tedious, error-prone, grows with every new field
public UserResponse toResponse(User user) {
    UserResponse dto = new UserResponse();
    dto.setId(user.getId());
    dto.setName(user.getName());
    dto.setEmail(user.getEmail());
    dto.setPhone(user.getPhone());
    // forgot to add createdAt... silent bug
    return dto;
}
```

### With MapStruct
```java
UserResponse toResponseDto(User user);  // one line, all fields mapped automatically
```

| Concern | Manual | MapStruct |
|---------|--------|-----------|
| Boilerplate | High | None |
| Type safety | Compile time | Compile time |
| Performance | Same | Same (no reflection) |
| Null safety | Manual | Built-in |
| Missed fields | Silent bug | Compiler warning |

---

## Why Use DTOs at All? (The Real Reason for This Pattern)

### 1. Separate API contract from DB schema
Your `User` entity has `passwordHash`, `internalFlag`, `auditTrail`.
Your `UserResponse` should expose none of that.

```java
@Entity
public class User {
    private Long id;
    private String name;
    private String passwordHash;  // NEVER expose this
    private String internalFlag;  // NEVER expose this
    private LocalDateTime createdAt;
}

public class UserResponse {
    private Long id;
    private String name;          // only what the client needs
}
```

### 2. Input validation lives in the DTO, not the entity
```java
public class CreateUserRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @Email
    private String email;
}
```

### 3. DB schema changes don't break your API
Rename a column in `User` → update the mapper → API contract unchanged.

### 4. Prevent mass assignment attacks
Without DTOs, a malicious client could POST `{"role": "ADMIN"}` and if you
bind directly to the entity, that field gets set. DTOs expose only what you
explicitly allow.

---

## Full Example with Your Flow

### Layers

```
CreateUserRequest (DTO - input)
        ↓ mapper.toEntity()
      User (entity - domain)
        ↓ service processes
      User (entity - domain)
        ↓ mapper.toResponseDto()
   UserResponse (DTO - output)
```

### Controller

```java
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userMapper.toEntity(request);          // ← Request DTO → Entity
        User saved = userService.create(user);
        UserResponse response = userMapper.toResponseDto(saved); // ← Entity → Response DTO
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return userMapper.toResponseDto(user);             // ← Entity → Response DTO
    }
}
```

### Service (knows nothing about DTOs)

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User create(User user) {
        return userRepository.save(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
```

### Mapper

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    // field names match → auto-mapped
    UserResponse toResponseDto(User user);

    // ignore fields the client shouldn't set
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    User toEntity(CreateUserRequest request);
}
```

### DTOs & Entity

```java
// Input DTO
public class CreateUserRequest {
    @NotBlank private String name;
    @Email    private String email;
    @NotBlank private String password;  // raw password, service will hash it
}

// Output DTO
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}

// Entity
@Entity
public class User {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String email;
    private String passwordHash;
    private LocalDateTime createdAt;
}
```

---

## Key Annotations

### `@Mapping` — field name mismatch or custom mapping

```java
@Mapping(source = "firstName", target = "name")
@Mapping(source = "address.city", target = "city")   // nested field
UserResponse toResponseDto(User user);
```

### `@Mapping(ignore = true)` — skip a field

```java
@Mapping(target = "id", ignore = true)
User toEntity(CreateUserRequest request);
```

### `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`
### — partial update (PATCH)

```java
// Only update fields that are non-null in the request
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updateEntityFromDto(UpdateUserRequest request, @MappingTarget User user);
```

```java
// In service — PATCH flow
@Transactional
public User partialUpdate(Long id, UpdateUserRequest request) {
    User user = userRepository.findById(id).orElseThrow();
    userMapper.updateEntityFromDto(request, user); // only non-null fields applied
    return userRepository.save(user);
}
```

### `@AfterMapping` — post-processing

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "passwordHash", ignore = true)
    User toEntity(CreateUserRequest request);

    @AfterMapping
    default void hashPassword(CreateUserRequest request, @MappingTarget User user) {
        user.setPasswordHash(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
    }
}
```

### Mapping lists

```java
List<UserResponse> toResponseDtoList(List<User> users);
// MapStruct generates the loop automatically
```

---

## Maven / Gradle Setup

```xml
<!-- Maven -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>
```

> With Lombok, put `mapstruct-processor` AFTER `lombok` in the annotation processors list,
> otherwise MapStruct can't see Lombok-generated getters/setters.

---

## Where Should the Mapper Be Called?

| Option | Verdict |
|--------|---------|
| **Controller calls mapper** (your flow) | Preferred — service stays pure domain logic |
| Service calls mapper | Avoid — service shouldn't know about DTOs |
| Mapper inside repository | Wrong layer entirely |

Your flow is the standard clean architecture approach.
The service layer only deals with domain objects, making it independently testable.

---

## Common Interview Questions

**Q: Why not use ModelMapper instead?**
MapStruct generates code at compile time (zero reflection overhead, compile-time errors).
ModelMapper uses reflection at runtime (slower, runtime failures).

**Q: What happens if you add a new field to the entity?**
MapStruct will map it automatically if the DTO has a field with the same name.
If it doesn't, you get an "unmapped target property" warning — configurable to error.

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper { ... }
```

**Q: How do you handle bidirectional relationships (avoid infinite loops)?**
Use `@Mapping(ignore = true)` on the back-reference side.

```java
@Mapping(target = "orders.user", ignore = true)
UserResponse toResponseDto(User user);
```

**Q: Can mappers use Spring beans (e.g. a service)?**
Yes, with `uses` attribute:

```java
@Mapper(componentModel = "spring", uses = {AddressMapper.class})
public interface UserMapper { ... }
```
