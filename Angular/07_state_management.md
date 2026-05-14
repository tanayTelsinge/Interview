# State Management

## When Does State Management Matter?

- Two unrelated components need the same data
- Navigating away and back must preserve data
- Multiple components mutate shared state (cart, auth, notifications)
- Prop drilling becomes unmanageable (passing data through 4+ layers)

---

## 3 Approaches in Angular

| Approach | Best for | Complexity |
|---|---|---|
| Service + BehaviorSubject | Small-medium apps, simple shared state | Low |
| Signals (Angular 16+) | Modern Angular, reactive UI state | Low-Medium |
| NgRx | Large apps, complex async flows, time-travel debugging | High |

---

## Approach 1: Service + BehaviorSubject

The most common pattern in mid-sized Angular apps:

```typescript
@Injectable({ providedIn: 'root' })
export class CartService {
  private items$ = new BehaviorSubject<CartItem[]>([]);

  // Expose as read-only observable
  cart$ = this.items$.asObservable();
  itemCount$ = this.items$.pipe(map(items => items.length));

  addItem(item: CartItem) {
    this.items$.next([...this.items$.getValue(), item]);
  }

  removeItem(id: string) {
    this.items$.next(this.items$.getValue().filter(i => i.id !== id));
  }
}
```

```typescript
// Any component — no prop drilling
export class HeaderComponent {
  itemCount$ = inject(CartService).itemCount$;
}
```

```html
<span>{{ itemCount$ | async }}</span>
```

---

## Approach 2: Signals (Angular 16+)

Signals are Angular's built-in reactive primitive — no RxJS needed for simple state:

```typescript
@Injectable({ providedIn: 'root' })
export class CartService {
  private items = signal<CartItem[]>([]);

  // Computed signal — auto-updates when items changes
  itemCount = computed(() => this.items().length);
  total = computed(() => this.items().reduce((sum, i) => sum + i.price, 0));

  addItem(item: CartItem) {
    this.items.update(current => [...current, item]);
  }
}
```

```typescript
// Component — no subscribe, no async pipe
export class HeaderComponent {
  private cartService = inject(CartService);
  itemCount = this.cartService.itemCount;  // computed signal
}
```

```html
<span>{{ itemCount() }}</span>
```

**BehaviorSubject vs Signal:**
```
BehaviorSubject → RxJS, works with pipe operators, interops with HTTP
Signal         → Angular native, no subscribe, automatic DOM update, cleaner syntax
```

Use Signals for UI state. Use BehaviorSubject when you need RxJS operators (debounce, switchMap on state changes).

---

## Approach 3: NgRx (Redux Pattern)

### Data Flow

```
Component
  → dispatches Action
    → Reducer updates Store (pure function, no side effects)
      → Selector reads Store
        → Component re-renders
              ↕
           Effects (handle side effects: API calls, routing)
```

### 1. Define State + Actions

```typescript
// user.actions.ts
export const loadUsers = createAction('[User] Load');
export const loadUsersSuccess = createAction('[User] Load Success',
  props<{ users: User[] }>()
);
export const loadUsersFailure = createAction('[User] Load Failure',
  props<{ error: string }>()
);
```

### 2. Reducer

```typescript
// user.reducer.ts
interface UserState { users: User[]; loading: boolean; error: string | null; }
const initialState: UserState = { users: [], loading: false, error: null };

export const userReducer = createReducer(
  initialState,
  on(loadUsers, state => ({ ...state, loading: true })),
  on(loadUsersSuccess, (state, { users }) => ({ ...state, users, loading: false })),
  on(loadUsersFailure, (state, { error }) => ({ ...state, error, loading: false }))
);
```

### 3. Selectors

```typescript
// user.selectors.ts
const selectUserState = (state: AppState) => state.users;
export const selectAllUsers = createSelector(selectUserState, s => s.users);
export const selectLoading = createSelector(selectUserState, s => s.loading);
```

### 4. Effects (API calls)

```typescript
// user.effects.ts
loadUsers$ = createEffect(() =>
  this.actions$.pipe(
    ofType(loadUsers),
    switchMap(() =>
      this.userService.getUsers().pipe(
        map(users => loadUsersSuccess({ users })),
        catchError(err => of(loadUsersFailure({ error: err.message })))
      )
    )
  )
);
```

### 5. Component

```typescript
export class UsersComponent {
  users$ = this.store.select(selectAllUsers);
  loading$ = this.store.select(selectLoading);

  constructor(private store: Store) {}

  ngOnInit() {
    this.store.dispatch(loadUsers());
  }
}
```

```html
<div *ngIf="loading$ | async">Loading...</div>
<li *ngFor="let user of users$ | async">{{ user.name }}</li>
```

---

## When to Use NgRx?

**Use NgRx when:**
- Multiple unrelated components modify the same data with complex interactions
- Need time-travel debugging (Redux DevTools)
- State transitions are complex (loading/error/success for many resources)
- Team is large and needs enforced data flow patterns

**Don't use NgRx when:**
- Service + BehaviorSubject or Signals would solve it in 30 lines
- Small/medium app where the overhead isn't justified

> Rule of thumb: start with Services + Signals. Add NgRx only when shared state has multiple async mutations that are hard to reason about.

---

## Follow-up Questions

**Q: How is NgRx different from a shared service with BehaviorSubject?**
→ Same concept (central store), but NgRx enforces unidirectional data flow — state can only change through Actions → Reducers. No component can mutate state directly. Adds boilerplate but gains predictability, DevTools debugging, and time-travel.

**Q: What is a Selector and why use it?**
→ A memoized function that reads a slice of the store. Memoized = only re-computes when the relevant slice changes, not on every store update. Compose selectors to avoid duplicating state-reading logic across components.

**Q: What is the difference between Actions and Effects?**
→ Actions are plain objects describing what happened (`loadUsers`). Effects listen for specific actions, perform side effects (API calls), and dispatch new actions (`loadUsersSuccess`). Reducers never touch async code — that's Effects' job.

**Q: Can you use Signals and NgRx together?**
→ Yes. NgRx 17+ has `selectSignal()` — reads store state as a Signal. Signals can also be converted to Observables with `toObservable()` for use in Effects.

**Q: Where do you handle loading and error states?**
→ In the store state. Standard pattern: `{ data, loading: boolean, error: string | null }`. Dispatch separate success/failure actions to transition between these states.
