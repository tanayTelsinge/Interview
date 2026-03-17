package decorator.good;

import java.util.HashMap;
import java.util.Map;

/**
 * Decorator: Caching.
 *
 * Wraps any DataService and adds an in-memory cache.
 * Can be stacked with LoggingDecorator in any order.
 *
 * Note: This is a simplified cache (no TTL, no eviction).
 * Production: use Redis or Caffeine instead. But the Decorator structure is the same.
 */
public class CachingDecorator implements DataService {

    private final DataService wrapped;
    private final Map<Integer, String> cache = new HashMap<>();

    public CachingDecorator(DataService wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String fetchUser(int userId) {
        if (cache.containsKey(userId)) {
            System.out.println("[CACHE] Hit for userId=" + userId);
            return cache.get(userId);
        }

        System.out.println("[CACHE] Miss for userId=" + userId + ". Fetching from source...");
        String result = wrapped.fetchUser(userId);
        cache.put(userId, result);
        return result;
    }

    @Override
    public void saveUser(int userId, String data) {
        wrapped.saveUser(userId, data);
        // Invalidate cache on write
        cache.remove(userId);
        System.out.println("[CACHE] Cache invalidated for userId=" + userId);
    }
}
