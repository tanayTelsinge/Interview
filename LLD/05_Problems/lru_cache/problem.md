Design an LRU Cache

Q. What operations should it support?
- get(key): return value if key exists, else -1
- put(key, value): insert or update key-value pair

Q. What happens when capacity is exceeded?
- Evict the Least Recently Used (LRU) entry.

Q. What is the required time complexity?
- Both get and put must run in O(1).

Q. Should it be thread-safe?
- No (single-threaded for now).

Q. Is capacity fixed at construction?
- Yes.
