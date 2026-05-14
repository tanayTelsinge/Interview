# Angular Interview Cheat Sheet

---

## Angular vs React

| | Angular | React |
|--|---------|-------|
| Type | Full framework | Library |
| Developed by | Google | Meta |
| Language | TypeScript | JavaScript/TypeScript |
| Data binding | Two-way | One-way |
| DOM | Change detection | Virtual DOM |
| Routing | Built-in | Add-on (React Router) |
| State management | Built-in services | Add-on (Redux) |
| HTTP | Built-in HttpClient | Add-on (axios/fetch) |
| Learning curve | Steep | Moderate |
| Best for | Enterprise, large teams | Startups, fast iteration |

**Interview Answer:**
> "Angular is a complete framework — routing, HTTP, forms, state management all built in. React is a library focused only on UI — everything else is add-on. Angular enforces strict TypeScript and opinionated structure making it suitable for large enterprise teams like Citi. React offers more flexibility."

---

## Component

### What is a Component?
Smallest building block of Angular UI. Consists of 4 parts:

| Part | Description |
|------|-------------|
| Template | HTML — the view |
| Class | TypeScript — behaviour/logic |
| Metadata | `@Component` decorator — selector, templateUrl, styleUrls |
| Styles | Scoped CSS/SCSS — only applies to this component |

```typescript
@Component({
  selector: 'app-finance',           // HTML tag: <app-finance>
  templateUrl: './finance.component.html',
  styleUrls: ['./finance.component.scss']
})
export class FinanceComponent implements OnInit {
  title = 'Finance Dashboard';

  constructor(private financeService: FinanceService) {}

  ngOnInit(): void {
    // fetch data here, not in constructor
  }
}
```

---

## Module

### What is a Module?
Collection of components, directives, pipes and services grouped together.

```typescript
@NgModule({
  declarations: [FinanceComponent, OwnerComponent],  // components in this module
  imports: [CommonModule, RouterModule, FormsModule], // other modules needed
  exports: [FinanceComponent],                        // expose to other modules
  providers: [FinanceService]                         // services
})
export class FinanceModule {}
```

| Property | Description |
|----------|-------------|
| `declarations` | Components, directives, pipes belonging to this module |
| `imports` | Other modules this module depends on |
| `exports` | What this module exposes to others |
| `providers` | Services available in this module |
| `bootstrap` | Root component (AppModule only) |

**`forRoot` vs `forChild`:**
```typescript
// AppModule — use forRoot (configures router once)
RouterModule.forRoot(routes)

// Feature modules — use forChild
RouterModule.forChild(routes)
```

---

## Dependency Injection

