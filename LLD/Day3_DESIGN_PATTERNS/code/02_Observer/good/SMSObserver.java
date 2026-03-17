package observer.good;

/**
 * Concrete Observer: SMS notification.
 *
 * Different behavior from EmailObserver — sends SMS regardless of threshold.
 * Both implement the same Observer interface — polymorphism at work.
 */
public class SMSObserver implements Observer {

    private final String phoneNumber;

    public SMSObserver(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void update(String stockSymbol, double newPrice) {
        System.out.println("[SMS -> " + phoneNumber + "] " +
            stockSymbol + " price update: ₹" + newPrice);
    }
}
