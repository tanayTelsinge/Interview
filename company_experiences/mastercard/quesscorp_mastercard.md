# Quesscorp × Mastercard Interview — Frontend Heavy (Angular)

> Overall: Frontend heavy — Angular with live coding expected. (Failed here)

---

## Angular / Frontend

### 1. What is Observable and how does it work?

An **Observable** is a lazy data stream that emits values over time. It is part of the RxJS library and follows the **Observer pattern**.

- **Lazy**: nothing executes until you `.subscribe()`
- **Unicast**: each subscriber gets its own independent execution
- Can emit **multiple values** asynchronously (unlike a Promise which resolves once)
- Can be **cancelled** by calling `subscription.unsubscribe()`

**How it works internally:**
1. You create an Observable with a producer function
2. When `.subscribe()` is called, the producer runs
3. It calls `next()`, `error()`, or `complete()` on the observer

---

### 2. Write code for Observable

```typescript
import { Observable } from 'rxjs';

// Creating an observable
const obs$ = new Observable<number>(observer => {
  observer.next(1);
  observer.next(2);
  observer.next(3);
  observer.complete();
});

// Subscribing
const subscription = obs$.subscribe({
  next: val => console.log('Value:', val),
  error: err => console.error('Error:', err),
  complete: () => console.log('Done')
});

// Unsubscribing (important to prevent memory leaks)
subscription.unsubscribe();

// --- Real-world: HTTP + RxJS operators ---
import { HttpClient } from '@angular/common/http';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

// In a service
getUsers(): Observable<User[]> {
  return this.http.get<User[]>('/api/users').pipe(
    map(users => users.filter(u => u.active)),
    catchError(err => of([]))  // fallback to empty array
  );
}

// In a component — using async pipe (preferred, auto-unsubscribes)
// Template: *ngFor="let user of users$ | async"
this.users$ = this.userService.getUsers();
```

---

### 3. What is Signal and how does it work?

**Signals** (introduced in Angular 16) are a **reactive primitive** — a wrapper around a value that notifies consumers when the value changes.

Key characteristics:
- **Synchronous** and **glitch-free** reactivity
- Angular knows exactly which components/computeds depend on which signals — enables **fine-grained change detection**
- Three types: `signal()`, `computed()`, `effect()`

**How it works:**
- Reading a signal inside a `computed()` or `effect()` registers a **dependency**
- When the signal value changes, only dependent computeds/effects re-run
- No need for `ChangeDetectorRef.markForCheck()` — Angular handles it automatically

```
signal(value)  →  computed reads it  →  template reads computed  →  Angular updates only that DOM node
```

---

### 4. Write code for Signal

```typescript
import { Component, signal, computed, effect } from '@angular/core';

@Component({
  selector: 'app-counter',
  template: `
    <p>Count: {{ count() }}</p>
    <p>Double: {{ double() }}</p>
    <button (click)="increment()">+</button>
    <button (click)="decrement()">-</button>
  `
})
export class CounterComponent {
  // Writable signal
  count = signal(0);

  // Computed signal — auto-updates when count changes
  double = computed(() => this.count() * 2);

  constructor() {
    // Effect — runs whenever count changes (use for side effects)
    effect(() => {
      console.log('Count changed to:', this.count());
    });
  }

  increment() { this.count.update(c => c + 1); }
  decrement() { this.count.update(c => c - 1); }

  // Other signal mutation methods
  // this.count.set(10);          // set absolute value
  // this.count.update(c => c+1); // update based on current
}
```

---

### 5. Reactive vs Template-Driven Forms

| Feature | Reactive Forms | Template-Driven Forms |
|---|---|---|
| Setup | `ReactiveFormsModule` | `FormsModule` |
| Form definition | In component class (`FormGroup`, `FormControl`) | In HTML template (`ngModel`) |
| Data flow | Explicit, synchronous | Two-way binding, async |
| Validation | In class, easy to unit test | In template via directives |
| Dynamic fields | Easy (`FormArray`) | Complex |
| Best for | Complex / dynamic forms | Simple forms |

---

### 6. Input that only accepts characters (not numbers) — Reactive & Template Form