### How DI works in Angular
Angular has an **Injector** (like Spring's IoC container) that manages object lifecycle.

```typescript
// Register service — singleton across app
@Injectable({
  providedIn: 'root'  // available everywhere, single instance
})
export class FinanceService {}

// Inject in component — constructor injection
constructor(private financeService: FinanceService) {}

// Modern way (Angular 14+) — inject() function
private financeService = inject(FinanceService);
```

**Spring analogy:**
> `providedIn: 'root'` = Spring's default singleton scope
> Constructor injection = `@Autowired`
> Angular Injector = Spring IoC container

---

## Lifecycle Hooks

| Hook | When called | Common use |
|------|-------------|------------|
| `ngOnChanges` | `@Input` value changes | React to parent data changes |
| `ngOnInit` | After first ngOnChanges | API calls, initialization |
| `ngDoCheck` | Every change detection cycle | Custom change detection |
| `ngAfterViewInit` | After view renders | DOM manipulation |
| `ngOnDestroy` | Before component destroyed | Unsubscribe observables |

### Constructor vs ngOnInit
```typescript
constructor(private service: FinanceService) {
  // ONLY use for DI
  // @Input() values NOT available here
}

ngOnInit(): void {
  // @Input() values available here
  // Safe to make API calls
  this.loadData();
}
```

**Spring analogy:** Constructor = bean instantiation, ngOnInit = `@PostConstruct`

---

## Data Binding

### Types of Binding
```html
<!-- Property binding — class to template -->
<input [value]="username" />

<!-- Event binding — template to class -->
<button (click)="onSubmit()">Submit</button>

<!-- Two-way binding — both directions -->
<input [(ngModel)]="username" />
<!-- requires FormsModule imported in module -->

<!-- String interpolation -->
<p>{{ username }}</p>
```

### Two-way binding internals
```html
<!-- [(ngModel)] is shorthand for: -->
<input [ngModel]="username" (ngModelChange)="username = $event" />
```

**"Banana in a box"** — `[()]` = property binding + event binding combined.

---

## Observables & RxJS

### Observable vs Promise

| | Observable | Promise |
|--|-----------|---------|
| Values | Multiple over time | Single value |
| Execution | Lazy (only on subscribe) | Eager (runs immediately) |
| Cancellable | Yes (unsubscribe) | No |
| Operators | map, filter, switchMap etc | .then() only |

### Common RxJS Operators
```typescript
import { map, filter, switchMap, catchError } from 'rxjs/operators';

// map — transform value
this.financeService.getPayments().pipe(
  map(payments => payments.filter(p => p.amount > 0))
);

// switchMap — cancel previous, use latest (search/autocomplete)
this.searchInput.valueChanges.pipe(
  switchMap(term => this.financeService.search(term))
);

// catchError — handle errors
this.financeService.getPayments().pipe(
  catchError(err => {
    console.error(err);
    return of([]);  // return empty array on error
  })
);
```

### Subject vs BehaviorSubject

| | Subject | BehaviorSubject |
|--|---------|----------------|
| Initial value | None required | Required |
| Late subscribers | Get nothing | Get last emitted value |
| Use case | Events | Shared state |

```typescript
// Subject — event bus
private paymentSubject = new Subject<Payment>();
payment$ = this.paymentSubject.asObservable();
this.paymentSubject.next(payment);

// BehaviorSubject — shared state
private userSubject = new BehaviorSubject<User>(null);
user$ = this.userSubject.asObservable();
// new subscribers immediately get current user
this.userSubject.next(user);
```

### Always unsubscribe in ngOnDestroy
```typescript
private subscription: Subscription;

ngOnInit(): void {
  this.subscription = this.financeService.getPayments()
    .subscribe(payments => this.payments = payments);
}

ngOnDestroy(): void {
  this.subscription.unsubscribe();  // prevent memory leak
}

// Better — async pipe handles unsubscribe automatically
// payments$ = this.financeService.getPayments();
// <div *ngFor="let p of payments$ | async">
```

---

## Change Detection

### Default Strategy
- Angular checks **every component** in the tree on every event
- Zone.js intercepts async events (click, HTTP, timer) and triggers detection
- Expensive for large component trees

### OnPush Strategy
- Component only checked when:
  - `@Input` reference changes (not mutation)
  - Event from within the component
  - Async pipe emits
  - `ChangeDetectorRef.markForCheck()` called

```typescript
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush
})
```

### Mutation vs Reference (Critical)
```typescript
// OnPush will NOT detect — same reference
this.payment.amount = 1000;

// OnPush WILL detect — new reference
this.payment = { ...this.payment, amount: 1000 };
```

**Citi relevance:** Large financial dashboards — OnPush + immutable data = significant performance improvement.

---

## Routing

### Setup
```typescript
const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'finance', component: FinanceComponent },
  { path: 'finance/:id', component: FinanceDetailComponent },
  {
    path: 'owner',
    loadChildren: () => import('./owner/owner.module')
      .then(m => m.OwnerModule)  // lazy loading
  },
  { path: '**', redirectTo: '' }  // wildcard 404
];

@NgModule({
  imports: [RouterModule.forRoot(routes)]
})
```

```html
<!-- router outlet — where routed components render -->
<router-outlet></router-outlet>

<!-- navigation -->
<a routerLink="/finance">Finance</a>
<a [routerLink]="['/finance', payment.id]">Details</a>
```

### Router vs ActivatedRoute

| | Router | ActivatedRoute |
|--|--------|---------------|
| Purpose | Navigate to routes | Read current route info |
| Analogy | Steering wheel | GPS |

```typescript
constructor(
  private router: Router,           // for navigation
  private route: ActivatedRoute     // for reading params
) {}

// Navigate programmatically
this.router.navigate(['/finance', id]);

// Read route params
this.route.params.subscribe(params => {
  this.id = params['id'];
});

// Read query params
this.route.queryParams.subscribe(params => {
  this.page = params['page'];
});
```

---

## Route Guards

| Guard | Purpose | Failure action |
|-------|---------|---------------|
| `canActivate` | Can user access route? | Redirect to login |
| `canActivateChild` | Can user access child routes? | Redirect |
| `canDeactivate` | Can user leave route? | Show unsaved changes dialog |
| `canLoad` | Can lazy module load? | Block load |
| `resolve` | Pre-fetch data before route | Load data first |

```typescript
// Modern functional guard (Angular 14+)
export const authGuard = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) return true;
  return router.navigate(['/login']);
};

// Usage in routes
{ path: 'finance', component: FinanceComponent, canActivate: [authGuard] }
```

**Citi relevance:**
- `canActivate` — role based access (VP vs analyst)
- `canDeactivate` — prevent leaving unsaved financial form
- `resolve` — pre-load financial data before rendering

---

## Lazy Loading

### Route-level lazy loading
```typescript
{
  path: 'finance',
  loadChildren: () => import('./finance/finance.module')
    .then(m => m.FinanceModule)
}

// Standalone component (Angular 14+)
{
  path: 'finance',
  loadComponent: () => import('./finance.component')
    .then(c => c.FinanceComponent)
}
```

**Why it matters for Citi:**
> Large enterprise app — lazy loading means finance module doesn't load until user navigates there. Faster initial load time.

---

## Forms

### Template-driven vs Reactive

| | Template-driven | Reactive |
|--|----------------|---------|
| Logic in | HTML | TypeScript |
| Form model | Implicit (Angular creates) | Explicit (you create) |
| Execution | Async | Sync |
| Unit testable | Hard | Easy |
| Use case | Simple forms | Complex, dynamic forms |

### Reactive Forms
```typescript
// Component
this.form = this.fb.group({
  username: ['', [Validators.required, Validators.minLength(3)]],
  email: ['', Validators.email],
  amount: ['', [Validators.required, Validators.min(0)]]
});

// Access values
this.form.value;          // { username: '', email: '', amount: '' }
this.form.valid;          // true/false
this.form.get('username').errors; // null or error object
```

```html
<form [formGroup]="form" (ngSubmit)="submit()">
  <input formControlName="username" />
  <div *ngIf="form.get('username').errors?.required">Required</div>
  <div *ngIf="form.get('username').errors?.minlength">Min 3 chars</div>
  <button type="submit" [disabled]="form.invalid">Submit</button>
</form>
```

### FormGroup, FormControl, FormArray

| | Description | Use case |
|--|-------------|----------|
| `FormControl` | Single field | Individual input |
| `FormGroup` | Group of controls | A form |
| `FormArray` | Dynamic list of controls | Add/remove rows |

```typescript
// FormArray — dynamic rows (e.g. multiple beneficiaries)
this.form = this.fb.group({
  beneficiaries: this.fb.array([])
});

get beneficiaries() {
  return this.form.get('beneficiaries') as FormArray;
}

addBeneficiary() {
  this.beneficiaries.push(this.fb.group({
    name: ['', Validators.required],
    amount: ['', Validators.required]
  }));
}
```

### Custom Validator
```typescript
// Single field validator
function amountValidator(control: AbstractControl) {
  const value = control.value;
  if (!value) return null;
  return value > 0 ? null : { negativeAmount: true };
}

// Cross-field validator (group level)
function dateRangeValidator(group: AbstractControl) {
  const start = group.get('startDate').value;
  const end = group.get('endDate').value;
  return start < end ? null : { invalidRange: true };
}

// Usage
this.form = this.fb.group({
  amount: ['', [Validators.required, amountValidator]],
  startDate: [''],
  endDate: ['']
}, { validators: dateRangeValidator });
```

---

## Services

```typescript
@Injectable({
  providedIn: 'root'  // singleton
})
export class FinanceService {
  private apiUrl = 'api/finance';

  constructor(private http: HttpClient) {}

  getPayments(): Observable<Payment[]> {
    return this.http.get<Payment[]>(this.apiUrl);
  }

  savePayment(payment: Payment): Observable<Payment> {
    return this.http.post<Payment>(this.apiUrl, payment);
  }
}
```

**Why services instead of component logic:**
- Reusable across components
- Singleton — shared state via BehaviorSubject
- Testable independently
- Separation of concerns

---

## Angular 17 New Features

### 1. @defer — Deferrable Views
Lazy load parts of template without routing.

```html
<!-- Load heavy component only when visible in viewport -->
@defer (on viewport) {
  <app-heavy-chart />
} @placeholder {
  <p>Loading chart...</p>
} @loading (minimum 500ms) {
  <app-spinner />
} @error {
  <p>Failed to load chart</p>
}
```

**Triggers:**
| Trigger | When |
|---------|------|
| `on viewport` | Element enters viewport |
| `on idle` | Browser is idle |
| `on interaction` | User clicks/focuses |
| `on timer(2s)` | After delay |
| `when condition` | Custom boolean condition |

**Citi relevance:** Financial dashboard with heavy charts — defer non-critical sections, load critical data first.

---

### 2. New Control Flow Syntax
Replaces `*ngIf`, `*ngFor`, `*ngSwitch` directives.

```html
<!-- Old way -->
<div *ngIf="payments.length > 0">...</div>
<div *ngFor="let p of payments">...</div>

<!-- New way (Angular 17+) -->
@if (payments.length > 0) {
  <div>{{ payments.length }} payments</div>
} @else {
  <p>No payments found</p>
}

@for (payment of payments; track payment.id) {
  <div>{{ payment.amount }}</div>
} @empty {
  <p>No payments</p>
}

@switch (payment.status) {
  @case ('PAID') { <span>Paid</span> }
  @case ('PENDING') { <span>Pending</span> }
  @default { <span>Unknown</span> }
}
```

**Why better:**
- Built into compiler — better performance than directives
- `track` is mandatory in `@for` — prevents full re-render on list change
- `@empty` block — no need for separate `*ngIf` for empty state

---

### 3. Signals (Reactivity Primitive)
New reactive primitive — alternative to RxJS for simple state.

```typescript
import { signal, computed, effect } from '@angular/core';

// Create signal
count = signal(0);

// Computed — derives from signal, auto updates
doubleCount = computed(() => this.count() * 2);

// Effect — runs when signal changes (like useEffect in React)
effect(() => {
  console.log('Count changed:', this.count());
});

// Update signal
increment() {
  this.count.update(c => c + 1);  // or this.count.set(5)
}
```

```html
<!-- Read signal in template — call it as function -->
<p>Count: {{ count() }}</p>
<p>Double: {{ doubleCount() }}</p>
```

**Signals vs RxJS:**
| | Signals | RxJS Observable |
|--|---------|----------------|
| Complexity | Simple | Complex |
| Use case | Component state | Async streams, HTTP |
| Subscription | Not needed | Required |
| Learning curve | Low | High |

---

### 4. Standalone Components (Stable in 17)
Components without NgModule.

```typescript
@Component({
  standalone: true,                          // no module needed
  selector: 'app-finance',
  templateUrl: './finance.component.html',
  imports: [CommonModule, RouterModule]      // import directly in component
})
export class FinanceComponent {}
```

**Benefits:**
- Simpler — no module boilerplate
- Better tree shaking — only import what you use
- Easier lazy loading per component

---

### 5. Built-in SSR (Server Side Rendering)
```bash
# Enable SSR during project creation
ng new my-app --ssr

# Add SSR to existing app
ng add @angular/ssr
```

**Why SSR matters:**
- Faster initial page load
- Better SEO
- Important for public facing financial portals

---

## Angular Performance Optimization

### 1. OnPush Change Detection
```typescript
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush
})
// Only checks when @Input reference changes — not every event
```

### 2. TrackBy in @for / *ngFor
```html
<!-- Without trackBy — full DOM re-render on any list change -->
<div *ngFor="let payment of payments">

<!-- With trackBy — only re-renders changed items -->
<div *ngFor="let payment of payments; trackBy: trackByPaymentId">

<!-- Angular 17 @for — track mandatory -->
@for (payment of payments; track payment.id) {
```

```typescript
trackByPaymentId(index: number, payment: Payment): number {
  return payment.id;
}
```

### 3. Lazy Loading Modules/Components
```typescript
// Only load finance module when user navigates to /finance
{
  path: 'finance',
  loadChildren: () => import('./finance/finance.module')
    .then(m => m.FinanceModule)
}
```

### 4. Async Pipe — Auto Unsubscribe
```typescript
// Component — no manual subscribe/unsubscribe
payments$ = this.financeService.getPayments();
```
```html
<!-- Async pipe subscribes and unsubscribes automatically -->
@for (payment of payments$ | async; track payment.id) {
  <div>{{ payment.amount }}</div>
}
```

### 5. @defer for Heavy Components
```html
@defer (on viewport) {
  <app-heavy-chart />
}
```

### 6. Pure Pipes
```typescript
@Pipe({ name: 'formatCurrency', pure: true })
// pure: true (default) — only recalculates when input changes
// pure: false — recalculates every change detection cycle (avoid)
```

### 7. Avoid Function Calls in Templates
```html
<!-- Bad — called every change detection cycle -->
<p>{{ calculateTotal(payments) }}</p>

<!-- Good — use pipe or computed property -->
<p>{{ total }}</p>  <!-- calculated once in component -->
```

### 9. Tree Shaking
Removes unused code at build time — smaller bundle = faster load.

```typescript
// Bad — imports entire library, tree shaking blocked
import * as _ from 'lodash';

// Good — only imports what you use
import { debounce } from 'lodash-es';  // only debounce bundled
```

```bash
# Tree shaking enabled automatically in production build
ng build --configuration=production
```

**Why standalone components improve tree shaking:**
```typescript
// Old NgModule — unused components still bundled
// Standalone — only explicitly imported items bundled
@Component({
  standalone: true,
  imports: [ReactiveFormsModule]  // only this gets bundled, nothing else
})
```

**Works because of ES6 modules:**
- `import/export` are static — bundler knows at build time what's used
- CommonJS `require()` is dynamic — tree shaking doesn't work

**Interview Answer:**
> "Tree shaking removes unused code at build time — if you import a library but use only 2 functions, the rest are excluded from the final bundle. Angular's production build does this automatically via esbuild/Webpack. Standalone components improve tree shaking further — you explicitly declare only what each component needs."

---

### 10. Bundle Optimization
```bash
# Production build — tree shaking, minification, AOT
ng build --configuration=production
```

---

## Observable vs Promise — Deep Dive

### Key Differences

| | Observable | Promise |
|--|-----------|---------|
| Values | Multiple over time | Single value |
| Execution | **Lazy** — runs only on subscribe | **Eager** — runs immediately |
| Cancellable | Yes — unsubscribe() | No |
| Operators | 100+ (map, filter, switchMap) | .then() only |
| Error handling | catchError operator | .catch() |
| Retry | retry() operator | Manual |
| Angular HTTP | Returns Observable | Not used |

### Lazy vs Eager
```typescript
// Promise — EAGER — runs immediately even without .then()
const promise = fetch('/api/payments');  // HTTP call fires NOW

// Observable — LAZY — nothing happens until subscribe
const observable = this.http.get('/api/payments');  // no HTTP call yet
observable.subscribe(...);  // HTTP call fires HERE
```

### Cancellation
```typescript
// Promise — cannot cancel
const promise = fetch('/api/payments');
// no way to cancel this

// Observable — can cancel
const subscription = this.http.get('/api/payments')
  .subscribe(...);
subscription.unsubscribe();  // cancels the HTTP request
```

### switchMap — cancels previous (critical for search)
```typescript
// User types fast — cancels previous search, only latest matters
this.searchInput.valueChanges.pipe(
  debounceTime(300),          // wait 300ms after user stops typing
  distinctUntilChanged(),     // don't search if same value
  switchMap(term =>           // cancel previous, use latest
    this.financeService.search(term)
  )
).subscribe(results => this.results = results);
```

### When to use Promise vs Observable
| Use Promise | Use Observable |
|-------------|----------------|
| One-time async operation | Multiple values over time |
| Simple async/await code | HTTP with operators (retry, cancel) |
| Non-Angular code | Angular HttpClient |
| Third party libraries | WebSocket, real-time data |

### Converting between them
```typescript
// Observable to Promise
const result = await firstValueFrom(this.http.get('/api/payments'));

// Promise to Observable
import { from } from 'rxjs';
const observable = from(fetch('/api/payments'));
```

---

## TypeScript vs JavaScript

### Key Differences

| | TypeScript | JavaScript |
|--|-----------|-----------|
| Typing | Static (compile time) | Dynamic (runtime) |
| Errors caught | At compile time | At runtime |
| IDE support | Excellent (autocomplete, refactor) | Basic |
| Interfaces | Yes | No |
| Generics | Yes | No |
| Decorators | Yes | Limited |
| Compiled to | JavaScript | Runs directly |
| Learning curve | Higher | Lower |
| Enterprise use | Preferred | Legacy/simple projects |

### TypeScript Key Features

#### Types
```typescript
// Primitive types
let name: string = 'Tanay';
let age: number = 30;
let active: boolean = true;

// Array
let payments: number[] = [100, 200, 300];
let names: Array<string> = ['a', 'b'];

// Union type
let id: string | number = 'ABC123';

// Any — avoid in production
let data: any = 'anything';
```

#### Interface
```typescript
interface Payment {
  id: number;
  amount: number;
  status: 'PAID' | 'PENDING' | 'OVERDUE';  // literal type
  dueDate?: Date;  // optional field
}

// Usage
const payment: Payment = {
  id: 1,
  amount: 1000,
  status: 'PENDING'
};
```

#### Generics
```typescript
// Generic function
function getFirst<T>(items: T[]): T {
  return items[0];
}

getFirst<number>([1, 2, 3]);   // returns number
getFirst<string>(['a', 'b']);  // returns string

// Generic interface
interface ApiResponse<T> {
  data: T;
  status: number;
  message: string;
}

// Usage
const response: ApiResponse<Payment[]> = {
  data: payments,
  status: 200,
  message: 'success'
};
```

#### Decorators (used heavily in Angular)
```typescript
@Component({...})        // class decorator
@Input()                 // property decorator
@HostListener('click')   // method decorator
```

#### Type vs Interface
```typescript
// Interface — for objects, extendable
interface Animal {
  name: string;
}
interface Dog extends Animal {
  breed: string;
}

// Type — for unions, primitives, tuples
type Status = 'PAID' | 'PENDING' | 'OVERDUE';
type Pair = [string, number];
type StringOrNumber = string | number;
```

#### Null Safety
```typescript
// Optional chaining
const city = user?.address?.city;  // no null reference error

// Nullish coalescing
const name = user.name ?? 'Anonymous';  // use right side if null/undefined

// Non-null assertion (use carefully)
const name = user.name!;  // tells TS "trust me, not null"
```

### Why TypeScript for Enterprise (Citi)
> "TypeScript catches type errors at compile time — critical in financial systems where wrong data types can cause calculation errors. It provides excellent IDE support for large codebases — refactoring, autocomplete, navigation. Interfaces model domain objects clearly — `Payment`, `Settlement`, `Ledger` — making the codebase self-documenting. This is why Angular chose TypeScript and why large banks prefer it."

---

## Gap Mitigation — Frontend at Citi

When asked about frontend experience:

> "My primary strength is backend — Java, Spring Boot, microservices, financial domain. I've worked with Angular at Maxxton — contributed to UI bug fixes, minor components, and was part of the Angular 16 to 17 migration. I understand Angular's architecture — components, services, DI, reactive forms, RxJS. I'm comfortable contributing to frontend discussions and can ramp up quickly. For complex financial UIs, the backend contract and data modeling is where I add the most value."

---

## NgRx — State Management

### What is NgRx?
Redux pattern implemented for Angular. Centralized, immutable state management using RxJS Observables.

**When to use NgRx:**
- Large app with complex shared state
- Multiple components need the same data
- State changes need to be tracked/auditable
- Time-travel debugging needed

**When NOT to use:**
- Simple apps — BehaviorSubject in a service is enough
- Component-local state — use component variables

---

### Core Concepts

```
Component → dispatches Action
Action    → describes what happened ("Load Payments")
Reducer   → pure function, produces new state from action
Store     → holds the single source of truth (state)
Selector  → reads slice of state
Effect    → handles side effects (API calls)
```

### Flow Diagram
```
Component
   ↓ dispatch(action)
Store
   ↓ action
Reducer → new state
   ↓
Store (updated)
   ↓ select(selector)
Component (re-renders)

Side effects:
Action → Effect → API call → success/failure action → Reducer
```

---

### Implementation

#### 1. Define State
```typescript
// state/payment.state.ts
export interface PaymentState {
  payments: Payment[];
  loading: boolean;
  error: string | null;
}

export const initialState: PaymentState = {
  payments: [],
  loading: false,
  error: null
};
```

#### 2. Define Actions
```typescript
// state/payment.actions.ts
import { createAction, props } from '@ngrx/store';

export const loadPayments = createAction('[Payment] Load Payments');

export const loadPaymentsSuccess = createAction(
  '[Payment] Load Payments Success',
  props<{ payments: Payment[] }>()
);

export const loadPaymentsFailure = createAction(
  '[Payment] Load Payments Failure',
  props<{ error: string }>()
);
```

#### 3. Define Reducer
```typescript
// state/payment.reducer.ts
import { createReducer, on } from '@ngrx/store';

export const paymentReducer = createReducer(
  initialState,

  on(loadPayments, state => ({
    ...state,
    loading: true,
    error: null
  })),

  on(loadPaymentsSuccess, (state, { payments }) => ({
    ...state,
    loading: false,
    payments  // new reference — works with OnPush
  })),

  on(loadPaymentsFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  }))
);
```

#### 4. Define Selectors
```typescript
// state/payment.selectors.ts
import { createSelector, createFeatureSelector } from '@ngrx/store';

export const selectPaymentState = createFeatureSelector<PaymentState>('payments');

export const selectAllPayments = createSelector(
  selectPaymentState,
  state => state.payments
);

export const selectLoading = createSelector(
  selectPaymentState,
  state => state.loading
);

export const selectError = createSelector(
  selectPaymentState,
  state => state.error
);

// Derived selector — memoized, only recalculates when input changes
export const selectOverduePayments = createSelector(
  selectAllPayments,
  payments => payments.filter(p => p.status === 'OVERDUE')
);
```

#### 5. Define Effects (Side Effects — API calls)
```typescript
// state/payment.effects.ts
import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { switchMap, map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Injectable()
export class PaymentEffects {

  loadPayments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(loadPayments),           // listen for this action
      switchMap(() =>
        this.paymentService.getPayments().pipe(
          map(payments => loadPaymentsSuccess({ payments })),
          catchError(error => of(loadPaymentsFailure({ error: error.message })))
        )
      )
    )
  );

  constructor(
    private actions$: Actions,
    private paymentService: PaymentService
  ) {}
}
```

#### 6. Use in Component
```typescript
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush  // works perfectly with NgRx
})
export class PaymentComponent implements OnInit {

  payments$ = this.store.select(selectAllPayments);
  loading$ = this.store.select(selectLoading);
  overduePayments$ = this.store.select(selectOverduePayments);

  constructor(private store: Store) {}

  ngOnInit(): void {
    this.store.dispatch(loadPayments());  // trigger load
  }

  approvePayment(id: number): void {
    this.store.dispatch(approvePayment({ id }));  // dispatch action
  }
}
```

```html
@if (loading$ | async) {
  <app-spinner />
}

@for (payment of payments$ | async; track payment.id) {
  <div>{{ payment.amount }}</div>
}
```

---

### NgRx vs BehaviorSubject

| | NgRx | BehaviorSubject in Service |
|--|------|---------------------------|
| Complexity | High | Low |
| Boilerplate | High (actions, reducers, effects, selectors) | Minimal |
| Debugging | Redux DevTools, time travel | Console logs |
| Scalability | Excellent for large apps | Gets messy in large apps |
| Side effects | Effects (structured) | Manual in service |
| Use case | Large enterprise apps | Small-medium apps |
| Citi relevance | Complex financial dashboards | Simple shared state |

---

### NgRx + OnPush = Performance

```
NgRx state is immutable — reducers always return new objects
New object = new reference
OnPush detects new reference → re-renders only affected components
= maximum performance for large financial dashboards
```

---

### Common Interview Questions

**Q: What is the difference between Action and Effect in NgRx?**
> "Action describes what happened — a plain object with a type. Effect handles side effects triggered by actions — API calls, navigation, logging. Effects listen for specific actions, perform async work, then dispatch success or failure actions back to the store."

**Q: Why use NgRx over a simple service with BehaviorSubject?**
> "For simple shared state, BehaviorSubject in a service is sufficient. NgRx becomes valuable when state is complex — multiple components modifying the same data, need for audit trail, time-travel debugging, or when side effects need structured management. For a financial dashboard at Citi with complex shared state across many components, NgRx provides structure and predictability."

**Q: What is a Selector and why is it important?**
> "Selectors are pure functions that read slices of state from the store. They are memoized — NgRx only recalculates when the input state changes, not on every emission. This prevents unnecessary recalculation for derived data like filtered payment lists."

**Q: What is the difference between switchMap, mergeMap and concatMap in Effects?**

| Operator | Behavior | Use case |
|----------|----------|----------|
| `switchMap` | Cancels previous, uses latest | Search, load on navigation |
| `mergeMap` | Runs all concurrently | Independent parallel requests |
| `concatMap` | Queues, runs one at a time | Sequential operations, order matters |

> "In payment loading I'd use switchMap — if user navigates away, cancel the previous load. For submitting multiple independent payments, mergeMap. For sequential operations where order matters like transaction steps, concatMap."

---

### Interview Answer — NgRx Experience

> "I'm familiar with NgRx architecture — store, actions, reducers, effects, and selectors. The Redux pattern maps well to financial applications where state auditability and predictability are important. NgRx pairs well with OnPush change detection — immutable state means new references on every update, which OnPush detects efficiently. For Citi's financial dashboards with complex shared state, NgRx would be the right choice over simple service-based state management."

---

## Key Angular Concepts Summary

| Concept | One line |
|---------|----------|
| Component | UI building block with template + class |
| Module | Group of related components/services |
| DI | Angular manages object creation and injection |
| Observable | Lazy async stream, multiple values |
| BehaviorSubject | Observable with current value — shared state |
| Change Detection | How Angular knows when to update UI |
| OnPush | Performance — only check on reference change |
| Lazy Loading | Load modules only when needed |
| Guard | Control route access |
| Reactive Forms | Type-safe, testable forms defined in TypeScript |
| NgRx | Redux state management — store, actions, reducers, effects, selectors |
| Signals | New Angular 17 reactivity primitive — simpler than RxJS for component state |