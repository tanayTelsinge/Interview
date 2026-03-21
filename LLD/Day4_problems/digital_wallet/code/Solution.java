package Day4_problems.digital_wallet.code;

import Day4_problems.digital_wallet.code.domain.Transaction;
import Day4_problems.digital_wallet.code.domain.User;
import Day4_problems.digital_wallet.code.domain.Wallet;
import Day4_problems.digital_wallet.code.exception.InsufficientFundsException;
import Day4_problems.digital_wallet.code.service.WalletService;
import Day4_problems.digital_wallet.code.strategies.CardStrategy;
import Day4_problems.digital_wallet.code.strategies.UPIStrategy;

import java.util.List;

public class Solution {

    public static void main(String[] args) {
        // 1. Create 2 users with wallets
        Wallet wallet1 = new Wallet("W001", "U001");
        Wallet wallet2 = new Wallet("W002", "U002");

        User user1 = new User("U001", "Alice", "alice@example.com", wallet1);
        User user2 = new User("U002", "Bob", "bob@example.com", wallet2);

        WalletService walletService = new WalletService();

        System.out.println("=== Add Money ===");

        // 2. User1 adds money via UPI
        walletService.addMoney(user1.getWallet(), 1000.0, new UPIStrategy());

        // 3. User2 adds money via Card
        walletService.addMoney(user2.getWallet(), 500.0, new CardStrategy());

        System.out.println("\n=== Transfer ===");

        // 4. Transfer from User1 to User2
        walletService.transfer(user1.getWallet(), user2.getWallet(), 300.0);

        System.out.println("\n=== Insufficient Funds Transfer ===");

        // 5. Try transfer with insufficient funds
        try {
            walletService.transfer(user2.getWallet(), user1.getWallet(), 10000.0);
        } catch (InsufficientFundsException e) {
            System.out.println("Transfer failed: " + e.getMessage());
        }

        System.out.println("\n=== Balances ===");

        // 6. Print balances
        System.out.println("Alice's balance: " + walletService.getBalance(user1.getWallet()));
        System.out.println("Bob's balance:   " + walletService.getBalance(user2.getWallet()));

        System.out.println("\n=== Transaction History: Alice ===");
        List<Transaction> aliceTxns = walletService.getTransactionHistory(user1.getWallet());
        for (Transaction t : aliceTxns) {
            System.out.println("[" + t.getType() + "] " + t.getAmount() + " — " + t.getDescription()
                    + " | Status: " + t.getStatus() + " | " + t.getTimestamp());
        }

        System.out.println("\n=== Transaction History: Bob ===");
        List<Transaction> bobTxns = walletService.getTransactionHistory(user2.getWallet());
        for (Transaction t : bobTxns) {
            System.out.println("[" + t.getType() + "] " + t.getAmount() + " — " + t.getDescription()
                    + " | Status: " + t.getStatus() + " | " + t.getTimestamp());
        }
    }
}
