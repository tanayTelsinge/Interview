package Day4_problems.digital_wallet.code.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import Day4_problems.digital_wallet.code.enums.TransactionStatus;
import Day4_problems.digital_wallet.code.enums.TransactionType;

public class Transaction {

    private String txnId;
    private String walletId;
    private double amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private LocalDateTime timestamp;

    public Transaction(String walletId, double amount, TransactionType type, TransactionStatus status, String description) {
        this.txnId = UUID.randomUUID().toString();
        this.walletId = walletId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public String getTxnId() {
        return txnId;
    }

    public String getWalletId() {
        return walletId;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