**Reactive Form:**

```typescript
// component.ts
import { FormBuilder, Validators } from '@angular/forms';

form = this.fb.group({
  name: ['', [Validators.pattern('^[a-zA-Z ]*$')]]
});

// Or use a custom directive to block keypress
constructor(private fb: FormBuilder) {}
```

```html
<!-- component.html -->
<form [formGroup]="form">
  <input
    formControlName="name"
    (keypress)="allowOnlyLetters($event)"
  />
  <span *ngIf="form.get('name')?.errors?.['pattern']">
    Only letters allowed
  </span>
</form>
```

```typescript
allowOnlyLetters(event: KeyboardEvent): boolean {
  return /[a-zA-Z ]/.test(event.key);
}
```

**Template-Driven Form:**

```html
<form #myForm="ngForm">
  <input
    name="name"
    ngModel
    pattern="^[a-zA-Z ]*$"
    (keypress)="allowOnlyLetters($event)"
    #nameField="ngModel"
  />
  <span *ngIf="nameField.errors?.['pattern']">Only letters allowed</span>
</form>
```

```typescript
allowOnlyLetters(event: KeyboardEvent): boolean {
  return /[a-zA-Z ]/.test(event.key);
}
```

> **Better approach:** Create a reusable `LettersOnlyDirective` that intercepts `keydown` and blocks numeric keys — works in both form types.

```typescript
@Directive({ selector: '[lettersOnly]' })
export class LettersOnlyDirective {
  @HostListener('keypress', ['$event'])
  onKeyPress(event: KeyboardEvent) {
    return /[a-zA-Z ]/.test(event.key);
  }
}
// Usage: <input lettersOnly formControlName="name">
```

---

### 7. What is forkJoin and how does it work?

`forkJoin` is an RxJS operator that takes **multiple Observables**, runs them **in parallel**, and emits a **single array** of their last values **only when all of them complete**.

- Similar to `Promise.all()`
- If **any** Observable errors, `forkJoin` errors immediately
- If **any** Observable never completes, `forkJoin` never emits

**Use case:** Making multiple independent HTTP calls simultaneously and waiting for all results.

---

### 8. Write code for forkJoin

```typescript
import { forkJoin } from 'rxjs';
import { HttpClient } from '@angular/common/http';

// In a component or service
loadDashboard() {
  forkJoin({
    users:    this.http.get('/api/users'),
    products: this.http.get('/api/products'),
    orders:   this.http.get('/api/orders')
  }).subscribe({
    next: ({ users, products, orders }) => {
      // All three completed — results available here
      this.users    = users;
      this.products = products;
      this.orders   = orders;
    },
    error: err => console.error('One of the requests failed', err)
  });
}

// Array syntax (results come back in same order)
forkJoin([
  this.http.get('/api/users'),
  this.http.get('/api/products')
]).subscribe(([users, products]) => {
  console.log(users, products);
});
```

---

### 9. WCAG — What is it and how much contrast is required?

**WCAG** = Web Content Accessibility Guidelines (published by W3C).

It defines 3 conformance levels: **A**, **AA**, **AAA**.
Most apps target **AA**.

**Contrast ratios required:**

| Text type | AA | AAA |
|---|---|---|
| Normal text (< 18pt / 14pt bold) | **4.5 : 1** | 7 : 1 |
| Large text (≥ 18pt / 14pt bold) | **3 : 1** | 4.5 : 1 |
| UI components & graphics | **3 : 1** | — |

**Memory trick:** Normal text = **4.5**, Large/UI = **3**, AAA doubles it.

Other WCAG principles — **POUR**:
- **P**erceivable
- **O**perable
- **U**nderstandable
- **R**obust

---

### 10. What is ARIA and how does it work?

**ARIA** = Accessible Rich Internet Applications (WAI-ARIA spec by W3C).

It adds **semantic meaning** to HTML elements so screen readers can understand them — especially useful for custom components (e.g., a `<div>` acting as a button).

Three categories of ARIA attributes:

