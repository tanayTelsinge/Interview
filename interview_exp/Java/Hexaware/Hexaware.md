- Implement Producer Consumer problem.

![alt text](image.png)
- 3 types by which we can implement Producer Consumer problem
  1. Using wait and notify
  2. Using Blocking Queue
  3. Using Semaphores

- Below is implementation using wait and notify

```java
import java.util.LinkedList;
import java.util.Queue;
public class ProducerConsumer {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int CAPACITY = 5;

    public void produce(int value) throws InterruptedException {
        synchronized (this) {
            while (queue.size() == CAPACITY) {
                wait();
            }
            queue.add(value);
            System.out.println("Produced: " + value);
            notifyAll();
        }
    }

    public int consume() throws InterruptedException {
        synchronized (this) {
            while (queue.isEmpty()) {
                wait();
            }
            int value = queue.poll();
            System.out.println("Consumed: " + value);
            notifyAll();
            return value;
        }
    }
}

``` 

- Below is implementation using Blocking Queue

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProducerConsumer {
    private static final int CAPACITY = 5;
    private static final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(CAPACITY);

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable producer = () -> {
            try {
                for (int i = 0; i < 10; i++) {
                    System.out.println("Producing: " + i);
                    queue.put(i);
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Runnable consumer = () -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Integer value = queue.take();
                    System.out.println("Consuming: " + value);
                    TimeUnit.MILLISECONDS.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        executor.submit(producer);
        executor.submit(consumer);

        executor.shutdown();
    }
}
```


  - This type of problems are Concurrency coding problems. (Defog Tech youtube videos covered it https://www.youtube.com/watch?v=UOr9kMCCa5g)

  