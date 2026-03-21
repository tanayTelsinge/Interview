# Angular — Interview Cheatsheet (6 YOE Fullstack)

## Core Mental Model
Angular = **opinionated full framework**. Components + Services + DI + RxJS + Modules. Everything is TypeScript.

---

## Change Detection

### Default (CheckAlways)
- Checks entire component tree top-down on **every event, timer, HTTP response**
- Zone.js patches async APIs to trigger detection

### OnPush
```typescript
@Component({ changeDetection: ChangeDetectionStrategy.OnPush })
```
Re-renders only when:
- `@Input()` reference changes (not mutation)
- An event inside the component fires
- `async` pipe resolves a new value
- `markForCheck()` called manually

**Use OnPush everywhere possible** — major perf win.

```typescript
constructor(private cdr: ChangeDetectorRef) {}
this.cdr.markForCheck();    // schedule check
this.cdr.detectChanges();   // immediate check (use sparingly)
```

### Signals (Angular 16+)
```typescript
count = signal(0);
doubled = computed(() => this.count() * 2);

this.count.set(5);
this.count.update(v => v + 1);
```
- Fine-grained reactivity — only components that read the signal re-render
- No Zone.js needed with `zoneless` mode (Angular 18+)

---

## Dependency Injection

```typescript
@Injectable({ providedIn: 'root' })  // singleton app-wide
export class UserService {}

@Injectable({ providedIn: 'any' })   // new instance per lazy module
```

### Injection Tokens
```typescript
const API_URL = new InjectionToken<string>('API_URL');
providers: [{ provide: API_URL, useValue: 'https://api.example.com' }]

constructor(@Inject(API_URL) private url: string) {}
```

### inject() function (Angular 14+)
```typescript
// Use outside constructor — in functions, guards, interceptors
const router = inject(Router);
const http   = inject(HttpClient);
```

### Hierarchical DI
```
Root Injector → Module Injector → Component Injector → Child Component Injector
```
Providing in component → scoped instance (not singleton).

---

## Lifecycle Hooks

**Execution order (on first load):**
```
constructor → ngOnChanges → ngOnInit → ngDoCheck
→ ngAfterContentInit → ngAfterContentChecked
→ ngAfterViewInit → ngAfterViewChecked
```
**On every change detection cycle:** ngDoCheck → ngAfterContentChecked → ngAfterViewChecked
**On destroy:** ngOnDestroy

| Hook | When | Common Use |
|---|---|---|
| `ngOnChanges` | Input changes (before ngOnInit on first change) | React to input changes, compare prev/curr |
| `ngOnInit` | Once, after first ngOnChanges | Fetch data, init logic — NOT constructor |
| `ngDoCheck` | Every CD cycle | Custom change detection (use sparingly — expensive) |
| `ngAfterContentInit` | After `<ng-content>` projected, once | Access `@ContentChild` |
| `ngAfterContentChecked` | After every content CD check | Rarely used |
| `ngAfterViewInit` | After view + children rendered, once | Access `@ViewChild`, init 3rd-party libs |
| `ngAfterViewChecked` | After every view CD check | Rarely used |
| `ngOnDestroy` | Before component removed | Unsubscribe, cleanup timers/listeners |

**constructor vs ngOnInit:**
- `constructor` = DI only, no input values available yet
- `ngOnInit` = inputs are set, safe to use them, make HTTP calls here

```typescript
ngOnDestroy() {
  this.subscription.unsubscribe();
  // or use takeUntilDestroyed() / DestroyRef
}
```

---

## RxJS Essentials

### Common Operators

| Operator | Use Case |
|---|---|
| `map` | Transform value |
| `filter` | Conditionally pass |
| `switchMap` | Cancel previous, start new (HTTP search) |
| `mergeMap` | Run all concurrently (parallel uploads) |
| `concatMap` | Queue — run in order, wait for each |
| `exhaustMap` | Ignore new while current runs (login button) |
| `debounceTime` | Wait for pause (search input) |
| `distinctUntilChanged` | Skip duplicate emissions |
| `combineLatest` | Combine multiple streams, emit when any changes |
| `forkJoin` | Wait for all to complete (like Promise.all) |
| `takeUntilDestroyed` | Auto-unsubscribe on destroy (Angular 16+) |

```typescript
// Search with auto-cancel
this.searchControl.valueChanges.pipe(
  debounceTime(300),
  distinctUntilChanged(),
  switchMap(term => this.api.search(term)),
  takeUntilDestroyed(this.destroyRef)
).subscribe(results => this.results = results);
```

### Subject Types

| Type | Behavior |
|---|---|
| `Subject` | No initial value, no replay |
| `BehaviorSubject` | Holds current value, emits to new subscribers |
| `ReplaySubject(n)` | Replays last n values to new subscribers |
| `AsyncSubject` | Emits only last value on complete |

```typescript
// BehaviorSubject — most common for state
private userSubject = new BehaviorSubject<User | null>(null);
user$ = this.userSubject.asObservable();  // expose as Observable
```

---

## Routing

```typescript
const routes: Routes = [
  { path: 'home', component: HomeComponent },
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule),
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: 'home' }
];
```

### Guards (functional, Angular 15+)
```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? true : inject(Router).createUrlTree(['/login']);
};
```

### Route Resolvers
```typescript
export const userResolver: ResolveFn<User> = (route) =>
  inject(UserService).getUser(route.paramMap.get('id')!);
```

---

## Modules vs Standalone (Angular 14+)

