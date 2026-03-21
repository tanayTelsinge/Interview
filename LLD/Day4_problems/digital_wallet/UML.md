# Digital Wallet — UML Class Diagram

> Matches the actual implementation in Day4_problems/digital_wallet/code/
> This is what you'd sketch on a whiteboard in the first 10 minutes.

---

## Enums

```
«enumeration»          «enumeration»          «enumeration»
TransactionType        TransactionStatus      WalletStatus
───────────────        ─────────────────      ────────────
CREDIT                 SUCCESS                ACTIVE
DEBIT                  FAILED                 BLOCKED
```

---

## Class Diagram

```
┌──────────────────────────┐
│          User             │
├──────────────────────────┤
│ - userId: String          │
│ - name: String            │
│ - email: String           │
│ - wallet: Wallet     ◆    │  ← composition (wallet owned by user)
├──────────────────────────┤
│ + getUserId()             │
│ + getName()               │
│ + getEmail()              │
│ + getWallet()             │
└──────────────────────────┘


┌──────────────────────────────────────────────┐
│                   Wallet                      │
├──────────────────────────────────────────────┤
│ - walletId: String                            │
│ - userId: String                              │
│ - balance: double                             │
│ - status: WalletStatus                        │
│ - transactions: List<Transaction>        ◆    │  ← composition
├──────────────────────────────────────────────┤
│ + credit(amount, description): void           │  ← synchronized
│ + debit(amount, description): void            │  ← synchronized; throws InsufficientFundsException
│ + getBalance(): double                        │
│ + getTransactions(): List<Transaction>        │  ← unmodifiable
│ + getWalletId(): String                       │
│ + getUserId(): String                         │
│ + getStatus(): WalletStatus                   │
└──────────────────────────────────────────────┘


┌─────────────────────────────────────────┐
│              Transaction                 │
├─────────────────────────────────────────┤
│ - txnId: String  (UUID)                  │
│ - walletId: String                       │
│ - amount: double                         │
│ - type: TransactionType                  │
│ - status: TransactionStatus              │
│ - description: String                    │
│ - timestamp: LocalDateTime               │
├─────────────────────────────────────────┤
│ + getTxnId()                             │
│ + getWalletId()                          │
│ + getAmount()                            │
│ + getType()                              │
│ + getStatus()                            │
│ + getDescription()                       │
│ + getTimestamp()                         │
└─────────────────────────────────────────┘


┌──────────────────────────────────────────────────┐
│                  WalletService                    │
├──────────────────────────────────────────────────┤
│ + addMoney(wallet, amount, paymentStrategy): void │
│ + transfer(from, to, amount): void                │  ← deadlock-safe
│ + getBalance(wallet): double                      │
│ + getTransactionHistory(wallet): List<Transaction>│
└──────────────────────────────────────────────────┘
          │ uses
          ↓
┌──────────────────────────┐
│  «interface»              │
│   PaymentStrategy         │
├──────────────────────────┤
│ + getMethodName(): String │
│ + pay(amount): boolean    │
└──────────────────────────┘
          ▲
          │ implements
  ┌───────┼───────────────┐
  │       │               │
UPIStrategy  CardStrategy  NetBankingStrategy
  "UPI"      "CARD"        "NET_BANKING"


«exception»
InsufficientFundsException
  extends RuntimeException
```

---

## Concurrency: Transfer Deadlock

### Problem

Two threads transferring between the same pair of wallets in opposite directions:

```
Thread A: lock(Wallet1) → waiting for lock(Wallet2)
Thread B: lock(Wallet2) → waiting for lock(Wallet1)
→ Deadlock: both threads wait forever
```

### Fix: Lock Ordering by walletId

Always acquire the lock for the lexicographically lower walletId first. Both threads
then attempt to lock in the same order, eliminating circular wait.

```java
// In WalletService.transfer()
Wallet first  = from.getWalletId().compareTo(to.getWalletId()) < 0 ? from : to;
Wallet second = (first == from) ? to : from;

synchronized (first) {
    synchronized (second) {
        from.debit(amount, "Transfer to wallet " + to.getWalletId());
        to.credit(amount, "Transfer from wallet " + from.getWalletId());
    }
}
```

Thread A and Thread B both try to lock W001 first — one wins and proceeds;
the other blocks on W001 (not on W002), so no circular dependency exists.

---

## Relationships Summary

| From → To | Type | Why |
|---|---|---|
| User → Wallet | **Composition ◆** | Wallet is created and owned by User |
| Wallet → Transaction | **Composition ◆** | Transactions cannot exist outside a Wallet |
| WalletService → PaymentStrategy | **Dependency** | Uses interface; strategy passed per call |
| WalletService → Wallet | **Dependency** | Operates on wallets passed by caller |
| UPIStrategy / CardStrategy / NetBankingStrategy → PaymentStrategy | **Realization ◁---** | implements interface |
| Wallet → InsufficientFundsException | **throws** | Fail-fast guard before any state change |

---

## Key Design Points

1. **synchronized credit/debit** — balance update and transaction record are a single atomic unit; no thread can observe a balance change without a corresponding transaction entry.

2. **Deadlock-free transfer** — locks are always acquired in ascending walletId order, eliminating circular wait regardless of how many concurrent transfers target the same wallet pair.

3. **Strategy pattern for payment methods** — UPI, Card, and NetBanking are interchangeable at call time; adding a new method (e.g., `CryptoStrategy`) requires zero changes to `WalletService`.

4. **Wallet owns transactions** — the transaction list lives inside `Wallet`, making it the single source of truth; history retrieval is O(1) lookup with no join or separate table needed.

5. **InsufficientFundsException** — thrown before any balance mutation, so no partial state (e.g., deducted but not credited) can arise; the caller catches it and the wallet remains unchanged.
