package Day4_problems.digital_wallet.code.strategies;

public class UPIStrategy implements PaymentStrategy {

    @Override
    public String getMethodName() {
        return "UPI";
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("Processing UPI payment of " + amount);
        return true;
    }
}
