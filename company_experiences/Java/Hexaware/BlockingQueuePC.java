package interview_exp.Java.Hexaware;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueuePC {
    
    private static final int CAPACITY = 5;
    private final BlockingQueue<String> q = new ArrayBlockingQueue<>(CAPACITY);

    public void producer(String message) {
        try {
            q.put(message);
        } catch (InterruptedException e) {
        }
        System.out.println("Producer added message : " + message);
    }

    public void consumer() {
        try {
            String message = q.take();
            System.out.println("Consumer consumed message : " + message);
        } catch (InterruptedException e) {
        }
    }


    
    public static void main(String[] args) {
        BlockingQueuePC pc = new BlockingQueuePC();

        Thread t1 = new Thread(() -> {
            int i = 1;
            while (true) {
                pc.producer("msg-" + i);
                i++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        });
        Thread t2 = new Thread(() -> {
            while (true) {
                pc.consumer();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        });

        t1.start();
        t2.start();
    }

}
