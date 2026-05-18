package code.OCP.good;

/**
 * GOOD — OCP applied.
 *
 * PaymentMethod interface = the stable abstraction.
 * Adding a new payment type = new class only. Zero existing code modified.
 */
public interface PaymentMethod {
    void process(double amount);
    double calculateFee(double amount);
    String getName();
}
