package Day4_problems.splitwise.code;

import java.util.List;

import Day4_problems.splitwise.code.domain.Group;
import Day4_problems.splitwise.code.domain.Transaction;
import Day4_problems.splitwise.code.domain.User;
import Day4_problems.splitwise.code.service.ExpenseService;
import Day4_problems.splitwise.code.service.SettlementService;
import Day4_problems.splitwise.code.strategies.EqualSplitStrategy;
import Day4_problems.splitwise.code.strategies.ExactSplitStrategy;
import Day4_problems.splitwise.code.strategies.PercentageSplitStrategy;

public class Solution {

    public static void main(String[] args) {
        ExpenseService expenseService       = new ExpenseService();
        SettlementService settlementService = new SettlementService(expenseService);

        // Users
        User alice = new User("U1", "Alice", "alice@example.com");
        User bob   = new User("U2", "Bob",   "bob@example.com");
        User carol = new User("U3", "Carol", "carol@example.com");
        User dave  = new User("U4", "Dave",  "dave@example.com");

        Group group = new Group("G1", "Goa Trip", List.of(alice, bob, carol, dave));

        System.out.println("=== Adding Expenses ===\n");

        // Alice pays Rs.1200 for dinner — split equally among all 4
        expenseService.addExpense(group, alice, 1200, "Dinner",
                List.of(alice, bob, carol, dave),
                new EqualSplitStrategy(), null);

        // Bob pays Rs.800 for hotel — exact split: Alice=200, Bob=200, Carol=300, Dave=100
        expenseService.addExpense(group, bob, 800, "Hotel",
                List.of(alice, bob, carol, dave),
                new ExactSplitStrategy(), List.of(200.0, 200.0, 300.0, 100.0));

        // Carol pays Rs.600 for cab — percentage split: 40%, 30%, 20%, 10%
        expenseService.addExpense(group, carol, 600, "Cab",
                List.of(alice, bob, carol, dave),
                new PercentageSplitStrategy(), List.of(40.0, 30.0, 20.0, 10.0));

        // Print net balances
        System.out.println("\n=== Net Balances ===\n");
        expenseService.getNetBalances(group).forEach((userId, balance) -> {
            String name = expenseService.getUserRegistry().get(userId).getName();
            System.out.printf("%-8s : %+.2f%n", name, balance);
        });

        // Settle
        System.out.println("\n=== Settlement Transactions ===\n");
        List<Transaction> transactions = settlementService.settle(group);
        transactions.forEach(System.out::println);
    }
}
