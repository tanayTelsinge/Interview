package observer.good;

/**
 * Observer interface — implemented by all subscribers.
 *
 * The Subject (publisher) only knows about this interface,
 * not about EmailObserver, SMSObserver, or PushObserver concretely.
 *
 * Interview tip: Some designs use a generic event type parameter here:
 *   Observer<T> { void update(T event); }
 * This avoids type-casting and is cleaner for multiple event types.
 */
public interface Observer {

    /**
     * Called by the Subject when its state changes.
     *
     * @param stockSymbol the stock that changed
     * @param newPrice    the updated price
     */
    void update(String stockSymbol, double newPrice);
}
