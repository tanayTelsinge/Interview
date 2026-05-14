# Signals (Angular 16+)

## What is a Signal?
A reactive variable — Angular knows exactly when it changes and updates only that DOM node.

```typescript
title = signal('learn-angular');  // declare
title()                           // read (called as function)
title.set('new value')            // update
title.update(val => val + '!')    // update based on current value
```

## Signals vs Normal Variable
```typescript
title = 'hello'           // Angular doesn't know when this changes
title = signal('hello')   // Angular knows exactly when this changes
```

## Signals vs React useState

| | React useState | Angular Signal |
|---|---|---|
| Syntax | `const [t, setT] = useState('hello')` | `t = signal('hello')` |
| Read | `t` | `t()` |
| Update | `setT('new')` | `t.set('new')` |
| Component re-runs? | Yes, whole function re-runs | No |
| DOM update | Virtual DOM diffs, patches real DOM | Directly patches only that node |
| Diffing needed? | Yes | No |

## How each works

**React useState:**
1. State changes → entire component function re-runs
2. Virtual DOM diffs old vs new output
3. Only changed part updates in real DOM

**Angular Signal:**
1. Signal changes → Angular knows exactly which DOM node uses it
2. Directly updates that node — no diffing needed

> React is fast because diffing is cheap.
> Angular signals are faster because there's nothing to diff.

## Java Analogy
Like an `Observable` field — instead of polling for changes, it notifies subscribers when value changes.

## When to use
- Use signals for any reactive state in Angular 16+
- Replaces the older `BehaviorSubject` + RxJS pattern for simple state
- We'll cover signals in depth when we reach state management

## Signals vs BehaviorSubject
- 90% of cases — just holding and sharing state → use Signal
- 10% of cases — need RxJS operators on state (debounce, switchMap) → still BehaviorSubject
- Angular provides `toSignal(obs$)` and `toObservable(signal)` to convert between both
