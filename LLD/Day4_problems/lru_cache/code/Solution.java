package Day4_problems.lru_cache.code;

import Day4_problems.lru_cache.code.service.LRUCache;

public class Solution {
    public static void main(String[] args) {
        LRUCache<Integer, Integer> cache = new LRUCache<>(3);

        cache.put(1, 10);
        cache.put(2, 20);
        cache.put(3, 30);

        System.out.println(cache.get(1));  // 10  — 1 becomes MRU; order: 1,3,2
        cache.put(4, 40);                  // evicts 2 (LRU); order: 4,1,3

        System.out.println(cache.get(2));  // null — evicted
        System.out.println(cache.get(3));  // 30
        System.out.println(cache.get(4));  // 40

        cache.put(1, 100);                 // update existing key
        System.out.println(cache.get(1));  // 100
    }
}
