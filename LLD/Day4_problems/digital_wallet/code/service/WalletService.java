package Day4_problems.digital_wallet.code.service;

import java.util.List;

import Day4_problems.digital_wallet.code.domain.Transaction;
import Day4_problems.digital_wallet.code.domain.Wallet;
import Day4_problems.digital_wallet.code.strategies.PaymentStrategy;

public class WalletService {

    public void addMoney(Wallet wallet, double amount, PaymentStrategy paymentStrategy) {
        String method = paymentStrategy.getMethodName();
        boolean success = paymentStrategy.pay(amount);
        if (success) {
            wallet.credit(amount, "Add money via " + method);
            System.out.println("Successfully added " + amount + " to wallet " + wallet.getWalletId() + " via " + method);
        } else {
            System.out.println("Payment failed via " + method + " for amount " + amount);
        }
    }

    
    /**
     * Transfers {@code amount} from one wallet to another in a deadlock-safe manner.
     * Locks are always acquired in ascending walletId order, ensuring that two concurrent
     * transfers between the same pair of wallets never deadlock each other.
     */
    public void transfer(Wallet from, Wallet to, double amount) {
        // Deadlock-safe: always acquire locks in walletId order (lower ID first)
        boolean fromFirst = from.getWalletId().compareTo(to.getWalletId()) < 0;
        Wallet first  = fromFirst ? from : to;
        Wallet second = fromFirst ? to   : from;

        synchronized (first) {
            synchronized (second) {
                from.debit(amount, "Transfer to wallet " + to.getWalletId());
                to.credit(amount, "Transfer from wallet " + from.getWalletId());
                System.out.println("Transferred " + amount + " from wallet " + from.getWalletId()
                        + " to wallet " + to.getWalletId());
            }
        }
    }

    public double getBalance(Wallet wallet) {
        return wallet.getBalance();
    }

    public List<Transaction> getTransactionHistory(Wallet wallet) {
        return wallet.getTransactions();
    }
}
