package Day4_problems.digital_wallet.code.strategies;

public interface PaymentStrategy {
    String getMethodName();
    boolean pay(double amount);
}
