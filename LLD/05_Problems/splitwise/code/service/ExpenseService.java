package Day4_problems.splitwise.code.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Day4_problems.splitwise.code.domain.Expense;
import Day4_problems.splitwise.code.domain.Group;
import Day4_problems.splitwise.code.domain.Split;
import Day4_problems.splitwise.code.domain.User;
import Day4_problems.splitwise.code.strategies.SplitStrategy;

public class ExpenseService {

    // groupId → (userId → netBalance)
    // positive = this user is owed money; negative = this user owes money
    private final Map<String, Map<String, Double>> groupNetBalances = new HashMap<>();

    // userId → User (needed by SettlementService to resolve names)
    private final Map<String, User> userRegistry = new HashMap<>();

    public Expense addExpense(Group group, User paidBy, double amount,
                              String description, List<User> participants,
                              SplitStrategy splitStrategy, List<Double> values) {

        List<Split> splits = splitStrategy.split(amount, participants, values);

        String expenseId = "EXP_" + System.currentTimeMillis();
        Expense expense = new Expense(expenseId, description, amount, paidBy, splits, group);

        // register all participants (paidBy is expected to be in participants list)
        participants.forEach(u -> userRegistry.put(u.getUserId(), u));

        // update net balances
        Map<String, Double> balances = groupNetBalances
                .computeIfAbsent(group.getGroupId(), k -> new HashMap<>());

        // paidBy gets credit for the full amount
        balances.merge(paidBy.getUserId(), amount, Double::sum);

        // each participant gets debited their share
        splits.forEach(split ->
                balances.merge(split.getUser().getUserId(), -split.getAmount(), Double::sum));

        System.out.println("Added: \"" + description + "\" Rs." + amount
                + " paid by " + paidBy.getName());
        return expense;
    }

    public Map<String, Double> getNetBalances(Group group) {
        return Collections.unmodifiableMap(
                groupNetBalances.getOrDefault(group.getGroupId(), new HashMap<>()));
    }

    public Map<String, User> getUserRegistry() {
        return userRegistry;
    }
}
