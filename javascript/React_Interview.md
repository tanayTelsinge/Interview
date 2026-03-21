# React — Interview Cheatsheet (6 YOE Fullstack)

## Core Mental Model
React = **UI = f(state)**. Re-render when state/props change. Virtual DOM diffs and patches real DOM.

---

## React Lifecycle (Hooks equivalent)

```
MOUNT                    UPDATE                   UNMOUNT
─────                    ──────                   ───────
useState init            state/props change       cleanup fn runs
render (JSX)             render (JSX)             component removed
DOM updated              DOM updated
useLayoutEffect          useLayoutEffect
useEffect ✓              useEffect ✓
```

| Class lifecycle         | Hooks equivalent |
|---|---|
| `constructor`           | `useState` / `useReducer` initial value |
| `componentDidMount`     | `useEffect(() => {}, [])` |
| `componentDidUpdate`    | `useEffect(() => {}, [dep])` |
| `componentWillUnmount`  | `useEffect(() => { return () => cleanup }, [])` |
| `shouldComponentUpdate` | `React.memo` / `useMemo` |
| `getDerivedStateFromError` | Error Boundary (class only) |

**Key rule:** cleanup function runs before next effect AND on unmount — not just unmount.

---

## Hooks Deep Dive

### useState
```jsx
const [count, setCount] = useState(0);
setCount(prev => prev + 1);  // ← always use functional update when new state depends on old
```

### useEffect
```jsx
useEffect(() => {
  const sub = subscribe(id);
  return () => sub.unsubscribe();   // cleanup
}, [id]);                           // re-runs when id changes
```

| Dependency | Behavior |
|---|---|
| `[]` | Run once on mount |
| `[a, b]` | Run on mount + when a or b changes |
| no array | Run after every render |

**Pitfall:** Object/array deps cause infinite loop → memoize or use primitives.

### useRef
- Holds mutable value that **doesn't trigger re-render**
- DOM access: `<div ref={myRef}>` → `myRef.current`
- Store interval IDs, previous values, third-party instances

### useCallback / useMemo
```jsx
const fn   = useCallback(() => doSomething(a), [a]);  // memoize function reference
const val  = useMemo(() => expensiveCalc(a), [a]);    // memoize computed value
```
Only add when: passing to child with `React.memo`, or as dep in another hook.
**Don't over-memoize** — has its own cost.

### useContext
```jsx
const theme = useContext(ThemeContext);  // no prop drilling
```
Re-renders all consumers when context value changes → split contexts by update frequency.

### useReducer
```jsx
const [state, dispatch] = useReducer(reducer, initialState);
dispatch({ type: 'INCREMENT' });
```
Better than `useState` when: multiple sub-values, next state depends on previous, complex transitions.

### useLayoutEffect
- Fires **synchronously after DOM mutation, before paint**
- Use for: reading DOM layout, preventing flicker (tooltip positioning)
- Default to `useEffect`; reach for this only when visual glitch appears

### Custom Hooks
Rule: must start with `use`, can call other hooks. Extract reusable logic — keeps components clean.

```jsx
// API call with loading + error state
function useFetch(url) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  useEffect(() => {
    fetch(url)
      .then(r => r.json())
      .then(d => setData(d))
      .catch(e => setError(e))
      .finally(() => setLoading(false));
  }, [url]);
  return { data, loading, error };
}

// Sorting hook — memoizes sorted result, avoids recompute on every render
function useSortedData(data, key, order = 'asc') {
  const [sortKey, setSortKey] = useState(key);
  const [sortOrder, setSortOrder] = useState(order);
  const sorted = useMemo(() =>
    [...data].sort((a, b) =>
      sortOrder === 'asc' ? a[sortKey] > b[sortKey] ? 1 : -1
                          : a[sortKey] < b[sortKey] ? 1 : -1
    ), [data, sortKey, sortOrder]);
  return { sorted, setSortKey, setSortOrder };
}

// Auth flow hook
function useAuth() {
  const [user, setUser] = useState(null);
  const login  = useCallback((credentials) => authService.login(credentials).then(setUser), []);
  const logout = useCallback(() => { authService.logout(); setUser(null); }, []);
  return { user, login, logout };
}
```

