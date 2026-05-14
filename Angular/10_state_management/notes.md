# 10 - State Management (NgRx)

## Why NgRx?
- When multiple components need to share and update the same data
- Avoids prop drilling through many components
- Predictable state changes (like Redux)

## Core Concepts
```
Component → dispatches Action → Reducer updates Store → Selector reads Store → Component
                                      ↕
                                   Effects (for API calls)
```

## Setup
```bash
ng add @ngrx/store
ng add @ngrx/effects
```

## 1. Action
```typescript
// counter.actions.ts
export const increment = createAction('[Counter] Increment');
export const decrement = createAction('[Counter] Decrement');
export const loadUsers = createAction('[User] Load Users');
export const loadUsersSuccess = createAction('[User] Load Users Success',
  props<{ users: User[] }>()
);
```

## 2. Reducer
```typescript
// counter.reducer.ts
export const counterReducer = createReducer(
  0,  // initial state
  on(increment, state => state + 1),
  on(decrement, state => state - 1)
);
```

## 3. Selector
```typescript
export const selectCount = (state: AppState) => state.count;
```

## 4. Component
```typescript
count$ = this.store.select(selectCount);

constructor(private store: Store<AppState>) {}

increment() { this.store.dispatch(increment()); }
```
```html
<p>{{ count$ | async }}</p>
```

## 5. Effects (for API calls)
```typescript
loadUsers$ = createEffect(() =>
  this.actions$.pipe(
    ofType(loadUsers),
    switchMap(() => this.userService.getUsers().pipe(
      map(users => loadUsersSuccess({ users }))
    ))
  )
);
```

## Practice
- [ ] Build a counter with NgRx (increment/decrement)
- [ ] Add a todo list managed by NgRx store
- [ ] Fetch data via Effects and display with async pipe
