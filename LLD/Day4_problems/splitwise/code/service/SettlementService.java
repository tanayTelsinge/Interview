package Day4_problems.splitwise.code.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import Day4_problems.splitwise.code.domain.Group;
import Day4_problems.splitwise.code.domain.Transaction;
import Day4_problems.splitwise.code.domain.User;

public class SettlementService {

    private final ExpenseService expenseService;

    public SettlementService(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    public List<Transaction> settle(Group group) {
        Map<String, Double> netBalances = expenseService.getNetBalances(group);
        Map<String, User> userRegistry  = expenseService.getUserRegistry();

        // Separate into creditors (owed money) and debtors (owe money)
        List<String> creditorIds    = new ArrayList<>();
        List<Double> creditAmounts  = new ArrayList<>();
        List<String> debtorIds      = new ArrayList<>();
        List<Double> debtAmounts    = new ArrayList<>();

        for (Map.Entry<String, Double> entry : netBalances.entrySet()) {
            if (entry.getValue() > 0.001) {
                creditorIds.add(entry.getKey());
                creditAmounts.add(entry.getValue());
            } else if (entry.getValue() < -0.001) {
                debtorIds.add(entry.getKey());
                debtAmounts.add(-entry.getValue()); // store as positive
            }
        }

        // Sort descending so greedy pairs largest creditor with largest debtor,
        // minimising the number of transactions
        sortDescending(creditorIds, creditAmounts);
        sortDescending(debtorIds, debtAmounts);

        // Greedy two-pointer
        List<Transaction> transactions = new ArrayList<>();
        int i = 0, j = 0;

        while (i < creditorIds.size() && j < debtorIds.size()) {
            double credit = creditAmounts.get(i);
            double debt   = debtAmounts.get(j);
            double settleAmount = Math.min(credit, debt);

            transactions.add(new Transaction(
                    userRegistry.get(debtorIds.get(j)),
                    userRegistry.get(creditorIds.get(i)),
                    settleAmount));

            creditAmounts.set(i, credit - settleAmount);
            debtAmounts.set(j, debt   - settleAmount);

            if (creditAmounts.get(i) < 0.001) i++;
            if (debtAmounts.get(j)   < 0.001) j++;
        }

        return transactions;
    }

    private void sortDescending(List<String> ids, List<Double> amounts) {
        // Sort both lists together by amount descending
        List<Map.Entry<String, Double>> paired = new ArrayList<>();
        for (int k = 0; k < ids.size(); k++) {
            paired.add(Map.entry(ids.get(k), amounts.get(k)));
        }
        paired.sort(Comparator.comparingDouble(Map.Entry<String, Double>::getValue).reversed());
        for (int k = 0; k < paired.size(); k++) {
            ids.set(k, paired.get(k).getKey());
            amounts.set(k, paired.get(k).getValue());
        }
    }
}
