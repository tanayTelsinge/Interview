package decorator.good;

/**
 * Concrete Component — the real implementation.
 * No logging, no caching — single responsibility: talk to the database.
 */
public class DatabaseService implements DataService {

    @Override
    public String fetchUser(int userId) {
        System.out.println("  [DB] Querying database for userId=" + userId + "...");
        // Simulate DB call
        return "User{id=" + userId + ", name='Rahul', email='rahul@example.com'}";
    }

    @Override
    public void saveUser(int userId, String data) {
        System.out.println("  [DB] Persisting user " + userId + " to database: " + data);
    }
}
