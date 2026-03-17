package Day4_problems.parking_lot.code.strategies.pricingstrategy;

public class HourlyPricingStrategy implements PricingStrategy {
    
    private double pricePerHour;

    public HourlyPricingStrategy(double pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    @Override
    public double calculatePrice(int hoursParked) {
        return hoursParked * pricePerHour;
    }
}
