package decorator.bad;

/**
 * VIOLATION: Subclass explosion anti-pattern.
 *
 * If you need logging, caching, or both on top of a service,
 * you'd need separate subclasses for every combination.
 *
 * 2 features (logging, caching) → 4 subclasses:
 *   - DatabaseService
 *   - LoggingDatabaseService
 *   - CachingDatabaseService
 *   - LoggingAndCachingDatabaseService
 *
 * 3 features → 8 subclasses. N features → 2^N subclasses. This doesn't scale.
 *
 * Also: inheritance is compile-time. Can't choose at runtime whether to add
 * logging or not. Decorator solves both problems.
 */

// BAD: Hard-coded subclass
class DatabaseService_Bad {
    public String fetchUser(int userId) {
        System.out.println("  [DB] Fetching user " + userId + " from database...");
        return "User{id=" + userId + ", name='Rahul'}";
    }
}

// BAD: Had to open DatabaseService_Bad to create this... and it hardcodes logging
class LoggingDatabaseService_Bad extends DatabaseService_Bad {
    @Override
    public String fetchUser(int userId) {
        System.out.println("[LOG] fetchUser called with userId=" + userId);
        String result = super.fetchUser(userId);
        System.out.println("[LOG] fetchUser returned: " + result);
        return result;
    }
}

// BAD: Another subclass just for caching
class CachingDatabaseService_Bad extends DatabaseService_Bad {
    private java.util.Map<Integer, String> cache = new java.util.HashMap<>();

    @Override
    public String fetchUser(int userId) {
        if (cache.containsKey(userId)) {
            System.out.println("[CACHE] Hit for userId=" + userId);
            return cache.get(userId);
        }
        String result = super.fetchUser(userId);
        cache.put(userId, result);
        return result;
    }
}

// BAD: 4th class needed for both logging AND caching
class LoggingAndCachingDatabaseService_Bad extends DatabaseService_Bad {
    // Duplication of both logging and caching logic here... a mess.
}
