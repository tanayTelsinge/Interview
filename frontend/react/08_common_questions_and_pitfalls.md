# Common Interview Questions & Pitfalls

## Quick-Fire Q&A

**Q: What is React's mental model?**
→ `UI = f(state)`. Re-render when state/props change. VDOM diffs and patches real DOM.

**Q: When does a component re-render?**
→ State changes, props change, parent re-renders (even with same props unless `React.memo`), context changes.

**Q: useEffect cleanup — when does it run?**
→ Before the next effect runs AND on component unmount — not just unmount.

**Q: Why not use index as key?**
→ If list reorders/inserts, index shifts → React incorrectly reuses components → stale state/UI bugs. Use stable unique IDs.

**Q: Difference between useCallback and useMemo?**
→ `useCallback` memoizes a function reference. `useMemo` memoizes a computed value.
`useCallback(fn, deps)` === `useMemo(() => fn, deps)`

**Q: What is prop drilling and how to fix it?**
→ Passing props through many layers that don't need them. Fix: Context, component composition, state management.

**Q: Controlled vs uncontrolled components?**
→ Controlled = value in React state. Uncontrolled = value in DOM (accessed via ref). Prefer controlled for validation/dynamic forms.

**Q: useEffect vs useLayoutEffect?**
→ `useEffect` fires async after paint. `useLayoutEffect` fires sync before paint — use for DOM measurements to prevent visual flicker.

**Q: How does React Fiber work?**
→ Fiber is the reconciler. Breaks rendering into units of work — can pause, resume, abort. Enables concurrent features (useTransition, Suspense).

**Q: Redux-Saga vs Redux Thunk?**
→ Thunk = async logic inside action creator (simple fetch). Saga = generator-based middleware — better for cancel, retry, race conditions, sequential flows.

**Q: What is `React.StrictMode`?**
→ Development-only wrapper that double-invokes render + effects to surface impure functions and side effects. No impact in production.

**Q: What happens when you call setState during render?**
→ React throws an error. State updates must happen in event handlers, effects, or async callbacks — not synchronously during render.

---

## Pitfalls Checklist

| Pitfall | Fix |
|---|---|
| Stale closure in effect | Add variable to deps array or use a ref |
| Missing cleanup | Always return cleanup fn for subscriptions, intervals, timers |
| Mutating state directly | `setItems([...items, newItem])` not `items.push(newItem)` |
| Object/array in deps → infinite loop | Memoize with useMemo or use primitives as deps |
| Index as key | Use stable unique IDs |
| Anonymous function as prop | `useCallback` to stabilise reference for memo'd children |
| Context causing excessive re-renders | Split contexts, memoize value object |
| setState after unmount | Abort fetch on cleanup / check mounted flag |
| Calling hooks conditionally | Hooks must always be called in same order — no conditionals |
| Forgetting to handle loading/error | Always handle both states in data-fetching components |

---

## Coding Questions

**Q: Write a debounce hook**
```jsx
function useDebounce(value, delay) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}

// Usage
const debouncedSearch = useDebounce(searchInput, 300);
```

**Q: Write a usePrevious hook**
```jsx
function usePrevious(value) {
  const ref = useRef();
  useEffect(() => { ref.current = value; });
  return ref.current;  // returns value from previous render
}
```

**Q: Counter with increment/decrement/reset**
```jsx
function Counter() {
  const [count, setCount] = useState(0);
  return (
    <>
      <button onClick={() => setCount(p => p - 1)}>-</button>
      <span>{count}</span>
      <button onClick={() => setCount(p => p + 1)}>+</button>
      <button onClick={() => setCount(0)}>Reset</button>
    </>
  );
}
```

**Q: Fetch data with loading/error state**
```jsx
function UserCard({ id }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    fetch(`/api/users/${id}`)
      .then(r => r.json())
      .then(data => { if (!cancelled) setUser(data); })
      .catch(err => { if (!cancelled) setError(err.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };  // cleanup — prevent setState after unmount
  }, [id]);

  if (loading) return <Spinner />;
  if (error)   return <p>Error: {error}</p>;
  return <div>{user.name}</div>;
}
```

**Q: sum(3)(4)(5) — currying**
```js
function sum(a) {
  return function(b) {
    return function(c) {
      return a + b + c;
    };
  };
}
sum(3)(4)(5);  // 12
```

**Q: sum(3, 4, 5) — variadic**
```js
function sum(...nums) {
  return nums.reduce((total, n) => total + n, 0);
}
sum(3, 4, 5);  // 12
```

---

## Accessibility (WCAG) — Quick Reference

```jsx
// Focus error element on validation failure
const errorRef = useRef(null);
useEffect(() => {
  if (hasError) errorRef.current?.focus();
}, [hasError]);
<div ref={errorRef} tabIndex={-1} role="alert">{errorMessage}</div>

// Accessible button
<button onClick={handleClick} aria-label="Close dialog">✕</button>

// aria-live for dynamic content
<div aria-live="polite">{loading ? 'Loading...' : data}</div>
```

| Practice | How |
|---|---|
| Focus management | `useRef` + `.focus()` on error/modal open |
| Announce updates | `aria-live="polite"` on dynamic regions |
| Keyboard nav | Use semantic elements (`button`, `a`) not `div` |
| Screen reader labels | `aria-label`, `aria-describedby` |
