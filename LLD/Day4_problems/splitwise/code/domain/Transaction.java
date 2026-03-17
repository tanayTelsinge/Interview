package Day4_problems.splitwise.code.domain;

// Represents a settlement transaction: `from` pays `amount` to `to`
public class Transaction {

    private User from;   // debtor
    private User to;     // creditor
    private double amount;

    public Transaction(User from, User to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public User getFrom()      { return from; }
    public User getTo()        { return to; }
    public double getAmount()  { return amount; }

    @Override
    public String toString() {
        return from.getName() + " pays Rs." + String.format("%.2f", amount) + " to " + to.getName();
    }
}
