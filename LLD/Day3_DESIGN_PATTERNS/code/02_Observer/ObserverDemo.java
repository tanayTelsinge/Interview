package observer;

import observer.good.*;

/**
 * Demo: Observer Pattern
 *
 * Shows subscribe, unsubscribe, and notification isolation.
 */
public class ObserverDemo {

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("   OBSERVER PATTERN DEMO");
        System.out.println("========================================\n");

        // Subject (publisher)
        StockPriceSubject infosys = new StockPriceSubject("INFY", 1500.0);

        // Observers (subscribers)
        Observer emailObserver = new EmailObserver("investor@gmail.com", 1400.0);
        Observer smsObserver   = new SMSObserver("+91-9999999999");

        infosys.subscribe(emailObserver);
        infosys.subscribe(smsObserver);

        // Trigger notifications
        infosys.updatePrice(1450.0);  // Both notified
        infosys.updatePrice(1380.0);  // Email shows ALERT (below 1400 threshold)

        // Unsubscribe SMS observer
        System.out.println("\n--- Unsubscribing SMS observer ---");
        infosys.unsubscribe(smsObserver);
        infosys.updatePrice(1350.0);  // Only email notified now

        // Demonstrate observer isolation (bad observer doesn't break others)
        System.out.println("\n--- Testing observer isolation ---");
        Observer faultyObserver = (symbol, price) -> {
            throw new RuntimeException("This observer is broken!");
        };
        infosys.subscribe(faultyObserver);
        infosys.subscribe(new SMSObserver("+91-8888888888"));
        infosys.updatePrice(1300.0);  // faulty observer fails, SMS still notified

        System.out.println("\n--- Interview takeaways ---");
        System.out.println("1. CopyOnWriteArrayList prevents ConcurrentModificationException");
        System.out.println("2. try-catch in notify loop isolates bad observers");
        System.out.println("3. Adding Push/Slack observers requires ZERO changes to StockPriceSubject");
    }
}
