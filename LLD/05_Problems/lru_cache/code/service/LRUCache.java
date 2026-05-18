package Day4_problems.lru_cache.code.service;

import Day4_problems.lru_cache.code.domain.DoublyLinkedList;
import Day4_problems.lru_cache.code.domain.Node;

import java.util.HashMap;
import java.util.Map;

public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private final DoublyLinkedList<K, V> list;

    public LRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.list = new DoublyLinkedList<>();
    }

    // O(1) — return value if present, else null; marks key as most recently used
    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) return null;
        list.remove(node);
        list.addToFront(node);
        return node.value;
    }

    // O(1) — insert or update; evicts LRU entry when over capacity
    public void put(K key, V value) {
        Node<K, V> existing = map.get(key);
        if (existing != null) {
            existing.value = value;
            list.remove(existing);
            list.addToFront(existing);
            return;
        }

        if (list.getSize() == capacity) {
            Node<K, V> lru = list.removeLast();
            map.remove(lru.key); // key stored in node so we can clean the map in O(1)
        }

        Node<K, V> node = new Node<>(key, value);
        list.addToFront(node);
        map.put(key, node);
    }
}
