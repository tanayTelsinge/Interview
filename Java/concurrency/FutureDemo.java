import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FutureDemo {


  public static void main(String[] args) throws ExecutionException, InterruptedException {

    ExecutorService executor = Executors.newFixedThreadPool(3);

    long start = System.currentTimeMillis();
    System.out.println("Placing orders...");
    // 1. Normal way - sequentially
   // makePizza();
   // makePasta();
   // makeJuice();
    // Total time = 3s + 2s + 1s = 6s

    // 2. Using Future - concurrently
    Callable<String> pizzaTask = () -> makePizza();
    Callable<String> pastaTask = () -> makePasta();
    Callable<String> juiceTask = () -> makeJuice();
    Future<String> pizza = executor.submit(pizzaTask);
    Future<String> pasta = executor.submit(pastaTask);
    Future<String> juice = executor.submit(juiceTask);


    // ⚠️ BLOCKING
    String pizzaResult = pizza.get();   // blocks 3s
    String pastaResult = pasta.get();   // blocks more
    String juiceResult = juice.get();   // blocks more
    System.out.println("💬 Customer is chatting, not waiting...");
    // Total time = max(3s, 2s, 1s) = 3s

    // 3. Using CompletableFuture - concurrently
    //Total Time = 3s (when the longest task is done) Same as Future but non-blocking
  /*  CompletableFuture<String> pizza = CompletableFuture.supplyAsync(() -> makePizza(), executor);
    CompletableFuture<String> pasta = CompletableFuture.supplyAsync(() -> makePasta(), executor);
    CompletableFuture<String> juice = CompletableFuture.supplyAsync(() -> makeJuice(), executor);
    // Non-blocking
    CompletableFuture<Void> allOf = CompletableFuture.allOf(pizza, pasta, juice);

    System.out.println("💬 Customer is chatting, not waiting...");
    allOf.join();*/
    long end = System.currentTimeMillis();
    System.out.println(end - start);
  }


  static String makePizza() {
    sleep(3000); // 3s
    System.out.println("🍕 Pizza ready");
    return "Pizza";
  }

  static String makePasta() {
    sleep(2000); // 2s
    System.out.println("🍝 Pasta ready");
    return "Pasta";
  }

  static String makeJuice() {
    sleep(1000); // 1s
    System.out.println("🥤 Juice ready");
    return "Juice";
  }

  static void sleep(int ms) {
    try { Thread.sleep(ms); } catch (InterruptedException e) { }
  }

}