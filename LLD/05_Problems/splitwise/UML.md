# Splitwise — UML Class Diagram

> Matches the actual implementation in 05_Problems/splitwise/code/

---

## Class Diagram

```
«enumeration»
SplitType
─────────
EQUAL
EXACT
PERCENTAGE


┌──────────────────┐     ┌──────────────────────────────┐
│      User         │     │            Group              │
├──────────────────┤     ├──────────────────────────────┤
│ - userId          │     │ - groupId                     │
│ - name            │     │ - name                        │
│ - email           │     │ - members: List<User>         │
└──────────────────┘     └──────────────────────────────┘


┌──────────────────────────────┐
│            Split              │
├──────────────────────────────┤
│ - user: User                  │  (one participant's share)
│ - amount: double              │
└──────────────────────────────┘


┌──────────────────────────────────────────┐
│                 Expense                   │
├──────────────────────────────────────────┤
│ - expenseId                               │
│ - description                             │
│ - amount: double                          │
│ - paidBy: User                            │
│ - splits: List<Split>               ◆    │  ← composition
│ - group: Group                            │
└──────────────────────────────────────────┘


┌──────────────────────────────────┐
│           Transaction             │
├──────────────────────────────────┤
│ - from: User   (debtor)           │
│ - to: User     (creditor)         │
│ - amount: double                  │
├──────────────────────────────────┤
│ + toString()                      │
└──────────────────────────────────┘


┌─────────────────────────────────────────────────────────────┐
│                      ExpenseService                          │
├─────────────────────────────────────────────────────────────┤
│ - groupNetBalances: Map<groupId, Map<userId, Double>>        │
│ - userRegistry: Map<userId, User>                           │
├─────────────────────────────────────────────────────────────┤
│ + addExpense(group, paidBy, amount, description,            │
│              participants, strategy, values): Expense        │
│ + getNetBalances(group): Map<userId, Double>                │
│ + getUserRegistry(): Map<userId, User>                      │
└─────────────────────────────────────────────────────────────┘
          │ uses
          ↓
┌──────────────────────────────────────────────┐
│  «interface»                                  │
│  SplitStrategy                                │
├──────────────────────────────────────────────┤
│ + split(totalAmount, users, values)           │
│   : List<Split>                               │
└──────────────────────────────────────────────┘
          ▲
          │ implements
    ┌─────┼──────────────────┐
    │     │                  │
EqualSplit  ExactSplit  PercentageSplit
Strategy    Strategy    Strategy


┌─────────────────────────────────────────────────────────────┐
│                    SettlementService                         │
├─────────────────────────────────────────────────────────────┤
│ - expenseService: ExpenseService                            │
├─────────────────────────────────────────────────────────────┤
│ + settle(group): List<Transaction>                          │
└─────────────────────────────────────────────────────────────┘
```

---

## Relationships Summary

| From → To | Type | Why |
|---|---|---|
| Group → User | **Association** | Group references members; Users exist independently |
| Expense → Split | **Composition ◆** | Splits are created for and owned by an Expense |
| Expense → User (paidBy) | **Association** | References User, doesn't own it |
| ExpenseService → SplitStrategy | **Dependency** | Passed per call; any impl substitutable |
| SettlementService → ExpenseService | **Dependency** | Injected via constructor |

---

## Net Balance Update Logic

```
When expense is added (paidBy = Alice, amount = 1200, equal split among 4):

  splits = [Alice=300, Bob=300, Carol=300, Dave=300]

  netBalance[Alice] += 1200   (she paid, gets credit)
  netBalance[Alice] -= 300    (her own share)
  netBalance[Bob]   -= 300
  netBalance[Carol] -= 300
  netBalance[Dave]  -= 300

  Result: Alice = +900, Bob = -300, Carol = -300, Dave = -300
```

---

## Settlement Algorithm (Greedy Two-Pointer)

```
Given net balances:
  Alice = +900, Bob = -300, Carol = -300, Dave = -300

creditors = [Alice(900)]
debtors   = [Bob(300), Carol(300), Dave(300)]

Round 1: settle min(900, 300) = 300 → Bob pays Alice Rs.300
         Alice: 900-300=600, Bob: 0 ✓

Round 2: settle min(600, 300) = 300 → Carol pays Alice Rs.300
         Alice: 600-300=300, Carol: 0 ✓

Round 3: settle min(300, 300) = 300 → Dave pays Alice Rs.300
         All zeroed out ✓

3 transactions instead of potentially 6+ pairwise.
```

---

## Key Design Decisions to Mention in Interview

1. **Strategy pattern** for splits — `EqualSplitStrategy`, `ExactSplitStrategy`, `PercentageSplitStrategy` each independently implement `SplitStrategy`; adding `SplitBySharesStrategy` = zero changes to `ExpenseService`
2. **Net balance map** (not pairwise) — simpler to maintain; sufficient for settlement. Mention pairwise as a future enhancement for "Alice owes Bob Rs.X" display
3. **paidBy also participates in splits** — their share is debited; net = amount paid − own share
4. **Validation in strategies** — EXACT checks sum == total; PERCENTAGE checks sum == 100 before any state mutation (fail-fast)
5. **Greedy two-pointer settlement** — O(N), pairs largest creditor with largest debtor; not always globally optimal (min-transactions is NP-hard) but acceptable for interviews
