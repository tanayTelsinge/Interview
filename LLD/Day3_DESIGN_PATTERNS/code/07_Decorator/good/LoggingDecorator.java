package decorator.good;

/**
 * Decorator: Logging.
 *
 * Wraps any DataService and adds logging around every call.
 * The wrapped service could be DatabaseService, CachingDecorator, or another decorator.
 *
 * Key: LoggingDecorator implements DataService AND holds a DataService.
 * This "implements + wraps" is the structural signature of the Decorator pattern.
 */
public class LoggingDecorator implements DataService {

    private final DataService wrapped;   // The thing being decorated

    public LoggingDecorator(DataService wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String fetchUser(int userId) {
        System.out.println("[LOG] fetchUser(userId=" + userId + ") called");
        long start = System.currentTimeMillis();

        String result = wrapped.fetchUser(userId);  // Delegate to wrapped service

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[LOG] fetchUser returned in " + elapsed + "ms: " + result);
        return result;
    }

    @Override
    public void saveUser(int userId, String data) {
        System.out.println("[LOG] saveUser(userId=" + userId + ") called");
        wrapped.saveUser(userId, data);
        System.out.println("[LOG] saveUser complete");
    }
}