---

## Rendering & Performance

### React.memo
```jsx
const Child = React.memo(({ value }) => <div>{value}</div>);
// Skips re-render if props haven't changed (shallow compare)
```

### Reconciliation (Diffing Algorithm)
React compares previous vs new Virtual DOM trees to compute the minimal DOM changes.

**3 rules React follows:**
1. **Different element type** → tear down old tree, mount new tree from scratch
2. **Same element type** → keep DOM node, update only changed attributes/props
3. **Lists** → use `key` to match old vs new items; without key, React diffs by index (wrong for reorders)

```
<div>          <div>
  <Counter />    <span />     ← type changed → Counter unmounts (state lost!)
</div>         </div>
```

**Why keys matter:**
```
// Without key: React diffs by position → wrong component gets state
// With key: React tracks identity across reorders
[A, B, C] → [B, A, C]  // without key: 3 updates. With key: 2 moves
```

**Fiber** = the reconciler engine. Breaks render into units of work → can pause, resume, abort. Enables Concurrent Mode (React 18).

### Batching (React 18)
React 18 batches all state updates — even in `setTimeout`, Promises, native events.
```jsx
// Both updates batched → single re-render
setTimeout(() => {
  setA(1);
  setB(2);
}, 1000);
```
Use `flushSync` to opt out.

### Code Splitting
```jsx
const LazyPage = React.lazy(() => import('./Page'));
<Suspense fallback={<Spinner />}><LazyPage /></Suspense>
```

### Transitions (React 18)
```jsx
const [isPending, startTransition] = useTransition();
startTransition(() => setFilter(input));  // non-urgent update
```
`useDeferredValue` — defer re-rendering a slow child.

---

## State Management

| Tool | When |
|---|---|
| `useState` / `useReducer` | Local component state |
| Context | Low-frequency global state (theme, auth, locale) |
| Redux Toolkit | Complex global state, many writers/readers |
| Zustand | Simpler global state, less boilerplate than Redux |
| React Query / SWR | Server state (fetching, caching, sync) |

**Redux Toolkit flow:**
```
dispatch(action) → reducer → new state → re-render
```
Use `createSlice`, `createAsyncThunk`, `RTK Query`.

**Redux-Saga** — middleware for complex async flows (used when async logic is too complex for Thunk):
```
dispatch(action) → Saga watches → calls API → dispatches success/failure action
```
```jsx
// Watcher saga
function* watchFetchUser() {
  yield takeLatest('FETCH_USER', fetchUserSaga);  // cancels previous if fired again
}

// Worker saga
function* fetchUserSaga(action) {
  try {
    const user = yield call(api.getUser, action.payload);
    yield put({ type: 'FETCH_USER_SUCCESS', payload: user });
  } catch (e) {
    yield put({ type: 'FETCH_USER_FAIL', error: e.message });
  }
}
```
| Effect | What it does |
|---|---|
| `call` | Invoke async function (returns Promise) |
| `put` | Dispatch an action |
| `takeLatest` | Cancel previous, handle only latest |
| `takeEvery` | Handle every action (parallel) |
| `all` | Run sagas in parallel (like Promise.all) |

**Saga vs Thunk:** Thunk = simple async in action creator. Saga = complex flows (cancel, retry, race, sequences).

---

## Component Patterns

### Compound Components
```jsx
<Tabs>
  <Tabs.List>
    <Tabs.Tab>One</Tabs.Tab>
  </Tabs.List>
  <Tabs.Panel>Content</Tabs.Panel>
</Tabs>
```
Share implicit state via Context internally.

### Render Props
```jsx
<Mouse render={({ x, y }) => <Cursor x={x} y={y} />} />
```
Replaced mostly by custom hooks.

### HOC (Higher Order Component)
```jsx
const withAuth = (Component) => (props) =>
  isLoggedIn ? <Component {...props} /> : <Redirect to="/login" />;
```

