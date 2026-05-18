package code.OCP.good;

public class UPIPayment implements PaymentMethod {

    private final String vpa; // Virtual Payment Address e.g. user@upi

    public UPIPayment(String vpa) { this.vpa = vpa; }

    @Override
    public void process(double amount) {
        System.out.println("Sending UPI payment of " + amount + " to " + vpa);
        // UPI-specific: VPA validation, UPI PIN flow, etc.
    }

    @Override
    public double calculateFee(double amount) {
        return 0; // UPI is free
    }

    @Override
    public String getName() { return "UPI"; }
}
