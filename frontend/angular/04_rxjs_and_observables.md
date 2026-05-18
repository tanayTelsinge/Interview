# RxJS & Observables

## Observable vs Promise

| | Promise | Observable |
|---|---|---|
| Values | One | Many (stream) |
| Lazy? | No — executes immediately | Yes — only runs when subscribed |
| Cancellable? | No | Yes — unsubscribe() |
| Operators | `.then()`, `.catch()` | 100+ operators (map, filter, switchMap…) |
| Use in Angular | One-off HTTP calls (fine) | Streams, real-time data, complex async flows |

```typescript
// Promise — one value, eager
const p = fetch('/api/users');         // starts immediately

// Observable — stream, lazy
const o$ = this.http.get('/api/users');  // nothing happens yet
o$.subscribe(data => ...);              // now it runs
```

> Java analogy: Observable ≈ `CompletableFuture` + `Stream` combined. Lazy pipeline that can emit multiple values.

---

## Creating Observables

```typescript
// From HTTP — most common in Angular
this.http.get<User[]>('/api/users')

// From events
fromEvent(document, 'click')

// Timer
interval(1000)         // emits 0, 1, 2... every second
timer(3000, 1000)      // starts after 3s, then every 1s

// Static values
of(1, 2, 3)            // emits 1, 2, 3 and completes
from([1, 2, 3])        // same but from array
```

---

## Subscribing

```typescript
this.userService.getUsers().subscribe({
  next: (users) => this.users = users,
  error: (err) => console.error(err),
  complete: () => console.log('done')
});
```

---

## Key Operators

### Transformation
```typescript
// map — transform each value
users$.pipe(map(users => users.length))

// switchMap — cancel previous, switch to new inner observable
// Most common for search / navigation
searchTerm$.pipe(
  debounceTime(300),
  switchMap(term => this.api.search(term))
)

// mergeMap — run all inner observables concurrently (parallel requests)
ids$.pipe(
  mergeMap(id => this.api.getUser(id))
)

// concatMap — queue inner observables, run one at a time
actions$.pipe(
  concatMap(action => this.api.save(action))
)
```

### Filtering
```typescript
filter(user => user.active)
debounceTime(300)          // wait 300ms after last emission (search inputs)
distinctUntilChanged()     // skip if same as previous value
take(1)                    // take first value then complete
```

### Error Handling
```typescript
this.api.getUser(id).pipe(
  catchError(err => {
    console.error(err);
    return of(null);        // return fallback observable
  })
)
```

### Combination
```typescript
// forkJoin — wait for all to complete (like Promise.all)
forkJoin([this.api.getUser(), this.api.getOrders()])

// combineLatest — emit when any source emits (latest from all)
combineLatest([users$, filters$]).pipe(
  map(([users, filters]) => applyFilters(users, filters))
)
```

---

## switchMap vs mergeMap vs concatMap

| Operator | Behavior | Use for |
|---|---|---|
| `switchMap` | Cancels previous inner observable | Search, navigation, HTTP where only latest matters |
| `mergeMap` | Runs all concurrently | Parallel requests, no ordering needed |
| `concatMap` | Queues, runs one at a time | Sequential operations, saves in order |
| `exhaustMap` | Ignores new while current active | Login button (ignore double-click) |

---

## Subjects (both Observable and Observer)

```typescript
// Subject — multicast, no initial value
const events$ = new Subject<string>();
events$.next('click');   // emit
events$.subscribe(e => console.log(e));

// BehaviorSubject — has current value, new subscribers get latest
const user$ = new BehaviorSubject<User | null>(null);
user$.next(loggedInUser);
user$.getValue();         // read current value synchronously

// ReplaySubject — replays N last values to new subscribers
const log$ = new ReplaySubject<string>(10);
```

**BehaviorSubject is the standard pattern for shared state in Angular services:**
```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUser$ = new BehaviorSubject<User | null>(null);
  user$ = this.currentUser$.asObservable();  // expose as read-only

  login(user: User) { this.currentUser$.next(user); }
  logout() { this.currentUser$.next(null); }
}
```

---

## async Pipe — Preferred in Templates

Automatically subscribes and unsubscribes — no memory leaks:

```typescript
// Component
users$ = this.userService.getUsers();  // don't subscribe — let async pipe do it
```

```html
<ul>
  <li *ngFor="let user of users$ | async">{{ user.name }}</li>
</ul>
```

**Vs manual subscription:**
```typescript
// Avoid this pattern when async pipe can be used
ngOnInit() {
  this.userService.getUsers().subscribe(u => this.users = u);  // must unsubscribe manually
}
```

---

## Memory Leak — takeUntil Pattern

```typescript
private destroy$ = new Subject<void>();

ngOnInit() {
  interval(1000).pipe(
    takeUntil(this.destroy$)
  ).subscribe(tick => this.timer = tick);
}

ngOnDestroy() {
  this.destroy$.next();
  this.destroy$.complete();
}
```

Angular 16+ alternative:
```typescript
private destroyRef = inject(DestroyRef);

ngOnInit() {
  interval(1000).pipe(
    takeUntilDestroyed(this.destroyRef)
  ).subscribe(...);
}
```

---

## Follow-up Questions

**Q: Observable vs Promise — when to use which?**
→ Observables for streams (WebSocket, search-as-you-type, real-time), cancellable requests, or any complex async chain. Promises for simple one-off values. Angular's HttpClient returns Observables — you can `toPromise()` if needed but rarely should.

**Q: What is the difference between Subject and BehaviorSubject?**
→ Subject: no initial value, late subscribers miss past emissions. BehaviorSubject: has a current value, late subscribers immediately get the latest value. Use BehaviorSubject for state (auth status, cart items).

**Q: Why is switchMap dangerous if used wrong?**
→ It cancels the previous inner observable on each emission. For a save/delete operation, using switchMap would cancel the in-flight request if the user triggers it twice. Use concatMap or exhaustMap for writes.

**Q: Cold vs Hot observables?**
→ Cold: creates a new execution per subscriber (HTTP call, `of()`). Hot: shared execution across all subscribers (Subject, DOM events). BehaviorSubject is hot.

**Q: How does the async pipe prevent memory leaks?**
→ async pipe subscribes when the component renders and calls `unsubscribe()` automatically in `ngOnDestroy`. No manual cleanup needed — that's why it's the preferred pattern.
