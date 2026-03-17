package observer.good;

/**
 * Concrete Observer: Email notification.
 *
 * Adding this class required ZERO changes to StockPriceSubject — OCP.
 * This class is independently testable (pass it mock data, no Subject needed).
 */
public class EmailObserver implements Observer {

    private final String emailAddress;
    private final double alertThreshold;  // only alert if price drops below this

    public EmailObserver(String emailAddress, double alertThreshold) {
        this.emailAddress = emailAddress;
        this.alertThreshold = alertThreshold;
    }

    @Override
    public void update(String stockSymbol, double newPrice) {
        if (newPrice < alertThreshold) {
            System.out.println("[EMAIL -> " + emailAddress + "] ALERT: " +
                stockSymbol + " dropped to ₹" + newPrice +
                " (threshold: ₹" + alertThreshold + ")");
        } else {
            System.out.println("[EMAIL -> " + emailAddress + "] FYI: " +
                stockSymbol + " is at ₹" + newPrice);
        }
    }
}
