package Day4_problems.digital_wallet.code.strategies;

public class NetBankingStrategy implements PaymentStrategy {

    @Override
    public String getMethodName() {
        return "NET_BANKING";
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("Processing NetBanking payment of " + amount);
        return true;
    }
}
