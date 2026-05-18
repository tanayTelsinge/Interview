package singleton.good;

/**
 * GOOD Singleton #1: Double-Checked Locking with volatile.
 *
 * Use when: lazy initialization is required and you want explicit control.
 * Commonly asked in interviews — show you know WHY volatile is needed.
 *
 * volatile guarantees:
 * 1. Visibility: changes made by one thread are immediately visible to others
 * 2. Ordering: prevents instruction reordering during object construction
 *
 * This is the pattern used in many production codebases.
 */
public class SingletonDoubleChecked {

    // volatile is NON-NEGOTIABLE — without it, DCL is broken (see SingletonBroken.java)
    private static volatile SingletonDoubleChecked instance;

    private final String configValue;

    private SingletonDoubleChecked() {
        // Expensive initialization (e.g., read config file, open DB connection)
        this.configValue = "loaded-from-config";
        System.out.println("SingletonDoubleChecked initialized (should print only once)");
    }

    public static SingletonDoubleChecked getInstance() {
        if (instance == null) {                             // First check — no lock (fast path)
            synchronized (SingletonDoubleChecked.class) {  // Lock only for creation
                if (instance == null) {                     // Second check — inside lock
                    instance = new SingletonDoubleChecked();
                }
            }
        }
        return instance;  // After first creation, no lock acquired ever again
    }

    public String getConfigValue() {
        return configValue;
    }

    // Prevent cloning from breaking singleton
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Singleton cannot be cloned");
    }
}
