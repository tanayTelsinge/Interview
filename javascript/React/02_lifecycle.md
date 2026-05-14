# React Lifecycle

## Lifecycle Phases (Hooks equivalent)

```
MOUNT                    UPDATE                   UNMOUNT
─────                    ──────                   ───────
useState init            state/props change       cleanup fn runs
render (JSX)             render (JSX)             component removed
DOM updated              DOM updated
useLayoutEffect          useLayoutEffect
useEffect ✓              useEffect ✓
```

---

## Class Lifecycle → Hooks Mapping

| Class Method | Hook Equivalent |
|---|---|
| `constructor` | `useState` / `useReducer` initial value |
| `componentDidMount` | `useEffect(() => {}, [])` |
| `componentDidUpdate` | `useEffect(() => {}, [dep])` |
| `componentWillUnmount` | `useEffect(() => { return () => cleanup }, [])` |
| `shouldComponentUpdate` | `React.memo` / `useMemo` |
| `getDerivedStateFromError` | Error Boundary (class only — no hook equivalent) |

---

## Key Rules

- **Cleanup runs before next effect AND on unmount** — not just unmount.
- `useEffect` with `[]` = run once on mount (componentDidMount equivalent).
- `useEffect` with `[dep]` = run on mount + whenever dep changes (componentDidUpdate).
- `useEffect` with no array = run after every render.

```jsx
useEffect(() => {
  const sub = subscribe(id);
  return () => sub.unsubscribe();  // cleanup: runs before next effect or on unmount
}, [id]);
```

---

## useLayoutEffect vs useEffect

| | useEffect | useLayoutEffect |
|---|---|---|
| Timing | Async, after paint | Sync, after DOM mutation, before paint |
| Use for | Data fetching, subscriptions | DOM measurements, prevent visual flicker |
| Default | Yes | Only when visual glitch appears |

```jsx
// useLayoutEffect: read DOM before user sees it (tooltip positioning)
useLayoutEffect(() => {
  const rect = ref.current.getBoundingClientRect();
  setPosition({ top: rect.bottom });
}, []);
```

---

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
**No hook equivalent** — must be a class component. Wraps subtrees to catch render errors.

---

## Follow-up Questions

**Q: When does cleanup run exactly?**
→ Before the next effect fires (on re-render when deps change) AND when the component unmounts. Not just unmount.

**Q: Can you replicate componentDidMount exactly with hooks?**
→ `useEffect(() => {}, [])` is close but fires after paint. `componentDidMount` also fired after paint, so functionally equivalent. `useLayoutEffect` fires before paint if you need that.

**Q: Why is there no hook for getDerivedStateFromError?**
→ Error boundaries need to catch errors during rendering. React's render phase doesn't have a hook mechanism for this yet — it's on the roadmap.

**Q: What happens if you setState inside componentDidUpdate without a condition?**
→ Infinite loop — setState triggers update, update triggers componentDidUpdate, repeat. Always guard with a condition.

**Q: useEffect dependency array omitted vs empty array — difference?**
→ Omitted `[]` → runs after every render. Empty `[]` → runs once on mount only. Easy to mix up, lint rules catch it.
