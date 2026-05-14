# Rendering Strategies — CSR, SSR, SSG, ISR

## CSR — Client-Side Rendering
Server sends a near-empty HTML shell + JS bundle. Browser downloads, executes bundle, React mounts, fetches data, renders.

**Pros:** Rich interactivity, cheap server (static hosting), instant subsequent navigations (SPA)
**Cons:** Slow First Contentful Paint — nothing visible until JS runs. Bad for SEO. Large bundles hurt mobile.
**Use when:** Dashboards, admin tools, authenticated apps where SEO doesn't matter.

---

## SSR — Server-Side Rendering
Server runs React, generates full HTML per request, sends it. Browser paints immediately. JS loads → React "hydrates" static HTML (attaches event listeners → interactive).

**Pros:** Fast FCP, great SEO, works without JS (graceful degradation)
**Cons:** Each request hits server — higher TTFB under load. Hydration mismatch bugs if server and client render different output.
**Use when:** Marketing pages, e-commerce, news sites — SEO or initial load speed is critical.

---

## SSG — Static Site Generation
HTML pre-built at build time, served from CDN. Zero server cost per request.
**Use when:** Blogs, docs, marketing pages with infrequent data changes.

---

## ISR — Incremental Static Regeneration (Next.js)
SSG pages re-generated in the background after a TTL. Combines SSG performance with fresh data.

```jsx
// Next.js App Router — revalidate every 60 seconds
export const revalidate = 60;
```

---

## Streaming SSR (React 18 + Next.js App Router)
Server streams HTML chunks as components resolve. Browser can paint above-the-fold content before deep data fetches finish.

```jsx
// Suspense boundary = streaming chunk boundary
<Suspense fallback={<Spinner />}>
  <SlowDataComponent />  {/* streams when ready */}
</Suspense>
```

---

## Hydration
The process of React attaching event listeners to server-rendered HTML.

- Server sends fully rendered HTML (fast paint)
- React loads, traverses the DOM, attaches listeners (interactive)
- **Mismatch bug:** if server and client render different HTML, React throws a warning and re-renders from scratch — negating SSR benefit

---

## React 18 Key Features Summary

| Feature | Description |
|---|---|
| Concurrent Mode | Renders interruptible — high-priority updates jump the queue |
| Automatic Batching | All updates batched everywhere, not just event handlers |
| `useTransition` | Mark updates as non-urgent |
| `useDeferredValue` | Defer slow child re-renders |
| `Suspense` for data | Pair with React Query or framework (Next.js) |
| `createRoot` | New rendering API — required for concurrent features |
| Streaming SSR | Stream HTML chunks, progressive hydration |

---

## Infinite Scroll — React Implementation

**Approach:** Intersection Observer API with a sentinel element.

```jsx
function InfiniteList() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const sentinelRef = useRef(null);

  // Fetch on page change
  useEffect(() => {
    fetchPage(page).then(({ data, total }) => {
      setItems(prev => [...prev, ...data]);
      setHasMore(items.length + data.length < total);
    });
  }, [page]);

  // Observer — watch sentinel
  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && hasMore) setPage(p => p + 1);
    });
    if (sentinelRef.current) observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [hasMore]);

  return (
    <>
      {items.map(item => <Card key={item.id} {...item} />)}
      <div ref={sentinelRef} />  {/* sentinel */}
    </>
  );
}
```

**Why Intersection Observer over scroll events?**
- Scroll events fire continuously → need throttling/debouncing
- Intersection Observer is event-driven — only fires when element crosses viewport boundary — more performant

---

## Follow-up Questions

**Q: What is hydration mismatch and how do you fix it?**
→ Server renders HTML with value X, client renders with value Y (e.g., `Date.now()`, random IDs, browser-only values). React re-renders from scratch — wastes SSR benefit. Fix: use `suppressHydrationWarning` for known mismatches, avoid browser-only values in initial render.

**Q: SSR vs SSG — when is SSR worth the server cost?**
→ When content is user-specific (personalised pages) or changes per request (inventory, prices). SSG serves identical content to all — can't personalise. Use SSR when data is dynamic per-request; SSG when data is the same for everyone.

**Q: What is `createRoot` and why is it required for React 18 features?**
→ `createRoot` enables concurrent rendering. The old `ReactDOM.render` is legacy mode — no concurrent features. If you don't migrate to `createRoot`, batching and transitions don't work.

**Q: What is progressive hydration?**
→ Hydrate parts of the page incrementally (most visible first), rather than the whole tree at once. Streaming SSR + Suspense boundaries enable this — each boundary hydrates when its JS chunk loads.

**Q: Can you use SSR without Next.js?**
→ Yes — `renderToString` or `renderToPipeableStream` (streaming) from `react-dom/server`. But you need to manually handle routing, data fetching, and hydration. Next.js/Remix handle all this out of the box.
