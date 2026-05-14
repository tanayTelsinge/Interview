# JUnit & Mockito Interview Cheat Sheet

---

## Your Testing Setup

| Type | Framework | DB | Scope |
|------|-----------|-----|-------|
| Unit Tests | JUnit 5 + Mockito | No DB (mocked) | Single class |
| Integration Tests | JUnit 5 + Spring | Real dev DB | Full API flow |

---

## JUnit 5 Basics

### Key Annotations

| Annotation | Description |
|------------|-------------|
| `@Test` | Marks a test method |
| `@BeforeEach` | Runs before each test |
| `@AfterEach` | Runs after each test |
| `@BeforeAll` | Runs once before all tests (static) |
| `@AfterAll` | Runs once after all tests (static) |
| `@DisplayName` | Human readable test name |
| `@Disabled` | Skip test |
| `@ParameterizedTest` | Run test with multiple inputs |

### Basic Test Structure
```java
class PaymentServiceTest {

    @BeforeEach
    void setUp() {
        // initialize before each test
    }

    @Test
    @DisplayName("Should calculate correct payment amount")
    void shouldCalculateCorrectPaymentAmount() {
        // Arrange
        Payment payment = new Payment(1000.0, 0.1);

        // Act
        double result = paymentService.calculate(payment);

        // Assert
        assertEquals(1100.0, result);
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.calculate(new Payment(-100.0, 0.1));
        });
    }
}
```

### Common Assertions
```java
// Equality
assertEquals(expected, actual);
assertNotEquals(expected, actual);

// Null checks
assertNull(result);
assertNotNull(result);

// Boolean
assertTrue(result.isValid());
assertFalse(result.isEmpty());

// Exception
assertThrows(IllegalArgumentException.class, () -> service.method());

// Collections
assertEquals(3, list.size());
assertTrue(list.contains(item));

// Multiple assertions — all run even if one fails
assertAll(
    () -> assertEquals("Tanay", user.getName()),
    () -> assertEquals("Pune", user.getCity()),
    () -> assertNotNull(user.getId())
);
```

---

## Mockito Basics

### Setup
```java
@ExtendWith(MockitoExtension.class)  // your setup
class ReminderServiceTest {

    @Mock
    private ReservationRepository reservationRepository;  // mocked

    @Mock
    private ReservationClient reservationClient;  // mocked feign client

    @InjectMocks
    private ReminderService reminderService;  // class under test — mocks injected automatically
}
```

### Key Annotations

| Annotation | Description |
|------------|-------------|
| `@Mock` | Creates a mock object |
| `@InjectMocks` | Creates real object, injects mocks into it |
| `@Spy` | Real object but can stub specific methods |
| `@Captor` | Captures arguments passed to mock |

### Stubbing — when/thenReturn
```java
// Return value when method called
when(reservationRepository.findByStatus("OVERDUE"))
    .thenReturn(List.of(reservation1, reservation2));

// Return different values on consecutive calls
when(reservationClient.getReservation(anyLong()))
    .thenReturn(reservation1)
    .thenReturn(reservation2);

// Throw exception
when(reservationClient.getReservation(999L))
    .thenThrow(new ReservationNotFoundException("Not found"));

// Return based on argument
when(reservationRepository.findById(anyLong()))
    .thenAnswer(invocation -> {
        Long id = invocation.getArgument(0);
        return Optional.of(new Reservation(id));
    });
```

### Argument Matchers
```java
// Any value
when(repo.findById(anyLong())).thenReturn(Optional.of(reservation));

// Specific value
when(repo.findById(1L)).thenReturn(Optional.of(reservation));

// Any string
when(client.getByStatus(anyString())).thenReturn(list);

// Collection matchers
when(repo.findAllById(anyList())).thenReturn(reservations);

// Custom matcher
when(repo.findAll(any(Specification.class))).thenReturn(reservations);
```

### Verify — check interactions
```java
// Verify method was called
verify(reservationRepository).findByStatus("OVERDUE");

// Verify called exactly N times
verify(reservationClient, times(3)).getReservation(anyLong());

// Verify never called
verify(notificationService, never()).sendReminder(any());

// Verify no more interactions
verifyNoMoreInteractions(reservationRepository);

// Verify order of calls
InOrder inOrder = inOrder(reservationRepository, notificationService);
inOrder.verify(reservationRepository).findByStatus("OVERDUE");
inOrder.verify(notificationService).sendReminder(any());
```

### ArgumentCaptor — capture what was passed
```java
@Captor
ArgumentCaptor<Reminder> reminderCaptor;

// Capture argument passed to save
verify(reminderRepository).save(reminderCaptor.capture());
Reminder savedReminder = reminderCaptor.getValue();

// Assert on captured value
assertEquals("OVERDUE", savedReminder.getStatus());
assertEquals(1000.0, savedReminder.getAmount());
```

---

