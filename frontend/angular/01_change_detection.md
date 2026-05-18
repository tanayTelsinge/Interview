# Change Detection

## What is Change Detection?
Angular's mechanism to sync component state with the DOM.
When data changes, Angular checks what changed and updates only that part of the DOM.

---

## How it works internally

**Zone.js** (Angular's default mechanism):
- Monkey-patches all async APIs: `setTimeout`, `setInterval`, `Promise`, `XHR`, DOM events
- Every time any async event fires, Zone.js notifies Angular
- Angular runs change detection on the entire component tree (top-down)

```
User clicks button
  → Zone.js intercepts the event
  → Notifies Angular
  → Angular traverses entire component tree
  → Checks every component for changes
  → Updates DOM where needed
```

---

## Default vs OnPush Strategy

| | Default | OnPush |
|---|---|---|
| When it checks | Every event, timer, HTTP call | Only when @Input reference changes, async pipe emits, or manually triggered |
| Performance | OK for small apps | Much better for large apps |
| Use for | Any component | Components with immutable inputs |

```typescript
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserCardComponent {
  @Input() user: User;  // must pass new object reference to trigger update
}
```

---

## OnPush — Triggers (what causes it to run)

1. `@Input()` receives a **new reference** (not mutation)
2. An `async` pipe emits a new value
3. A DOM event fires inside this component
4. `ChangeDetectorRef.markForCheck()` is called manually

```typescript
// Triggers OnPush ✓ — new reference
this.user = { ...this.user, name: 'Alice' };

// Does NOT trigger OnPush ✗ — mutating same object
this.user.name = 'Alice';
```

---

## Signals (Angular 16+) — Fine-grained Reactivity

Signals bypass Zone.js entirely. Angular knows exactly which DOM nodes use a signal and updates only those.

```typescript
count = signal(0);

increment() {
  this.count.update(v => v + 1);  // only DOM node using count() updates
}
```

| | Zone.js | Signals |
|---|---|---|
| Granularity | Whole component tree check | Exact DOM node |
| Mechanism | Monkey-patches async APIs | Reactive wrapper |
| Bundle size | Zone.js adds ~10KB | Zone-free possible |
| Angular version | All | 16+ |

---

## Detaching Change Detection

For highly performance-sensitive components (charts, infinite lists):

```typescript
constructor(private cd: ChangeDetectorRef) {
  cd.detach();  // remove from CD tree
}

updateData() {
  this.data = fetchData();
  this.cd.detectChanges();  // manually trigger only when needed
}
```

---

## Java Analogy

Zone.js = Spring AOP interceptor — wraps every method call to run extra logic (CD notification) before/after. `OnPush` = `@Cacheable` — only re-computes when input changes.

---

## Follow-up Questions

**Q: What is Zone.js and can Angular work without it?**
→ Zone.js patches async APIs to notify Angular when to run CD. Angular 17+ supports zoneless mode with Signals — `provideExperimentalZonelessChangeDetection()`. Smaller bundle, better performance.

**Q: Why does mutating an object not trigger OnPush?**
→ OnPush checks reference equality (`===`). Mutating the same object keeps the same reference, so Angular sees no change. Must spread into a new object.

**Q: Default vs OnPush — which to use?**
→ Default for simple/small components. OnPush for any component receiving objects as `@Input` — forces immutable data flow and dramatically reduces unnecessary CD cycles.

**Q: markForCheck vs detectChanges?**
→ `markForCheck()` marks the component and its ancestors as dirty — CD runs on next tick. `detectChanges()` runs CD immediately and synchronously on this component only. `detectChanges` is for detached components.

**Q: What happens if you have a setInterval updating data and OnPush?**
→ DOM won't update because Zone.js notifies Angular but OnPush skips this component unless the interval updates an `@Input` reference, uses a signal, or you call `markForCheck()` inside.
