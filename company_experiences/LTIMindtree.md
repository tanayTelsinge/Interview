# LTIMindtree Interview Questions & Answers

---

## Angular

### Dynamic Component Loading
Load a component at runtime without declaring it in the template.

```typescript
@Component({ template: `<ng-container #container></ng-container>` })
export class HostComponent {
  @ViewChild('container', { read: ViewContainerRef }) container: ViewContainerRef;

  loadComponent() {
    this.container.clear();
    const ref = this.container.createComponent(MyDynamicComponent);
    ref.instance.title = 'Loaded dynamically';  // pass inputs
  }
}
```
Use cases: modal dialogs, tab panels, feature-flag driven UI, lazy-loaded widgets.

---

### Custom Directive

```typescript
@Directive({ selector: '[appHighlight]' })
export class HighlightDirective {
  @Input() appHighlight = 'yellow';

  constructor(private el: ElementRef, private renderer: Renderer2) {}

  @HostListener('mouseenter') onEnter() {
    this.renderer.setStyle(this.el.nativeElement, 'background', this.appHighlight);
  }
  @HostListener('mouseleave') onLeave() {
    this.renderer.removeStyle(this.el.nativeElement, 'background');
  }
}
```
```html
<p appHighlight="lightblue">Hover me</p>
```
- Use `Renderer2` not direct DOM manipulation (works with SSR, web workers)
- `@HostListener` = listen to host element events
- `@HostBinding` = bind to host element properties

---

### Pure vs Impure Pipes

| | Pure Pipe | Impure Pipe |
|---|---|---|
| **When it runs** | Only when input reference changes | Every change detection cycle |
| **Performance** | Fast — cached | Slow — runs constantly |
| **Default** | Yes (`pure: true`) | `pure: false` |
| **Use for** | Stable data (formatting, sorting) | Async data, filtering mutable arrays |

```typescript
@Pipe({ name: 'filter', pure: false })  // impure — reruns when array mutates
export class FilterPipe implements PipeTransform {
  transform(items: any[], search: string) {
    return items.filter(i => i.name.includes(search));
  }
}
```
**Gotcha:** Filtering/sorting arrays with a pure pipe won't update because array reference doesn't change on push/splice. Use impure pipe OR return a new array.

---

## Java / Spring

### Reentrant Lock
A `ReentrantLock` allows the **same thread to acquire the lock multiple times** without deadlocking itself.

```java
ReentrantLock lock = new ReentrantLock();

void outerMethod() {
  lock.lock();
  try {
    innerMethod();  // same thread acquires lock again — works, hold count = 2
  } finally {
    lock.unlock();  // hold count = 1
  }
}

void innerMethod() {
  lock.lock();      // reentrant — same thread, no deadlock
  try { /* ... */ }
  finally { lock.unlock(); }  // hold count back to 0 → released
}
```

**vs `synchronized`:**
| | `synchronized` | `ReentrantLock` |
|---|---|---|
| Reentrant | Yes | Yes |
| Try-lock (non-blocking) | No | `tryLock()` |
| Fairness | No | `new ReentrantLock(true)` |
| Interruptible | No | `lockInterruptibly()` |
| Condition variables | No | `newCondition()` |

Use `ReentrantLock` when you need timeout, fairness, or multiple conditions.

---

### How to Optimize a Spring Application

**1. DB layer (biggest impact)**
- Use `@Transactional(readOnly = true)` for read-only queries
- Avoid N+1 with `JOIN FETCH` or `@EntityGraph`
- Use projections/DTOs instead of fetching full entities
- Enable connection pooling (HikariCP — default in Spring Boot)

**2. Caching**
```java
@Cacheable("users")        // cache result
@CacheEvict("users")       // invalidate on update
```
Use Redis for distributed cache.

**3. Async processing**
```java
@Async
public CompletableFuture<User> fetchUser(Long id) { ... }
```

**4. Connection & thread pool tuning**
- `spring.datasource.hikari.maximum-pool-size=20`
- `server.tomcat.threads.max=200`

**5. Lazy loading beans** — `spring.main.lazy-initialization=true` (faster startup)

**6. Actuator + profiling** — identify slow endpoints with `/actuator/metrics`

---

### How to Optimize DB Queries

1. **Add indexes** on frequently filtered/joined columns
2. **Avoid SELECT \*** — select only needed columns
3. **Use EXPLAIN/EXPLAIN ANALYZE** — check if index is being used
4. **Fix N+1** — one query per parent + N queries for children → use JOIN or batch fetch
5. **Pagination** — `LIMIT/OFFSET` or keyset pagination for large data
6. **Avoid functions on indexed columns** — `WHERE YEAR(created_at) = 2024` kills index
7. **Use query cache** — Redis/Hibernate second-level cache for repeated reads
8. **Connection pooling** — don't open new connections per request (HikariCP)
9. **Batch inserts/updates** — `saveAll()` with `spring.jpa.properties.hibernate.jdbc.batch_size=50`

---

### What is AutoConfiguration

Spring Boot's mechanism to **automatically configure beans based on what's on the classpath**, without manual XML or `@Bean` declarations.

```
@SpringBootApplication
  └── @EnableAutoConfiguration
        └── scans META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
              └── conditionally loads config classes
```

**How it works:**
```java
@ConditionalOnClass(DataSource.class)        // only if DataSource is on classpath
@ConditionalOnMissingBean(DataSource.class)  // only if user hasn't defined their own
public class DataSourceAutoConfiguration { ... }
```

**Example:** Add `spring-boot-starter-web` → Tomcat + DispatcherServlet auto-configured. Add `spring-boot-starter-data-jpa` → EntityManagerFactory + HikariCP auto-configured.

**Override it:** Define your own `@Bean` — `@ConditionalOnMissingBean` ensures yours wins.

**Debug:** `--debug` flag prints auto-configuration report (what matched, what didn't).
