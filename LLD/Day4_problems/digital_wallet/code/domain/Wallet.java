package Day4_problems.digital_wallet.code.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Day4_problems.digital_wallet.code.enums.TransactionStatus;
import Day4_problems.digital_wallet.code.enums.TransactionType;
import Day4_problems.digital_wallet.code.enums.WalletStatus;
import Day4_problems.digital_wallet.code.exception.InsufficientFundsException;

public class Wallet {

    private String walletId;
    private String userId;
    private double balance;
    private WalletStatus status;
    private List<Transaction> transactions;

    public Wallet(String walletId, String userId) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = 0.0;
        this.status = WalletStatus.ACTIVE;
        this.transactions = new ArrayList<>();
    }

    public synchronized void credit(double amount, String description) {
        balance += amount;
        transactions.add(new Transaction(walletId, amount, TransactionType.CREDIT, TransactionStatus.SUCCESS, description));
    }

    public synchronized void debit(double amount, String description) {
        if (balance < amount) {
            throw new InsufficientFundsException(
                "Insufficient funds in wallet " + walletId + ". Balance: " + balance + ", Required: " + amount
            );
        }
        balance -= amount;
        transactions.add(new Transaction(walletId, amount, TransactionType.DEBIT, TransactionStatus.SUCCESS, description));
    }

    public double getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public String getWalletId() {
        return walletId;
    }

    public String getUserId() {
        return userId;
    }

    public WalletStatus getStatus() {
        return status;
    }
}
