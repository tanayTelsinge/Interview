package singleton.good;

/**
 * GOOD Singleton #3: Enum Singleton (Josh Bloch's recommendation from Effective Java).
 *
 * Handles ALL edge cases automatically:
 *   - Thread-safety: JVM guarantees enum instances are created once
 *   - Serialization: enums are serialization-safe by default
 *   - Reflection: can't break enum singleton via reflection (unlike class-based)
 *   - Clone: enums can't be cloned
 *
 * Limitation:
 *   - Can't lazily initialize (created when class loads)
 *   - Can't extend another class
 *   - Less flexible than class-based Singleton
 *
 * Best for: configuration holders, registry objects, simple singletons
 *           where you don't need to extend a class.
 *
 * Usage: SingletonEnum.INSTANCE.processRequest("data")
 */
public enum SingletonEnum {

    INSTANCE;

    // State is fine — only one instance exists
    private int requestCount = 0;

    public void processRequest(String data) {
        requestCount++;
        System.out.println("SingletonEnum processing request #" + requestCount + ": " + data);
    }

    public int getRequestCount() {
        return requestCount;
    }
}
