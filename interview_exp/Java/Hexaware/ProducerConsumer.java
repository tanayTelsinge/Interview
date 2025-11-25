package interview_exp.Java.Hexaware;

import java.util.LinkedList;
import java.util.Queue;

//Q. Implement Producer Consumer Pattern

public class ProducerConsumer {

    public static final Queue<String> q = new LinkedList<>();
    public static final int CAPACITY = 5;

    public synchronized void producer(String message) {
        try {
            if (q.size() == CAPACITY) {
                wait();
            } else {
                System.out.println("Producer added message : " + message);
                q.add(message);
            }
            notifyAll();
        } catch (InterruptedException e) {
            System.err.println("exception in producer");
        }
    }

    public synchronized void consumer() {
        try {
            if (q.isEmpty()) {
                wait();
            } else {
                String message = q.poll();
                System.out.println("Consumer consumed message : " + message);
            }
            notifyAll();
        } catch (InterruptedException e) {
            System.err.println("exception in producer");
        }
    }

    public static void main(String[] args) {
        ProducerConsumer pc = new ProducerConsumer();

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
