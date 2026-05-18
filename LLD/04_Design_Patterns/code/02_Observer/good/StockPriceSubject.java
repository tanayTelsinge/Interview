package observer.good;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Concrete Subject — tracks stock prices and notifies subscribers.
 *
 * Thread-safety note:
 *   Using CopyOnWriteArrayList instead of ArrayList for the observer list.
 *   This is a senior-level differentiator in interviews.
 *
 *   Why CopyOnWriteArrayList?
 *   - subscribe()/unsubscribe() can be called from any thread
 *   - notifyObservers() iterates the list while other threads might modify it
 *   - ArrayList would throw ConcurrentModificationException
 *   - CopyOnWriteArrayList: on write, copies the entire array (safe for iteration)
 *   - Trade-off: writes are O(n) — acceptable here because reads >> writes
 *     (price updates happen far more often than subscribe/unsubscribe)
 *
 * Memory leak note:
 *   If observers never call unsubscribe(), this list holds strong references,
 *   preventing garbage collection of subscriber objects.
 *   Fix options: use WeakReference, or require explicit lifecycle management.
 */
public class StockPriceSubject implements Subject {

    private final String stockSymbol;
    private double currentPrice;

    // IMPORTANT: CopyOnWriteArrayList for thread-safety (not ArrayList)
    private final List<Observer> observers = new CopyOnWriteArrayList<>();

    public StockPriceSubject(String stockSymbol, double initialPrice) {
        this.stockSymbol = stockSymbol;
        this.currentPrice = initialPrice;
    }

    @Override
    public void subscribe(Observer observer) {
        observers.add(observer);
        System.out.println("Observer subscribed to " + stockSymbol + ". Total subscribers: " + observers.size());
    }

    @Override
    public void unsubscribe(Observer observer) {
        observers.remove(observer);
        System.out.println("Observer unsubscribed from " + stockSymbol + ". Remaining: " + observers.size());
    }

    /**
     * Update the price and notify all observers.
     *
     * Exception isolation: one bad observer does NOT stop others from being notified.
     * Without the try-catch, if EmailObserver throws, SMSObserver never runs.
     */
    @Override
    public void notifyObservers() {
        System.out.println("\n[" + stockSymbol + "] Price changed to ₹" + currentPrice + " — notifying " + observers.size() + " observers");
        for (Observer observer : observers) {
            try {
                observer.update(stockSymbol, currentPrice);
            } catch (Exception e) {
                // IMPORTANT: isolate failures — don't let one bad observer break others
                System.err.println("Observer failed: " + observer.getClass().getSimpleName() + " — " + e.getMessage());
            }
        }
    }

    /**
     * Business method: update price and trigger notifications.
     */
    public void updatePrice(double newPrice) {
        if (newPrice != currentPrice) {
            this.currentPrice = newPrice;
            notifyObservers();
        }
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }
}