| Type | Example | Purpose |
|---|---|---|
| **Roles** | `role="button"`, `role="dialog"` | What the element *is* |
| **Properties** | `aria-label="Close"`, `aria-required="true"` | Describe element |
| **States** | `aria-expanded="false"`, `aria-disabled="true"` | Current condition |

**How it works:**
- Browser exposes ARIA attributes via the **Accessibility Tree**
- Screen readers (NVDA, JAWS, VoiceOver) read the tree, not the visual DOM

**Rules:**
1. Use native HTML elements first (`<button>`, `<input>`) — they have ARIA built in
2. Add ARIA only when native semantics don't exist
3. Never change native semantics (`role="button"` on `<h1>` is wrong)
4. All interactive ARIA controls must be keyboard-operable

```html
<!-- Custom toggle button -->
<div
  role="button"
  tabindex="0"
  aria-pressed="false"
  aria-label="Toggle dark mode"
  (click)="toggle()"
  (keydown.enter)="toggle()"
>
  Dark Mode
</div>

<!-- Live region — screen reader announces changes -->
<div aria-live="polite" aria-atomic="true">
  {{ statusMessage }}
</div>
```

---

### 11. Browser extensions for accessibility testing

| Extension | Purpose |
|---|---|
| **axe DevTools** (Deque) | Most popular — finds WCAG violations, shows exact elements |
| **Lighthouse** (Chrome built-in) | Audit score with actionable recommendations |
| **WAVE** (WebAIM) | Visual overlay showing errors, alerts, ARIA |
| **Colour Contrast Analyser** | Desktop app — pick any color on screen and check ratio |
| **Screen Reader** | NVDA (Windows, free), VoiceOver (Mac built-in), JAWS (Windows, paid) |

> In interviews: mention **axe DevTools** first — it's the industry standard and integrates with Cypress/Jest for automated a11y testing.

---

## Backend — Java

### 1. Functional Interface — what is it and why not abstract class?

A **Functional Interface** is an interface with **exactly one abstract method** (SAM — Single Abstract Method). It can be used as the target of a **lambda expression** or **method reference**.

```java
@FunctionalInterface
interface Transformer<T, R> {
  R transform(T input);   // only one abstract method
}

// Lambda usage
Transformer<String, Integer> length = s -> s.length();
System.out.println(length.transform("hello")); // 5
```

Built-in examples: `Runnable`, `Callable`, `Comparator`, `Predicate<T>`, `Function<T,R>`, `Consumer<T>`, `Supplier<T>`

**Why can't we use an abstract class instead?**

| | Functional Interface | Abstract Class |
|---|---|---|
| Lambda support | Yes — lambdas compile to SAM | No — lambdas don't target classes |
| Multiple inheritance | Yes (Java allows multiple interface impl) | No (single inheritance only) |
| State | No instance fields | Can have fields |
| Overhead | Zero (lambda = invokedynamic) | Object allocation required |

The key reason: **Java lambdas only work with interfaces, not classes**. The compiler converts a lambda into an instance of a functional interface — it has no mechanism to do the same for abstract classes.

If you used an abstract class:
```java
// This does NOT compile — lambdas can't target classes
AbstractTransformer<String,Integer> t = s -> s.length(); // ❌
```

Additionally, abstract classes carry state and constructors — lambdas are stateless by design.

---

### 2. Spring Security

Spring Security provides **authentication** and **authorization** for Spring applications.

**Core concepts:**

- **Authentication** — who are you? (username/password, JWT, OAuth2)
- **Authorization** — what are you allowed to do? (roles, permissions)
- **Security Filter Chain** — all requests pass through a chain of filters before reaching your controller

**Basic configuration (Spring Boot 3 / Spring Security 6):**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())  // disable for REST APIs
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/public/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
      )
      .sessionManagement(s -> s
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // for JWT
      )
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
```

**JWT flow:**
1. User logs in → server validates credentials → returns JWT
2. Client sends JWT in `Authorization: Bearer <token>` header
3. `JwtAuthFilter` intercepts each request, validates token, sets `SecurityContext`
4. Controller executes only if authorized

**Method-level security:**
```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) { ... }

@PostAuthorize("returnObject.username == authentication.name")
public User getUser(Long id) { ... }
```