## Full Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationClient reservationClient;

    @Mock
    private ReminderRepository reminderRepository;

    @InjectMocks
    private ReminderService reminderService;

    @Captor
    ArgumentCaptor<Reminder> reminderCaptor;

    @Test
    @DisplayName("Should generate reminder for overdue reservation")
    void shouldGenerateReminderForOverdueReservation() {
        // Arrange
        Reservation reservation = new Reservation(1L, LocalDate.now().minusDays(5), "CONFIRMED");
        List<Reservation> reservations = List.of(reservation);
        
        when(reservationRepository.findAll(any(Specification.class)))
            .thenReturn(reservations);
        when(reservationClient.getReservations(anySet()))
            .thenReturn(reservations);

        // Act
        reminderService.generateReminders();

        // Assert
        verify(reminderRepository).save(reminderCaptor.capture());
        Reminder savedReminder = reminderCaptor.getValue();
        
        assertAll(
            () -> assertEquals(1L, savedReminder.getReservationId()),
            () -> assertEquals("PENDING", savedReminder.getStatus()),
            () -> assertNotNull(savedReminder.getCreatedAt())
        );
    }

    @Test
    @DisplayName("Should not generate reminder when no overdue reservations")
    void shouldNotGenerateReminderWhenNoOverdueReservations() {
        // Arrange
        when(reservationRepository.findAll(any(Specification.class)))
            .thenReturn(Collections.emptyList());

        // Act
        reminderService.generateReminders();

        // Assert
        verify(reminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when reservation client fails")
    void shouldThrowExceptionWhenReservationClientFails() {
        // Arrange
        when(reservationRepository.findAll(any(Specification.class)))
            .thenReturn(List.of(new Reservation(1L)));
        when(reservationClient.getReservations(anySet()))
            .thenThrow(new RuntimeException("Service unavailable"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> reminderService.generateReminders());
        verify(reminderRepository, never()).save(any());
    }
}
```

---

## Integration Tests (Your Setup)

### Full API Flow with Real Dev DB
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:oracle:thin:@dev-db:1521:XE"  // real dev DB
})
class ReminderControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ReminderRepository reminderRepository;

    @Test
    void shouldGenerateRemindersSuccessfully() {
        // Act
        ResponseEntity<String> response = restTemplate
            .postForEntity("/api/reminders/generate", null, String.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(reminderRepository.count() > 0);
    }
}
```

---

## Common Interview Questions

### Q1: What is the difference between @Mock and @Spy?

| | @Mock | @Spy |
|--|-------|------|
| Object | Fake object | Real object |
| Methods | All stubbed (return null/0 by default) | Real methods called unless stubbed |
| Use case | External dependencies | Partial mocking |

```java
@Mock
ReservationRepository repo;  // fake — no real DB calls

@Spy
ReminderCalculator calculator = new ReminderCalculator();  // real object
doReturn(100.0).when(calculator).calculate(any());  // stub specific method only
```

---

### Q2: What is the difference between @Mock and @InjectMocks?

> "`@Mock` creates a fake dependency. `@InjectMocks` creates the real class under test and automatically injects all `@Mock` objects into it via constructor, setter, or field injection."

---

### Q3: How do you test void methods?

```java
// verify interaction instead of return value
doNothing().when(notificationService).sendReminder(any());

service.process();

verify(notificationService, times(1)).sendReminder(any(Reminder.class));
```

---

### Q4: What is the difference between unit and integration tests?

| | Unit Test | Integration Test |
|--|-----------|-----------------|
| Scope | Single class | Multiple layers |
| Dependencies | Mocked | Real |
| Speed | Fast (ms) | Slow (seconds) |
| DB | No | Yes (your setup: dev DB) |
| Isolation | High | Low |
| Purpose | Logic correctness | System correctness |

---

### Q5: Why do we mock feign clients in unit tests?

> "Feign clients make real HTTP calls to external services. In unit tests we don't want network dependency — it's slow, unreliable and the test should focus on our business logic, not the external service. We mock the feign client to return controlled responses and verify our code handles them correctly."

---

## Testing Best Practices

| Practice | Why |
|----------|-----|
| One assertion per test ideally | Clear failure reason |
| Descriptive test names | `shouldGenerateReminderWhenOverdue` not `test1` |
| Arrange-Act-Assert pattern | Readable structure |
| Test edge cases | null, empty, boundary values |
| Don't test framework code | Don't test Spring, JPA — test your logic |
| Mock external dependencies | Feign clients, external APIs |
| Never depend on test order | Each test should be independent |

---

## Interview Answer — Your Testing Experience

> "I wrote unit tests using JUnit 5 with Mockito extension. For each service class I mocked external dependencies — feign clients and repositories — using `@Mock` and injected them with `@InjectMocks`. I used `when/thenReturn` for stubbing, `verify` to assert interactions, and `ArgumentCaptor` to capture and assert on saved entities.
>
> For integration tests I tested the full API flow against our dev DB — covering the complete stack from controller through service to DB. This validated that the layers worked correctly together, especially for complex queries like our Specification-based reminder generation."