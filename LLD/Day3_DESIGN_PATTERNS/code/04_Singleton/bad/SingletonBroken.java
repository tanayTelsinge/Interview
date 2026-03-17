package singleton.bad;

/**
 * BROKEN Singleton implementations — know these to demonstrate what NOT to do.
 *
 * Interviewers ask about Singleton primarily to test thread-safety knowledge.
 * Showing awareness of these broken variants is a senior signal.
 */

// BROKEN #1: Not thread-safe at all
class SingletonNotThreadSafe {
    private static SingletonNotThreadSafe instance;
    private int connectionCount = 0;

    private SingletonNotThreadSafe() {}

    // BAD: Race condition — two threads can both see instance == null
    // and both call new SingletonNotThreadSafe() — two instances created
    public static SingletonNotThreadSafe getInstance() {
        if (instance == null) {
            // Thread A reaches here — instance is null, proceeds
            // Thread B reaches here simultaneously — instance is still null!
            instance = new SingletonNotThreadSafe();  // Both threads execute this
        }
        return instance;
    }
}

// BROKEN #2: Synchronized but slow
class SingletonSynchronizedSlow {
    private static SingletonSynchronizedSlow instance;

    private SingletonSynchronizedSlow() {}

    // WORKS but BAD for performance: every call acquires the lock,
    // even after the instance is already created.
    // In a high-throughput app (e.g., database connection pool),
    // this becomes a bottleneck.
    public static synchronized SingletonSynchronizedSlow getInstance() {
        if (instance == null) {
            instance = new SingletonSynchronizedSlow();
        }
        return instance;
    }
}

// BROKEN #3: Double-checked locking WITHOUT volatile
class SingletonDCLWithoutVolatile {
    private static SingletonDCLWithoutVolatile instance; // MISSING volatile!

    private SingletonDCLWithoutVolatile() {}

    // LOOKS right but is subtly broken due to CPU instruction reordering.
    //
    // Object creation (new SingletonDCLWithoutVolatile()) compiles to 3 steps:
    //   1. Allocate memory
    //   2. Initialize fields (run constructor)
    //   3. Assign reference to instance variable
    //
    // The JVM/CPU can reorder steps 2 and 3:
    //   1. Allocate memory
    //   3. Assign reference (instance is now non-null, but object not initialized!)
    //   2. Initialize fields
    //
    // Without volatile, Thread B can see instance != null (step 3 done),
    // but get a partially constructed object (step 2 not done yet).
    // volatile prevents this reordering.
    public static SingletonDCLWithoutVolatile getInstance() {
        if (instance == null) {
            synchronized (SingletonDCLWithoutVolatile.class) {
                if (instance == null) {
                    instance = new SingletonDCLWithoutVolatile();  // UNSAFE without volatile
                }
            }
        }
        return instance;
    }
}
