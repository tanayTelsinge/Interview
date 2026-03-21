package Day4_problems.digital_wallet.code.domain;

public class User {

    private String userId;
    private String name;
    private String email;
    private Wallet wallet;

    public User(String userId, String name, String email, Wallet wallet) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.wallet = wallet;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Wallet getWallet() {
        return wallet;
    }
}
