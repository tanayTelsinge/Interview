# React Hooks

## useState
```jsx
const [count, setCount] = useState(0);
setCount(prev => prev + 1);  // always use functional update when new state depends on old
```

**Pitfalls:**
- State updates are async — don't read state right after setting it
- Setting state with same value (Object.is) skips re-render
- Objects/arrays: always create a new reference — never mutate directly

```jsx
// Wrong
items.push(newItem);
setItems(items);

// Correct
setItems([...items, newItem]);
```

---

## useEffect

```jsx
useEffect(() => {
  const sub = subscribe(id);
  return () => sub.unsubscribe();  // cleanup
}, [id]);
```

| Dependency | Behaviour |
|---|---|
| `[]` | Run once on mount |
| `[a, b]` | Run on mount + when a or b changes |
| omitted | Run after every render |

**Pitfall:** Object/array in dep array causes infinite loop — each render creates a new reference.
Fix: use primitives in deps, or wrap object in `useMemo`.

---

## useRef

**3 use cases:**
1. Access a DOM element directly
2. Store a value that persists across renders without causing re-render
3. Hold a mutable value (interval ID, previous value)

**Key rule: `useRef` does NOT cause re-render. `useState` DOES.**

```jsx
const inputRef = useRef();
<input ref={inputRef} />
inputRef.current.focus();
```

**Practical — Stopwatch (persisting interval ID):**
```jsx
function Stopwatch() {
  const [time, setTime] = useState(0);
  const intervalRef = useRef(null);

  const start = () => {
    intervalRef.current = setInterval(() => setTime(p => p + 1), 1000);
  };
  const stop = () => clearInterval(intervalRef.current);

  return (
    <>
      <p>{time}s</p>
      <button onClick={start}>Start</button>
      <button onClick={stop}>Stop</button>
    </>
  );
}
```
Without `useRef`, interval ID would be lost between renders.

---

## useCallback

**Problem:** Every render recreates functions. Passing a new function reference to a `React.memo` child triggers its re-render even when nothing logically changed.

**`useCallback` caches the function reference** across renders (unless deps change).

```jsx
// Without — child re-renders every time Parent does
const handleClick = () => console.log("clicked");

// With — child skips re-render
const handleClick = useCallback(() => console.log("clicked"), []);

// With dependency
const handleSearch = useCallback(() => fetchResults(query), [query]);
```

**When to use:**
- Passing callback as prop to `React.memo` child
- Function used as dep in another `useEffect`

**Mental model:** `useCallback` is to functions what `useMemo` is to values.

---

## useMemo

Memoizes a computed value — recomputes only when deps change.

```jsx
const sorted = useMemo(() => expensiveSort(list), [list]);
const filtered = useMemo(() => list.filter(x => x.active), [list]);
```

**When to use:**
- Expensive computation (sorting/filtering large arrays)
- Referential stability for objects/arrays passed as props to memo'd children

**Don't over-memoize** — `useMemo` itself has overhead. Only use when profiling shows a problem or referential equality matters.

---

## useContext

```jsx
const theme = useContext(ThemeContext);
```

- Eliminates prop drilling
- All consumers re-render when context value changes
- Split contexts by update frequency to avoid unnecessary re-renders

```jsx
// Split — ThemeContext rarely changes, UserContext often changes
<ThemeContext.Provider value={theme}>
  <UserContext.Provider value={user}>
    <App />
  </UserContext.Provider>
</ThemeContext.Provider>
```

---

## useReducer

```jsx
const [state, dispatch] = useReducer(reducer, initialState);
dispatch({ type: 'INCREMENT' });
```

**Better than `useState` when:**
- Multiple related sub-values in one state object
- Next state depends on previous
- Complex transitions / many actions

```jsx
function reducer(state, action) {
  switch (action.type) {
    case 'INCREMENT': return { ...state, count: state.count + 1 };
    case 'RESET':     return initialState;
    default:          return state;
  }
}
```

---

## Custom Hooks

**Rule:** must start with `use`, can call other hooks. Extract reusable stateful logic.

```jsx
// useFetch — API call with loading + error state
function useFetch(url) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(url)
      .then(r => r.json())
      .then(setData)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [url]);

  return { data, loading, error };
}

// useAuth
function useAuth() {
  const [user, setUser] = useState(null);
  const login  = useCallback((creds) => authService.login(creds).then(setUser), []);
  const logout = useCallback(() => { authService.logout(); setUser(null); }, []);
  return { user, login, logout };
}
```

---

## Follow-up Questions

**Q: Why use functional update `setCount(prev => prev + 1)` instead of `setCount(count + 1)`?**
→ `count` in a closure may be stale (captured from a previous render). Functional form always gets the latest state — critical inside async callbacks and intervals.

**Q: Can you call hooks conditionally?**
→ No. Hooks must be called in the same order every render (Rules of Hooks). React tracks hooks by call order. Conditionals break the order and corrupt state mapping.

**Q: What's the difference between `useRef` and a module-level variable?**
→ Module-level variable is shared across all instances of the component. `useRef` is per-instance — each mounted component gets its own ref.

**Q: When would you pick `useReducer` over `useState`?**
→ When you have 3+ related state fields, or when the next state depends on the previous in complex ways. Also makes testing easier — reducer is a pure function.

**Q: Can two components share a custom hook's state?**
→ No. Each component call to a custom hook gets its own isolated state. To share state, lift it up or use Context/state management.

**Q: `useCallback` with empty deps `[]` — when does the function ever update?**
→ It doesn't. If you reference stale state/props inside, you'll get a stale closure. Solution: add the dep, or use a ref to hold the latest value.

**Q: What is a stale closure in useEffect?**
→ The effect captures variables from the render it was created in. If those variables change but the effect doesn't re-run (missing dep), it uses old values.
```jsx
// Bug: count is always 0 inside the interval
useEffect(() => {
  const id = setInterval(() => console.log(count), 1000);
  return () => clearInterval(id);
}, []);  // missing count in deps

// Fix: use functional update or add count to deps
```
