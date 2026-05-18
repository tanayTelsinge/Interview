package Day4_problems.lru_cache.code.domain;

public class DoublyLinkedList<K, V> {
    private final Node<K, V> head; // dummy MRU sentinel
    private final Node<K, V> tail; // dummy LRU sentinel
    private int size;

    public DoublyLinkedList() {
        head = new Node<>(null, null);
        tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

    // Insert node right after head (MRU position)
    public void addToFront(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
        size++;
    }

    // Detach an arbitrary node from the list
    public void remove(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
        size--;
    }

    // Remove and return the node just before tail (LRU position)
    public Node<K, V> removeLast() {
        if (size == 0) return null;
        Node<K, V> lru = tail.prev;
        remove(lru);
        return lru;
    }

    public int getSize() {
        return size;
    }
}
