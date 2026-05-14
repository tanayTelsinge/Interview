# Component Patterns

## Controlled vs Uncontrolled Components

### Controlled — state lives in React
```jsx
function App() {
  const [name, setName] = useState("");
  return <input value={name} onChange={(e) => setName(e.target.value)} />;
}
```

### Uncontrolled — state lives in DOM, access via ref
```jsx
function App() {
  const inputRef = useRef();
  const handleSubmit = () => alert(inputRef.current.value);
  return (
    <>
      <input ref={inputRef} />
      <button onClick={handleSubmit}>Submit</button>
    </>
  );
}
```

| Use Controlled when | Use Uncontrolled when |
|---|---|
| Validation | Simple one-off form |
| Dynamic UI (enable/disable button) | File input |
| Real-time input handling | Integrating third-party DOM lib |
| Complex form logic | Performance (very large forms) |

---

## React.memo
Skips re-render when props haven't changed (shallow compare).

```jsx
const Child = React.memo(({ value, onClick }) => {
  return <button onClick={onClick}>{value}</button>;
});
```

Must pair with `useCallback`/`useMemo` for function/object props — otherwise shallow compare always fails.

---

## HOC — Higher Order Component
A function that takes a component and returns an enhanced component.

```jsx
const withAuth = (Component) => (props) =>
  isLoggedIn ? <Component {...props} /> : <Redirect to="/login" />;

const ProtectedPage = withAuth(Dashboard);
```

**Drawbacks:** prop name collisions, hard to trace in DevTools. Custom hooks replaced most HOC use cases.

---

## Render Props
Pass a function as a prop — the component calls it to decide what to render.

```jsx
<Mouse render={({ x, y }) => <Cursor x={x} y={y} />} />
```

Mostly replaced by custom hooks — hooks are cleaner and composable.

---

## Compound Components
Components that share implicit state via Context internally, used together as a group.

```jsx
<Tabs>
  <Tabs.List>
    <Tabs.Tab id="one">One</Tabs.Tab>
    <Tabs.Tab id="two">Two</Tabs.Tab>
  </Tabs.List>
  <Tabs.Panel id="one">Content One</Tabs.Panel>
  <Tabs.Panel id="two">Content Two</Tabs.Panel>
</Tabs>
```

Implementation uses `createContext` internally — `Tabs` holds active tab state, children consume it via context.

**Use when:** a group of components need to share state without the parent managing it explicitly (Accordion, Dropdown, Tabs, Select).

---

## Prop Drilling vs Alternatives

**Prop drilling:** passing props through multiple layers that don't need them.

```
App → Layout → Page → Section → Card → Button   ← Button needs user
```

**Solutions:**
1. **Context** — for global/shared state (auth, theme)
2. **Component composition** — pass children instead of props
3. **State management** — Redux / Zustand for complex cases

**Composition example (avoids drilling):**
```jsx
// Instead of drilling `user` through Layout → Page → Header
function App() {
  return (
    <Layout header={<Header user={user} />}>
      <Page />
    </Layout>
  );
}
```

---

## Portals
Render a component outside its parent DOM node (useful for modals, tooltips).

```jsx
ReactDOM.createPortal(<Modal />, document.getElementById('modal-root'));
```

The component still exists in the React tree (events bubble normally), but it renders to a different DOM node.

---

## Follow-up Questions

**Q: HOC vs Custom Hook — when to use which?**
→ Custom Hook: when you want to share stateful logic without affecting the component tree. HOC: when you need to wrap a component and inject props, or handle cross-cutting concerns like auth guards. In 2024+, prefer hooks — less magic, easier to debug.

**Q: Can a controlled component become uncontrolled?**
→ Yes, if you pass `undefined` or `null` as `value`. React warns about this. Always initialise with an empty string, not `undefined`.

**Q: What is the children prop pattern?**
→ Passing JSX as `props.children` — enables flexible composition without prop drilling.
```jsx
<Card>
  <Title>Hello</Title>  {/* this is children */}
</Card>
```

**Q: How do compound components share state without prop drilling?**
→ The parent (e.g., `<Tabs>`) holds state and provides it via `createContext`. Child components (`<Tabs.Tab>`, `<Tabs.Panel>`) consume it with `useContext`. From the consumer's perspective, no props are passed.

**Q: What problem does a Portal solve that CSS can't?**
→ CSS `overflow: hidden` or `z-index` stacking contexts on ancestors clip or hide content. A Portal renders the modal at the top of the DOM (`#modal-root`) — outside those stacking contexts — so `z-index` and overflow work correctly.

**Q: What is a render prop vs children as function?**
→ Same pattern, different prop name. Render prop: `<Mouse render={fn} />`. Children as function: `<Mouse>{fn}</Mouse>`. Both pass a function the component calls. Children-as-function is more idiomatic today.
