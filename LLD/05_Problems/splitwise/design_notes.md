- ExpenseService: addExpense() — runs SplitStrategy, updates groupNetBalances map, registers users.
- SettlementService: settle() — greedy two-pointer on creditors + debtors lists.
- SplitStrategy (interface): split(totalAmount, users, values) → List<Split>.
  - EqualSplitStrategy      : totalAmount / users.size() per person
  - ExactSplitStrategy      : validates sum == totalAmount, maps values to splits
  - PercentageSplitStrategy : validates sum == 100%, maps percentages to splits
- netBalances: Map<groupId, Map<userId, Double>>
  - paidBy  gets +totalAmount (credit)
  - each split user gets -splitAmount (debit)
  - net = what others owe you minus what you owe others


Flow:
addExpense(group, paidBy, amount, description, participants, strategy, values):
  → splits = strategy.split(amount, participants, values)
  → balances[group][paidBy]   += amount
  → balances[group][splitUser] -= splitAmount  (for each split)

settle(group):
  → separate balances into:
       creditors: balance > 0  (owed money)
       debtors:   balance < 0  (owe money, stored as positive)
  → greedy two-pointer:
       settleAmount = min(creditAmounts[i], debtAmounts[j])
       emit Transaction(debtor, creditor, settleAmount)
       reduce both by settleAmount
       advance pointer that reached 0
  → O(N log N) if sorted, O(N) with two-pointer on unsorted

Example:
  Alice pays 1200 dinner (equal 4-way: 300 each)
    Alice: +1200 - 300 = +900
    Bob/Carol/Dave: -300 each

  Bob pays 800 hotel (exact: A=200, B=200, C=300, D=100)
    Bob: +800 - 200 = +600 cumulative
    Alice: -200, Carol: -300, Dave: -100 cumulative

  Carol pays 600 cab (40/30/20/10%)
    Carol: +600 - 120 = +480 cumulative
    etc.

  Settlement collapses all debts into minimum transactions.


Design Patterns:

- Strategy : SplitStrategy (Equal / Exact / Percentage)
  - New split type = new class only, ExpenseService unchanged (OCP)


SOLID Principles:

- SRP  : ExpenseService=balance tracking, SettlementService=settlement algorithm, Strategy=split logic
- OCP  : New split type = new SplitStrategy impl, no existing class changes
- LSP  : All SplitStrategy impls substitutable
- DIP  : ExpenseService depends on SplitStrategy interface, not concrete classes
- ISP  : SplitStrategy has single focused method


Key decisions worth mentioning in interview:

1. netBalance map (not pairwise) — simpler to maintain and sufficient for settlement.
   Pairwise (A owes B Rs.X) gives more detail but is harder to settle optimally.

2. Settlement greedy algorithm: pair max creditor with max debtor each round.
   Not always globally optimal (NP-hard for minimum transactions), but good enough for interviews.
   Mention: "for optimal minimum-transactions, this becomes a graph problem."

3. paidBy is ALSO a participant in splits — their share is debited from their own balance.
   Net effect: paidBy's balance = amount - their_own_share (not the full amount).

4. SplitStrategy validates inputs (sum check for EXACT, 100% check for PERCENTAGE)
   before any balance update — fail fast.


Known limitations / future scope:

- Pairwise balance tracking for "Alice owes Bob Rs.X" style display
- Multi-currency support
- Settle individual pair (not full group settlement)
