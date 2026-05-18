package observer.bad;

/**
 * VIOLATION: Observer pattern NOT applied.
 *
 * Problems:
 * 1. StockPriceNotifier_Bad directly calls EmailService and SMSService.
 *    Adding a new notification channel (e.g., Push) requires modifying this class.
 * 2. OCP violation — not open for extension.
 * 3. SRP violation — price tracking AND notification delivery in one class.
 * 4. Not thread-safe — direct method calls with no concurrency handling.
 * 5. If EmailService throws, SMSService is never called (no isolation).
 */
public class StockPriceNotifier_Bad {

    private String stockSymbol;
    private double price;

    // BAD: Hard-coded dependencies on concrete notification classes
    private EmailService emailService = new EmailService();
    private SMSService smsService = new SMSService();
    // Adding PushService requires editing this class and adding a new field

    public void updatePrice(double newPrice) {
        this.price = newPrice;
        System.out.println(stockSymbol + " price updated to: " + newPrice);

        // BAD: Every new notification method requires adding a line here
        emailService.sendPriceAlert(stockSymbol, newPrice);
        smsService.sendPriceAlert(stockSymbol, newPrice);
        // pushService.sendPriceAlert(...)  // have to add this + import + field above
    }
}

// Helper classes simulating concrete services
class EmailService {
    public void sendPriceAlert(String symbol, double price) {
        System.out.println("[Email] " + symbol + " is now ₹" + price);
    }
}

class SMSService {
    public void sendPriceAlert(String symbol, double price) {
        System.out.println("[SMS] " + symbol + " is now ₹" + price);
    }
}
