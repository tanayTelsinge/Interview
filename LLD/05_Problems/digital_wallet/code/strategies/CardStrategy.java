package Day4_problems.digital_wallet.code.strategies;

public class CardStrategy implements PaymentStrategy {

    @Override
    public String getMethodName() {
        return "CARD";
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("Processing Card payment of " + amount);
        return true;
    }
}
