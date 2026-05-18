import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * VIRTUAL THREADS (Java 21+)
 * - Lightweight threads managed by JVM, not OS
 * - Unmount from carrier thread when blocking (I/O), remount when ready
 * - Best for: I/O-bound tasks (DB, HTTP). NOT for CPU-bound work.
 */
public class VirtualThreads {

    // 1. Creating Virtual Threads
    static void demo1_create() throws InterruptedException {
        // Platform thread (old way)
        Thread platform = new Thread(() -> System.out.println("Platform: " + Thread.currentThread()));
        platform.start();
        platform.join();

        // Virtual thread
        Thread vt = Thread.ofVirtual().name("my-vt").start(() ->
            System.out.println("Virtual: " + Thread.currentThread() + " | isVirtual=" + Thread.currentThread().isVirtual())
        );
        vt.join();
    }

    // 2. ExecutorService (preferred for bulk tasks)
    static void demo2_executor() throws Exception {
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new java.util.ArrayList<Future<String>>();

            for (int i = 0; i < 5; i++) {
                int id = i;
                futures.add(ex.submit(() -> {
                    Thread.sleep(100); // simulate I/O
                    return "Task " + id + " on " + Thread.currentThread().getName();
                }));
            }

            for (var f : futures) System.out.println(f.get());
        }
    }

    // 3. Pinning — virtual thread gets stuck to carrier inside synchronized blocks
    //    Fix: use ReentrantLock instead of synchronized
    static final ReentrantLock lock = new ReentrantLock();

    static void demo3_pinning() throws InterruptedException {
        // BAD: synchronized pins the thread (can't unmount during sleep)
        Thread bad = Thread.ofVirtual().start(() -> {
            synchronized (VirtualThreads.class) {
                try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });

        // GOOD: ReentrantLock allows unmounting while waiting
        Thread good = Thread.ofVirtual().start(() -> {
            lock.lock();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });

        bad.join();
        good.join();
        System.out.println("Prefer ReentrantLock over synchronized with virtual threads.");
    }

    // 4. Structured Concurrency — child threads don't outlive parent
    static void demo4_structured() throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var user  = scope.fork(() -> { Thread.sleep(50); return "User#1"; });
            var order = scope.fork(() -> { Thread.sleep(80); return "Order#1"; });

            scope.join();
            scope.throwIfFailed();

            System.out.println(user.get() + " | " + order.get());
        }
    }

    // 5. Scoped Values — immutable, auto-inherited by child threads, auto-cleaned
    //    Better than ThreadLocal for virtual threads (no leaks, faster lookup)
    static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    static void demo5_scopedValues() throws Exception {
        ScopedValue.where(REQUEST_ID, "req-123").run(() -> {
            System.out.println("RequestId = " + REQUEST_ID.get());
            processRequest();
        });
        System.out.println("Bound outside scope? " + REQUEST_ID.isBound()); // false
    }

    static void processRequest() {
        System.out.println("[processRequest] " + REQUEST_ID.get());
    }

    public static void main(String[] args) throws Exception {
        demo1_create();
        demo2_executor();
        demo3_pinning();
        demo4_structured();
        demo5_scopedValues();
    }
}
