- Use STAR method to answer behavioral questions.

S: Our finance module had a critical payment file import feature that was consistently failing in production. Files contained thousands of records, causing the process to run over 30 minutes and hit DB connection timeouts, directly blocking client settlements.
T: I took ownership of diagnosing and resolving the issue end to end.
A: I started by profiling the API and traced the failures to three root causes:

Feign client calls were sequential — I parallelized them using CompletableFuture, reducing wait time significantly
Identified N+1 query problem — replaced individual queries with JOINs, reducing DB roundtrips
Found incorrect transaction propagation — Propagation.NEW was creating a new transaction per record, exhausting the connection pool. I restructured the transaction boundaries to fix this.

R: Processing time dropped from 30+ minutes to 10 minutes with zero failures post-deployment. This directly unblocked client settlement operations and restored confidence in the feature.


Technical Followup:
Follow-up 1: You mentioned CompletableFuture — how did you implement it? What thread pool did you use?

```java
List<Reservation> reservations = 
reservationIds.stream()
    .map(id -> reservationClient.getReservation(id))
    .collect(Collectors.toList());

// Refactored with CompletableFuture

List<CompletableFuture<Reservation>> futures = 
reservationIds.stream()
    .map(id -> CompletableFuture.supplyAsync(() ->
        reservationClient.getReservation(id), reservationExecutor)).collect(Collectors.toList());

List<Reservation> reservations = futures.stream()
    .map(CompletableFuture::join)
    .collect(Collectors.toList());
```

- What was N + 1 queries issue and how did you identify it?
- For each mutation, 2 mutation details, 
so for 10,000 mutations, we had 20,000 queries. 
- Identified by checking logs.
- Fixed it using JOIN FETCH in the repository layer, so we fetch mutation and details in one query.

```sql
-- Query 1: fetch mutations
SELECT * FROM mutation;

-- Hibernate sees mutationDetails is LAZY, fires per mutation
SELECT * FROM mutation_detail WHERE mutation_id = 1;
SELECT * FROM mutation_detail WHERE mutation_id = 2;

-- Refactored query with JOIN FETCH
@Query("SELECT DISTINCT m FROM Mutation m JOIN FETCH m.mutationDetails")

SELECT m.*, md.* FROM mutation m
JOIN mutation_detail md ON m.id = md.mutation_id;
```

- What was the transaction propagation issue and how did you fix it?
-10,000 mutations × 2 mutation details each = 20,000 saveDetails() calls
Each call had Propagation.REQUIRES_NEW
= 20,000 new transactions = 20,000 new DB connections requested

```java
// MutationService
@Transactional
public void processMutations(List<Mutation> mutations) {
    for(Mutation mutation : mutations) {
        mutationRepo.save(mutation);
        mutationDetailService.saveDetails(mutation.getDetails()); // calls separate service
    }
}

// MutationDetailService
@Transactional(propagation = Propagation.NEW)
public void saveDetails(List<MutationDetail> details) {
    mutationDetailRepo.saveAll(details);
}
```
Problem with Propagation.REQUIRES_NEW:

For each mutation, a brand new transaction is created
New transaction = new DB connection from pool
10,000 mutations = 10,000 new connections requested
Connection pool exhausted → failures

- 4th issue - no flushing OOM because of huge session - hibernate 1st level cache. Fixed by flushing and clearing session every 100 records.

```java
save mutation 1    → Hibernate holds in memory (dirty)
save mutation 2    → Hibernate holds in memory (dirty)
save mutation 3    → Hibernate holds in memory (dirty)
...
save mutation 10,000 → all 10,000 pending in session cache
→ flush at commit  → writes all at once → memory spike → OOM risk
// Refactored to flush and clear session every 100 records
for (int i = 0; i < mutations.size(); i++) {
    mutationRepo.save(mutations.get(i));
    if (i % 100 == 0) {
        mutationRepo.flush(); // write to DB
        mutationRepo.clear(); // clear session cache
    }
}
```
STAR for reminder migration

```java
//used specification for dynamic query building based on criteria fetched from DB
// Fetch criteria dynamically from DB
ReminderCriteria criteria = reminderCriteriaRepo.findByTermset(termset);
List<String> allowedStatuses = criteria.getStatuses();
DateRange dateRange = criteria.getDateRange();

// Build spec dynamically
Specification<Reservation> spec = Specification.where(null);

if(!allowedStatuses.isEmpty())
    spec = spec.and(new ReservationStatusSpec(allowedStatuses));

if(dateRange != null)
    spec = spec.and(new IssueDateSpec(dateRange));

spec = spec.and(new ReminderCriteriaSpec(criteria));

List<Reservation> reservations = reservationRepo.findAll(spec);
```

```java
Reminders job runs at midnight IST (00:00 April 8)
dueDate in DB = April 7 (UTC)
LocalDate.now() in IST = April 8

→ April 8 vs April 7
→ comparison off by 1 day
→ reminder either missed or triggered early
```

- Solution: Convert dueDate to IST before comparison
```java
// Convert dueDate to IST before comparison
ZoneId istZone = ZoneId.of("Asia/Kolkata");
for (Reservation reservation : reservations) {
    ZonedDateTime dueDateUtc = reservation.getDueDate().atStartOfDay(ZoneOffset.UTC);
    ZonedDateTime dueDateIst = dueDateUtc.withZoneSameInstant(istZone);
    
    if (dueDateIst.toLocalDate().equals(LocalDate.now(istZone))) {
        // Trigger reminder
    }
}
```

- IF senior stakeholder pushes for feature u cannot deliver in time, how do you handle it?
"First I'd make sure I fully understand the stakeholder's intent — sometimes what sounds technically wrong is actually solving a valid business problem we haven't fully understood. I'd have a direct conversation to understand their goal, not just their solution.
Then I'd bring data — not opinions. I'd prepare:

Technical risks of their approach (performance, security, maintainability)
Alternative approach that achieves same business goal
Cost comparison — short term vs long term

I'd present this as 'here's how we achieve your goal more effectively' — not 'you're wrong.'
If they still push back, I'd escalate with facts — involve my manager or architect, framing it as a risk discussion, not a conflict.
If overruled despite valid concerns, I'd document the decision and risks formally, then execute with full commitment."