### Standalone Components (default in Angular 17+)
```typescript
@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `...`
})
export class MyComponent {}
```
No NgModule needed. Bootstrap with `bootstrapApplication`.

---

## Forms

### Template-driven
```html
<form #f="ngForm" (ngSubmit)="submit(f)">
  <input name="email" ngModel required email>
</form>
```
Simple forms, less code, harder to unit test.

### Reactive Forms
```typescript
form = new FormGroup({
  email: new FormControl('', [Validators.required, Validators.email]),
  password: new FormControl('', Validators.minLength(8))
});
```
```html
<form [formGroup]="form" (ngSubmit)="submit()">
  <input formControlName="email">
  <span *ngIf="form.get('email')?.invalid">Invalid</span>
</form>
```
Better for: dynamic fields, complex validation, unit testing.

### Custom Validator
```typescript
function noSpaces(control: AbstractControl): ValidationErrors | null {
  return control.value?.includes(' ') ? { noSpaces: true } : null;
}
```

---

## HTTP & Interceptors

```typescript
// Functional interceptor (Angular 15+)
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();
  const authReq = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  return next(authReq);
};

// Register
bootstrapApplication(AppComponent, {
  providers: [provideHttpClient(withInterceptors([authInterceptor]))]
});
```

---

## NgRx (State Management)

```
Action → Reducer → Store → Selector → Component
                ↑
           Effect (side effects: HTTP)
```

```typescript
// Action
const loadUsers = createAction('[User] Load Users');
const loadUsersSuccess = createAction('[User] Load Users Success', props<{ users: User[] }>());

// Reducer
const userReducer = createReducer(
  initialState,
  on(loadUsersSuccess, (state, { users }) => ({ ...state, users }))
);

// Effect
loadUsers$ = createEffect(() => this.actions$.pipe(
  ofType(loadUsers),
  switchMap(() => this.api.getUsers().pipe(
    map(users => loadUsersSuccess({ users }))
  ))
));

// Selector
const selectUsers = createSelector(selectUserState, state => state.users);
```

---

## Performance

| Technique | How |
|---|---|
| `OnPush` CD | Reduce checks |
| `trackBy` in `*ngFor` | Avoid full list re-render on any change |
| Lazy loading | `loadChildren` / `loadComponent` → smaller initial bundle |
| Virtual scrolling | `CdkVirtualScrollViewport` for large lists |
| Signals + Zoneless | Fine-grained updates, no Zone overhead |
| `@defer` (Angular 17+) | Defer template block until visible/idle/interaction |

```html
<ng-container *ngFor="let item of items; trackBy: trackById">

@defer (on viewport) {
  <heavy-component />
}
```

---

## Angular 17+ New Control Flow

```html
@if (user) {
  <p>{{ user.name }}</p>
} @else {
  <p>Loading...</p>
}

@for (item of items; track item.id) {
  <li>{{ item.name }}</li>
} @empty {
  <li>No items</li>
}

@switch (status) {
  @case ('active') { <span>Active</span> }
  @default { <span>Inactive</span> }
}
```
Replaces `*ngIf`, `*ngFor`, `*ngSwitch`. Better performance, no import needed.

---

## Common Interview Questions

**Q: OnPush vs Default change detection?**
→ Default checks whole tree every cycle. OnPush checks only when input reference changes, event fires, or async pipe emits.

**Q: switchMap vs mergeMap vs concatMap vs exhaustMap?**
→ `switchMap` cancels previous (typeahead search). `mergeMap` runs all parallel (file uploads). `concatMap` queues in order. `exhaustMap` ignores new while processing (login submit).

**Q: Subject vs BehaviorSubject?**
→ `BehaviorSubject` holds current value and emits to new subscribers immediately. `Subject` doesn't — late subscribers miss past emissions.

**Q: How do you prevent memory leaks with RxJS?**
→ `takeUntilDestroyed(destroyRef)`, `async` pipe (auto-unsubscribes), `unsubscribe()` in `ngOnDestroy`.

**Q: ViewChild vs ContentChild?**
→ `ViewChild` accesses elements in the component's own template. `ContentChild` accesses projected `<ng-content>`.

**Q: How does lazy loading work?**
→ Route with `loadChildren`/`loadComponent` → separate chunk built by bundler. Loaded on navigation, not on app start.

**Q: What is Zone.js? Can Angular work without it?**
→ Zone.js patches async APIs to notify Angular when to run CD. Angular 18+ supports `zoneless` mode using Signals.

**Q: `providedIn: 'root'` vs providing in component?**
→ `'root'` = app-wide singleton. Component = new instance scoped to that component, destroyed with it.

**Q: Template-driven vs Reactive forms?**
→ Template-driven: simple, less code, harder to unit test. Reactive: explicit, testable, better for dynamic/complex forms.

**Q: What's the difference between `markForCheck` and `detectChanges`?**
→ `markForCheck` schedules the component for the next CD cycle. `detectChanges` runs CD immediately and synchronously.

---

## Pitfalls Checklist
- Mutating `@Input()` objects → OnPush won't detect → always return new reference
- Subscribing without `async` pipe or cleanup → memory leak
- Using `switchMap` for parallel calls → use `mergeMap` or `forkJoin`
- Forgetting `trackBy` on large `*ngFor` → full DOM re-render on any change
- Heavy logic in `ngDoCheck` → runs every single CD cycle
- Direct DOM manipulation → use `Renderer2`, not `document.querySelector`
- Circular DI → restructure or use `forwardRef`