### Controlled vs Uncontrolled
- **Controlled:** form value in React state → single source of truth
- **Uncontrolled:** value in DOM → access via `ref` (use for file inputs, third-party libs)

---

## React 18 Key Features

| Feature | Description |
|---|---|
| Concurrent Mode | Renders interruptible — high-priority updates jump the queue |
| Automatic Batching | All updates batched, not just event handlers |
| `useTransition` | Mark updates as non-urgent |
| `useDeferredValue` | Defer slow child re-renders |
| `Suspense` for data | Pair with React Query or framework (Next.js) |
| `createRoot` | New rendering API — required for concurrent features |

---

## Accessibility (WCAG) with React

Key rule: **keyboard users and screen readers must get the same experience as mouse users.**

```jsx
// Focus error element on validation failure (WCAG 2.1 — focus management)
const errorRef = useRef(null);
useEffect(() => {
  if (hasError) errorRef.current?.focus();  // moves screen reader to error
}, [hasError]);
<div ref={errorRef} tabIndex={-1} role="alert">{errorMessage}</div>

// Accessible button (not a div)
<button onClick={handleClick} aria-label="Close dialog">✕</button>

// aria-live for dynamic content (e.g. loading states)
<div aria-live="polite">{loading ? 'Loading...' : data}</div>
```

| WCAG Practice | How |
|---|---|
| Focus management | `useRef` + `.focus()` on error/modal open |
| Announce updates | `aria-live="polite"` on dynamic regions |
| Keyboard nav | Use semantic elements (`button`, `a`) not `div` |
| Screen reader labels | `aria-label`, `aria-describedby` |

## Error Boundaries
```jsx
class ErrorBoundary extends React.Component {
  state = { hasError: false };
  static getDerivedStateFromError() { return { hasError: true }; }
  componentDidCatch(err, info) { logError(err, info); }
  render() {
    return this.state.hasError ? <Fallback /> : this.props.children;
  }
}
```
No hook equivalent yet — must be class component.

---

## Common Interview Questions

**Q: useEffect cleanup — when does it run?**
→ Before the next effect runs AND on component unmount.

**Q: Why not use index as key?**
→ If list reorders/inserts, index changes → React incorrectly reuses components → stale state/UI bugs.

**Q: Difference between useCallback and useMemo?**
→ `useCallback` memoizes a function. `useMemo` memoizes a computed value. `useCallback(fn, deps)` = `useMemo(() => fn, deps)`.

**Q: When does a component re-render?**
→ State changes, props change, parent re-renders (even with same props unless `React.memo`), context changes.

**Q: What is prop drilling and how to avoid?**
→ Passing props through many layers. Fix: Context, state management, component composition.

**Q: Controlled vs uncontrolled components?**
→ Controlled = state in React. Uncontrolled = state in DOM (ref). Prefer controlled for validation/dynamic forms.

**Q: What's the difference between useEffect and useLayoutEffect?**
→ `useEffect` fires async after paint. `useLayoutEffect` fires sync before paint — use for DOM measurements.

**Q: How does React Fiber work?**
→ Fiber is the reconciler. It breaks rendering into units of work, can pause/resume/abort — enables concurrent features.

**Q: Write sum(3,4,5) that works for any number of args?**
```js
function sum(...nums) {
  return nums.reduce((total, num) => total + num, 0);
}
sum(3, 4, 5);  // 12 — rest params collect all args into array
```

**Q: Redux-Saga vs Redux Thunk?**
→ Thunk = async logic inside action creator (simple fetch). Saga = separate middleware using generators — better for cancel, retry, race conditions, sequential flows.

---

## Pitfalls Checklist
- Stale closures in effects → add to deps array or use ref
- Missing cleanup → memory leaks (subscriptions, intervals)
- Mutating state directly → `setItems([...items, newItem])` not `items.push()`
- Heavy computation in render → move to `useMemo`
- Anonymous functions as props → new reference each render → child always re-renders
- Context causing unnecessary re-renders → split or memoize value
