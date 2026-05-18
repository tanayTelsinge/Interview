package singleton;

import singleton.good.*;

/**
 * Demo: Singleton Pattern — all three good variants.
 */
public class SingletonDemo {

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("   SINGLETON PATTERN DEMO");
        System.out.println("========================================\n");

        // --- Double-Checked Locking ---
        System.out.println("--- Double-Checked Locking (with volatile) ---");
        SingletonDoubleChecked s1 = SingletonDoubleChecked.getInstance();
        SingletonDoubleChecked s2 = SingletonDoubleChecked.getInstance();
        System.out.println("Same instance? " + (s1 == s2));  // true
        System.out.println("Config: " + s1.getConfigValue());

        // --- Bill Pugh ---
        System.out.println("\n--- Bill Pugh (Initialization-on-demand) ---");
        SingletonBillPugh bp1 = SingletonBillPugh.getInstance();
        SingletonBillPugh bp2 = SingletonBillPugh.getInstance();
        System.out.println("Same instance? " + (bp1 == bp2));  // true
        bp1.doWork("task-A");
        bp2.doWork("task-B");  // same object

        // --- Enum Singleton ---
        System.out.println("\n--- Enum Singleton (most robust) ---");
        SingletonEnum.INSTANCE.processRequest("request-1");
        SingletonEnum.INSTANCE.processRequest("request-2");
        System.out.println("Total requests: " + SingletonEnum.INSTANCE.getRequestCount());

        System.out.println("\n--- Interview comparison ---");
        System.out.println("DCL + volatile     : explicit, production-common, needs volatile");
        System.out.println("Bill Pugh          : cleanest class-based, JVM-safe, no lock needed");
        System.out.println("Enum               : most robust, handles serialization/reflection");
        System.out.println("When to avoid      : use Spring @Bean(singleton) or DI instead");
    }
}
