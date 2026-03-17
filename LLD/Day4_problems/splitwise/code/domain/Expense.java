package Day4_problems.splitwise.code.domain;

import java.util.List;

public class Expense {

    private String expenseId;
    private String description;
    private double amount;
    private User paidBy;
    private List<Split> splits;
    private Group group;

    public Expense(String expenseId, String description, double amount,
                   User paidBy, List<Split> splits, Group group) {
        this.expenseId = expenseId;
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splits = splits;
        this.group = group;
    }

    public String getExpenseId()    { return expenseId; }
    public String getDescription()  { return description; }
    public double getAmount()       { return amount; }
    public User getPaidBy()         { return paidBy; }
    public List<Split> getSplits()  { return splits; }
    public Group getGroup()         { return group; }
}
