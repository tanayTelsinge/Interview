# LRU Cache — UML Class Diagram

> Matches the actual implementation in 05_Problems/lru_cache/code/
> This is what you'd sketch on a whiteboard in the first 10 minutes.

---

## Core Insight (say this first in interview)

> O(1) get + O(1) put + O(1) eviction requires **two data structures working together**:
> - `HashMap<K, Node>` → O(1) lookup by key
> - **Doubly Linked List** → O(1) move-to-front and remove-from-tail (no index shifting)

---

## Class Diagram

```
┌─────────────────────────────┐
│         Node<K, V>           │
├─────────────────────────────┤
│ - key: K                     │
│ - value: V                   │
│ - prev: Node<K, V>           │
│ - next: Node<K, V>           │
└─────────────────────────────┘
         ◆ (composition)
         │  used by
         ▼
┌──────────────────────────────────────┐
│          DoublyLinkedList<K, V>       │
├──────────────────────────────────────┤
│ - head: Node<K, V>   (dummy / MRU)   │
│ - tail: Node<K, V>   (dummy / LRU)   │
│ - size: int                          │
├──────────────────────────────────────┤
│ + addToFront(node): void             │  ← newly used → MRU end
│ + remove(node): void                 │  ← detach node from list
│ + removeLast(): Node<K, V>           │  ← evict LRU node
│ + getSize(): int                     │
└──────────────────────────────────────┘
         ◆ (composition)
         │  owns
         ▼
┌──────────────────────────────────────┐
│            LRUCache<K, V>             │
├──────────────────────────────────────┤
│ - capacity: int                      │
│ - map: HashMap<K, Node<K,V>>         │  ← O(1) lookup
│ - list: DoublyLinkedList<K, V>  ◆    │  ← O(1) order tracking
├──────────────────────────────────────┤
│ + get(key: K): V                     │  ← O(1)
│ + put(key: K, value: V): void        │  ← O(1)
└──────────────────────────────────────┘
```

---

## Sequence: get(key)

```
caller          LRUCache           HashMap           DoublyLinkedList
  │                │                  │                     │
  │── get(key) ──▶ │                  │                     │
  │                │── map.get(key) ─▶│                     │
  │                │◀── node ─────────│                     │
  │                │── list.remove(node) ──────────────────▶│
  │                │── list.addToFront(node) ───────────────▶│
  │                │                                         │
  │◀─ value ───────│
```

---

## Sequence: put(key, value) — eviction path

```
caller          LRUCache           HashMap           DoublyLinkedList
  │                │                  │                     │
  │── put(k,v) ──▶ │                  │                     │
  │                │── map.get(k) ───▶│                     │
  │                │◀── null ─────────│  (key not present)  │
  │                │── [capacity full?]                      │
  │                │── list.removeLast() ───────────────────▶│
  │                │◀── evictedNode ──────────────────────── │
  │                │── map.remove(evictedNode.key) ─────────▶│
  │                │── newNode = new Node(k, v)              │
  │                │── list.addToFront(newNode) ────────────▶│
  │                │── map.put(k, newNode) ──────────────────▶│
```

---

## Relationships Summary

| From → To | Type | Why |
|---|---|---|
| LRUCache → DoublyLinkedList | **Composition ◆** | List cannot exist independently; created inside LRUCache |
| LRUCache → HashMap | **Composition ◆** | Map is an internal implementation detail |
| DoublyLinkedList → Node | **Composition ◆** | Nodes have no meaning outside the list |

---

## Dummy Head/Tail Pattern (critical implementation detail)

```
head (dummy) ↔ [MRU node] ↔ ... ↔ [LRU node] ↔ tail (dummy)
```

- Sentinel nodes eliminate null checks on every insert/remove.
- `addToFront` always inserts after `head`; `removeLast` always removes before `tail`.

---

## Key Design Decisions to Mention in Interview

1. **Why doubly linked list?** Need O(1) removal of an arbitrary node — requires a `prev` pointer. Singly linked list would require O(n) traversal to find the previous node.
2. **Why store key in Node?** At eviction time we call `map.remove(evictedNode.key)` — without the key in the node we cannot clean up the map in O(1).
3. **Why dummy head/tail?** Removes edge-case null checks for empty list, insert-at-front, and remove-from-back — every operation becomes uniform.
4. **Why not use Java's LinkedHashMap?** Acceptable in interview if you explain how it works internally (it is exactly a HashMap + doubly linked list with access-order mode). Prefer to implement from scratch to demonstrate understanding.
5. **Generic `<K, V>`** — not locked to `<Integer, Integer>`; any key/value type with correct `hashCode`/`equals`.
