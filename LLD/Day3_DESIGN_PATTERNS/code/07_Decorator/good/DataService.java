package decorator.good;

/**
 * Component interface — implemented by both the concrete service
 * and all decorators. This is what makes them interchangeable.
 *
 * The JDK's java.io package is built entirely on this pattern:
 *   InputStream in = new BufferedInputStream(new GZIPInputStream(new FileInputStream(...)));
 */
public interface DataService {

    String fetchUser(int userId);

    void saveUser(int userId, String data);
}
