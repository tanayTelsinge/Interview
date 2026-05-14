# Virtual DOM, JSX & Reconciliation

## What is the DOM?
Browser's in-memory tree representation of HTML. Every tag = a node.
Direct DOM manipulation is expensive — touching a node can trigger reflow + repaint.

---

## What is the Virtual DOM?
A lightweight JS object tree that mirrors the real DOM. React keeps it in memory as a staging area.

When state changes:
1. React builds a new VDOM tree
2. Diffs new tree vs previous snapshot (reconciliation)
3. Only the minimal real DOM operations needed are applied (patching)

---

## What happens internally when you write `<div>` in React?
1. JSX compiles to `React.createElement('div', props, children)` — a plain JS object (VDOM node)
2. React adds this node to its Virtual DOM tree
3. Reconciliation diffs new VDOM vs previous
4. Only if this `<div>` is new/changed, React calls `document.createElement('div')` and inserts it into the real DOM

JSX never touches the real DOM directly — it always goes through VDOM diff first.

---

## Reconciliation (Diffing Algorithm)
React compares previous vs new VDOM trees to compute minimal DOM changes.

**3 rules React follows:**

| Rule | Behaviour |
|---|---|
| Different element type | Tear down old tree, mount new tree from scratch |
| Same element type | Keep DOM node, update only changed attributes/props |
| Lists | Use `key` to match old vs new items |

```
<div>          <div>
  <Counter />    <span />   ← type changed → Counter unmounts, state lost!
</div>         </div>
```

**Why keys matter:**
```
// Without key: React diffs by index → wrong component gets state on reorder
// With key: React tracks identity across reorders
[A, B, C] → [B, A, C]  // without key: 3 updates. With key: 2 moves
```
Never use array index as key if list can reorder/insert — stale state/UI bugs.

---

## React Fiber
Fiber is the reconciler engine (introduced React 16).

**Classic reconciler (pre-16):** synchronous — once started, ran to completion, blocking the main thread.

**Fiber introduces:**
1. **Incremental rendering** — work split into small units ("fibers"). React can pause mid-render, yield to browser (for input/paint), then resume.
2. **Priority lanes** — user keypress = urgent (sync). Background data fetch = low priority. High-priority work flushes first.

This powers Concurrent Mode, `useTransition`, `Suspense`, and streaming SSR in React 18+.

---

## Entry Point of a React App
```
index.html  →  has <div id="root">
main.tsx    →  ReactDOM.createRoot(document.getElementById('root')).render(<App />)
App.tsx     →  root component, all other components branch from here
```

---

## Follow-up Questions

**Q: If Virtual DOM is just a JS object, how is it faster than real DOM?**
→ JS object operations are cheap. The real cost is reading/writing to the actual DOM (reflow, repaint). VDOM lets React batch and minimise those writes.

**Q: Does React always update the real DOM?**
→ No. If the diff shows nothing changed, React skips the DOM write entirely.

**Q: What does React Fiber enable that the old reconciler couldn't?**
→ Interruptible rendering. Old reconciler was all-or-nothing. Fiber can pause work, handle a user event, then resume — keeping the UI responsive under heavy load.

**Q: Why does changing element type unmount the whole subtree?**
→ React assumes a different type means fundamentally different structure — cheaper to rebuild than patch. State is lost because the component instance is destroyed.

**Q: Key as index — when is it actually okay?**
→ Only when the list is static (never reorders, never inserts/deletes). If items can change position, use a stable unique ID.
