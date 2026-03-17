package decorator;

import decorator.good.*;

/**
 * Demo: Decorator Pattern
 *
 * Shows how decorators compose at runtime.
 * The same DatabaseService gains logging, caching, or both
 * without any changes to DatabaseService.java.
 */
public class DecoratorDemo {

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("   DECORATOR PATTERN DEMO");
        System.out.println("========================================\n");

        DataService db = new DatabaseService();

        // --- Only logging ---
        System.out.println("--- Logging only ---");
        DataService logged = new LoggingDecorator(db);
        logged.fetchUser(1);
        System.out.println();

        // --- Only caching ---
        System.out.println("--- Caching only ---");
        DataService cached = new CachingDecorator(db);
        cached.fetchUser(2);
        cached.fetchUser(2);  // Second call is a cache hit
        System.out.println();

        // --- Logging + Caching (cache wraps DB, logging wraps cache) ---
        // Call order: LoggingDecorator → CachingDecorator → DatabaseService
        System.out.println("--- Logging + Caching stacked ---");
        DataService loggedAndCached = new LoggingDecorator(new CachingDecorator(db));
        loggedAndCached.fetchUser(3);         // Cache miss — hits DB
        System.out.println();
        loggedAndCached.fetchUser(3);         // Cache hit — no DB call
        System.out.println();

        // --- Save invalidates cache ---
        System.out.println("--- Save invalidates cache ---");
        loggedAndCached.saveUser(3, "updated data");
        System.out.println();
        loggedAndCached.fetchUser(3);         // Cache miss again after invalidation

        System.out.println("\n--- Interview takeaway ---");
        System.out.println("DatabaseService.java was NEVER modified.");
        System.out.println("2 features composed without 2^2=4 subclasses.");
        System.out.println("Same pattern as Java's BufferedInputStream(new GZIPInputStream(...))");
    }
}
