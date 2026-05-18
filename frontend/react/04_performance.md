# React Performance

## When Does a Component Re-render?
1. Its own state changes
2. Its props change
3. Its parent re-renders (even with same props — unless `React.memo`)
4. Its context value changes

---

## React.memo
Wraps a component — skips re-render if props haven't changed (shallow compare).

```jsx
const Child = React.memo(({ value }) => <div>{value}</div>);
```

**Shallow compare** means: objects/arrays/functions are compared by reference, not deep equality.
So passing a new object `{}` or inline function `() => {}` every render still causes re-render.

**Fix:** use `useMemo` for objects, `useCallback` for functions passed as props.

---

## useMemo + useCallback (Performance angle)

```jsx
// Without — new object reference every render → Child always re-renders
<Child config={{ theme: 'dark' }} />

// With useMemo — same reference if theme didn't change
const config = useMemo(() => ({ theme: 'dark' }), []);
<Child config={config} />

// Without — new function every render
<Child onClick={() => handleClick(id)} />

// With useCallback — same reference
const onClick = useCallback(() => handleClick(id), [id]);
<Child onClick={onClick} />
```

**Rule:** Only memoize when you can measure the benefit. Premature memoization adds complexity for no gain.

---

## Batching (React 18)
React 18 batches all state updates — even in `setTimeout`, Promises, native events.

```jsx
// React 17: 2 renders (inside setTimeout, no batching)
// React 18: 1 render (automatic batching everywhere)
setTimeout(() => {
  setA(1);
  setB(2);  // batched → single re-render
}, 1000);
```

Use `flushSync` to opt out (force immediate re-render):
```jsx
import { flushSync } from 'react-dom';
flushSync(() => setCount(1));  // re-render happens before next line
```

---

## Code Splitting
Split the bundle — load components only when needed.

```jsx
const LazyPage = React.lazy(() => import('./Page'));

<Suspense fallback={<Spinner />}>
  <LazyPage />
</Suspense>
```

- `React.lazy` works with dynamic `import()`
- `Suspense` shows fallback while the chunk loads
- Use at route level for maximum impact

---

## useTransition (React 18)
Mark a state update as non-urgent — React keeps the UI responsive while it processes.

```jsx
const [isPending, startTransition] = useTransition();

startTransition(() => {
  setFilter(input);  // expensive re-render deferred
});

{isPending && <Spinner />}
```

**Use case:** filtering/sorting a large list while the user types — input stays responsive.

---

## useDeferredValue (React 18)
Defer re-rendering a slow child that depends on a value.

```jsx
const deferredQuery = useDeferredValue(query);
// deferredQuery lags behind query — React renders with old value while computing new
<HeavyList filter={deferredQuery} />
```

**vs useTransition:** `useTransition` wraps the setter. `useDeferredValue` wraps the value — useful when you don't control the setter (e.g., third-party component).

---

## Virtualization
For very long lists — only render what's visible in the viewport.

Libraries: `react-window`, `react-virtual`, `TanStack Virtual`

```jsx
import { FixedSizeList } from 'react-window';

<FixedSizeList height={600} itemCount={10000} itemSize={50}>
  {({ index, style }) => <div style={style}>Row {index}</div>}
</FixedSizeList>
```

---

## Follow-up Questions

**Q: React.memo does shallow compare — what if a prop is an object?**
→ Shallow compare checks reference equality. A new object `{}` each render ≠ previous object, even if contents are same. Use `useMemo` to stabilise the reference.

**Q: Does React.memo prevent re-render if context changes?**
→ No. `React.memo` only prevents re-render due to prop changes. If the component consumes a context that changes, it still re-renders.

**Q: What's the difference between useTransition and debouncing?**
→ Debouncing delays the update — nothing happens for X ms. `useTransition` starts the update immediately but at lower priority — the UI stays interactive while React works on it in the background.

**Q: When should you NOT use React.memo?**
→ When the component is cheap to render, or when props change on almost every render anyway — the comparison cost outweighs the savings.

**Q: What is concurrent rendering?**
→ React can pause, resume, or abandon renders mid-way. High-priority updates (user input) can interrupt low-priority renders (data processing). Enabled by `createRoot` in React 18.

**Q: flushSync use case?**
→ When you need the DOM updated before the next line runs — e.g., scroll to a newly added item, or third-party library that reads the DOM synchronously.
