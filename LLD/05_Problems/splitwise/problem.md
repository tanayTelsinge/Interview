====================================================
  SPLITWISE — LLD Problem Statement
====================================================

Design an expense sharing application.

----------------------------------------------------
FUNCTIONAL REQUIREMENTS
----------------------------------------------------
1. Users can create Groups and add members
2. Any member can add an Expense to a group (who paid, how much, description)
3. Expenses can be split in 3 ways:
   - EQUAL      : divide equally among all participants
   - EXACT      : each participant pays a specified exact amount
   - PERCENTAGE : each participant pays a specified percentage
4. System tracks net balance for each user in a group
   (positive = owed money, negative = owes money)
5. Settlement: calculate minimum transactions needed to clear all debts

----------------------------------------------------
NON-FUNCTIONAL / CLARIFYING QUESTIONS
----------------------------------------------------
- Can a user be in multiple groups?           → Yes
- Does the payer also participate in the split? → Yes (payer can owe their own share)
- Currency?                                   → Single currency (INR)
- Concurrency needed?                         → No (single user app for now)
- Persistence?                                → In-memory only

----------------------------------------------------
OUT OF SCOPE
----------------------------------------------------
- Actual payments / payment gateway
- Push notifications
- Currency conversion

----------------------------------------------------
FLOW
----------------------------------------------------
Group created → members added
  User adds expense:
    → select split type + participants
    → SplitStrategy calculates each user's share
    → netBalances updated:
         paidBy  += totalAmount
         each participant -= their share
  Settlement:
    → separate users into creditors (balance > 0) and debtors (balance < 0)
    → greedy two-pointer: pair largest creditor with largest debtor
    → emit Transaction(from=debtor, to=creditor, amount=min(credit, debt))
    → repeat until all balanced

----------------------------------------------------
ENTITIES
----------------------------------------------------
- User        : userId, name, email
- Group       : groupId, name, List<User> members
- Split       : user, amount  (one user's share of an expense)
- Expense     : expenseId, description, amount, paidBy, List<Split>, group
- Transaction : from (debtor), to (creditor), amount  (settlement output)

----------------------------------------------------
PATTERNS APPLICABLE
----------------------------------------------------
- Strategy : SplitStrategy — EQUAL, EXACT, PERCENTAGE
  Adding new split type = new class, zero changes to ExpenseService
