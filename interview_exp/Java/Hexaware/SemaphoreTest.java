package interview_exp.Java.Hexaware;
 import java.util.concurrent.Semaphore;

 //Semaphore is used to limit access of resources (threads) to specific code block.
 // it uses permits - eg. 2 only 2 threads can access at a time.
 // Mainly used to limit access to slow processes like DB calls, API, external calls.
 //acquire() to acquire access, acquireUnInterruptily() to avoid exception, release() to release thread.

public class SemaphoreTest {


    // only allow 2 slow calls at once
    private static final Semaphore apiLimit = new Semaphore(2);

    public static void main(String[] args) {
        for (int i = 1; i <= 10; i++) {
            int id = i;
            new Thread(() -> callSlowApi(id)).start();
        }
    }

    private static void callSlowApi(int id) {
        try {
            apiLimit.acquire(); // wait if too many calls
            System.out.println("Thread " + id + " calling API...");
            
            // simulate slow external API (DB call/HTTP/etc.)
            Thread.sleep(2000);

            System.out.println("Thread " + id + " done.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            apiLimit.release();
        }
    }
}
