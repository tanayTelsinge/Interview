package singleton.good;

/**
 * GOOD Singleton #2: Bill Pugh / Initialization-on-demand Holder idiom.
 *
 * This is the CLEANEST Java Singleton implementation.
 * No synchronized block, no volatile, yet fully thread-safe.
 *
 * How it works:
 *   - The JVM loads classes lazily — Holder is NOT loaded when SingletonBillPugh loads.
 *   - Holder is loaded only when getInstance() is first called.
 *   - The JVM guarantees that class loading (static field initialization) is thread-safe.
 *   - So INSTANCE is created exactly once, without any synchronization overhead.
 *
 * Use this in interviews if asked for the "best" Singleton implementation.
 * Follow up: "Why is this better than DCL?" → No volatile, simpler, JVM guarantees safety.
 *
 * Limitation: doesn't handle serialization (use Enum for that).
 */
public class SingletonBillPugh {

    private SingletonBillPugh() {
        System.out.println("SingletonBillPugh initialized (should print only once)");
    }

    // Inner class is NOT loaded until getInstance() is called (lazy)
    // Static field initialization by JVM is guaranteed thread-safe
    private static class Holder {
        private static final SingletonBillPugh INSTANCE = new SingletonBillPugh();
    }

    public static SingletonBillPugh getInstance() {
        return Holder.INSTANCE;  // Triggers Holder class loading — thread-safe by JVM spec
    }

    public void doWork(String task) {
        System.out.println("SingletonBillPugh doing: " + task);
    }
}
