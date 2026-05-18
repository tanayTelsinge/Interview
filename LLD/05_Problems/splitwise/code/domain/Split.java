package Day4_problems.splitwise.code.domain;

// Represents one user's share of an expense
public class Split {

    private User user;
    private double amount;

    public Split(User user, double amount) {
        this.user = user;
        this.amount = amount;
    }

    public User getUser()     { return user; }
    public double getAmount() { return amount; }
}
