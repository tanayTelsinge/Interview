package observer.good;

/**
 * Subject interface — implemented by all publishers.
 *
 * Separating the Subject interface allows a class to be observed
 * without inheriting from a base class (composition over inheritance).
 */
public interface Subject {

    void subscribe(Observer observer);

    void unsubscribe(Observer observer);

    void notifyObservers();
}
