

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demonstrates ENCAPSULATION.
 *
 * Key rules shown here:
 *  1. Private fields — internal state hidden
 *  2. Controlled mutation — no direct field access, only via methods that enforce invariants
 *  3. Defensive copy on getters — caller can't mutate internal list
 *  4. Immutable value returned where possible
 */
public class BankAccount {

    private final String accountId;
    private final String owner;
    private double balance;
    private final List<String> transactionHistory;

    public BankAccount(String accountId, String owner, double initialBalance) {
        if (initialBalance < 0) throw new IllegalArgumentException("Initial balance cannot be negative");
        this.accountId = accountId;
        this.owner = owner;
        this.balance = initialBalance;
        this.transactionHistory = new ArrayList<>();
        transactionHistory.add("Account opened with balance: " + initialBalance);
    }

    // Controlled mutation — enforces business rule (can't withdraw more than balance)
    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
        balance += amount;
        transactionHistory.add("Deposited: " + amount);
    }

    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive");
        if (amount > balance) throw new IllegalStateException("Insufficient funds");
        balance -= amount;
        transactionHistory.add("Withdrew: " + amount);
    }

    // Read-only access — balance is derived, not directly settable
    public double getBalance() {
        return balance;
    }

    // DEFENSIVE COPY — caller gets a snapshot, cannot modify internal list
    public List<String> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    public String getAccountId() { return accountId; }
    public String getOwner() { return owner; }

    // -------------------------------------------------------------------------
    // LAW OF DEMETER DEMO
    // -------------------------------------------------------------------------

    // BAD PATTERN (don't do this in your design):
    // caller does: account.getAddress().getCity().getZipCode()
    // This chains through multiple objects — caller knows too much about internal structure

    // GOOD PATTERN — expose what callers need directly
    public String getSummary() {
        return String.format("Account[%s] Owner: %s Balance: %.2f", accountId, owner, balance);
    }
}

// -------------------------------------------------------------------------
// VIOLATION EXAMPLE — what NOT to do (for comparison)
// -------------------------------------------------------------------------
class BadBankAccount {

    public double balance;                    // public field — anyone can set balance = -99999
    public List<String> history = new ArrayList<>();  // caller can history.clear()

    // No validation, no encapsulation
    public void deposit(double amount) {
        balance += amount;  // no check if amount is negative
        history.add("Deposited: " + amount);
    }

    // Returns internal list directly — caller can modify it
    public List<String> getHistory() {
        return history;  // BAD: caller can do getHistory().clear()
    }
}

class EncapsulationDemo {
    public static void main(String[] args) {
        BankAccount account = new BankAccount("ACC001", "Alice", 1000.0);
        account.deposit(500.0);
        account.withdraw(200.0);

        System.out.println(account.getSummary());
        System.out.println("History: " + account.getTransactionHistory());

        // This will throw UnsupportedOperationException — internal list is protected
        try {
            account.getTransactionHistory().add("FAKE ENTRY");
        } catch (UnsupportedOperationException e) {
            System.out.println("Cannot modify transaction history externally — encapsulation works!");
        }
    }
}
