# State Management

## When to Use What

| Tool | When |
|---|---|
| `useState` / `useReducer` | Local component state |
| Context | Low-frequency global state (theme, auth, locale) |
| Redux Toolkit | Complex global state, many writers/readers, large teams |
| Zustand | Simpler global state, less boilerplate than Redux |
| React Query / SWR | Server state — fetching, caching, sync, invalidation |

---

## Context

```jsx
const ThemeContext = createContext('light');

function App() {
  return (
    <ThemeContext.Provider value="dark">
      <Child />
    </ThemeContext.Provider>
  );
}

function Child() {
  const theme = useContext(ThemeContext);  // no prop drilling
  return <div className={theme}>...</div>;
}
```

**Pitfall:** All consumers re-render when context value changes.
**Fix:** Split contexts by update frequency. Memoize the value object.

```jsx
// Bad — new object every render → all consumers re-render
<Ctx.Provider value={{ user, theme }}>

// Good — memoize to stabilise reference
const value = useMemo(() => ({ user, theme }), [user, theme]);
<Ctx.Provider value={value}>
```

---

## Redux Toolkit

```
dispatch(action) → reducer → new state → re-render
```

```jsx
// slice
const counterSlice = createSlice({
  name: 'counter',
  initialState: { value: 0 },
  reducers: {
    increment: state => { state.value += 1; },  // Immer handles immutability
    decrement: state => { state.value -= 1; },
  }
});

export const { increment, decrement } = counterSlice.actions;

// component
const count = useSelector(state => state.counter.value);
const dispatch = useDispatch();
dispatch(increment());
```

**createAsyncThunk** for async:
```jsx
const fetchUser = createAsyncThunk('user/fetch', async (id) => {
  const res = await api.getUser(id);
  return res.data;
});
```

---

## Redux-Saga

Separate middleware using generators for complex async flows.

```
dispatch(action) → Saga watches → calls API → dispatches success/failure action
```

```jsx
// Worker saga
function* fetchUserSaga(action) {
  try {
    const user = yield call(api.getUser, action.payload);
    yield put({ type: 'FETCH_USER_SUCCESS', payload: user });
  } catch (e) {
    yield put({ type: 'FETCH_USER_FAIL', error: e.message });
  }
}

// Watcher saga
function* watchFetchUser() {
  yield takeLatest('FETCH_USER', fetchUserSaga);  // cancels previous if fired again
}
```

| Effect | What it does |
|---|---|
| `call` | Invoke async function (awaits Promise) |
| `put` | Dispatch an action |
| `takeLatest` | Cancel previous, handle only latest |
| `takeEvery` | Handle every action in parallel |
| `all` | Run sagas in parallel (like Promise.all) |

**Saga vs Thunk:**
- Thunk = async logic inside action creator — simple fetch/post
- Saga = separate middleware — use for cancel, retry, race, sequential flows, complex orchestration

---

## Zustand (brief)

```jsx
const useStore = create((set) => ({
  count: 0,
  increment: () => set(state => ({ count: state.count + 1 })),
}));

function Counter() {
  const { count, increment } = useStore();
  return <button onClick={increment}>{count}</button>;
}
```

No Provider needed. Simpler than Redux for medium-complexity global state.

---

## React Query / TanStack Query

Manages server state — caching, background refetch, loading/error states.

```jsx
const { data, isLoading, error } = useQuery({
  queryKey: ['users', id],
  queryFn: () => api.getUser(id),
});

const mutation = useMutation({
  mutationFn: (data) => api.createUser(data),
  onSuccess: () => queryClient.invalidateQueries(['users']),
});
```

**Why not just useEffect + useState?**
- No deduplication of identical requests
- No caching — same data fetched multiple times
- No background refetch / stale-while-revalidate
- No out-of-the-box loading/error state

---

## Follow-up Questions

**Q: Context vs Redux — when to choose Redux?**
→ Context is fine for low-frequency updates (theme, auth). Redux when: many components read/write the same state, complex update logic, you need DevTools time-travel debugging, or team size makes unstructured Context hard to maintain.

**Q: What problem does Redux Saga solve that Thunk can't?**
→ Complex async orchestration: cancelling in-flight requests (`takeLatest`), retrying on failure, race conditions, sequential multi-step flows. Thunk is just a function — no built-in mechanism for these.

**Q: What is Immer in Redux Toolkit?**
→ Immer lets you write "mutating" code in reducers (`state.value += 1`) while actually producing a new immutable state object under the hood. No more spread operators in reducers.

**Q: What is server state vs client state?**
→ Server state lives on the backend — React Query manages it (fetch, cache, sync). Client state lives only in the browser — Redux/Zustand/useState manage it. Mixing them in one Redux store leads to complexity React Query was built to avoid.

**Q: What does `invalidateQueries` do in React Query?**
→ Marks cached data as stale and triggers a background refetch. Use after mutations to keep UI in sync with server.

**Q: Can Context replace Redux entirely?**
→ For small apps, yes. For large apps with frequent updates across many components: no — every context change re-renders all consumers, and there's no middleware/DevTools equivalent.
