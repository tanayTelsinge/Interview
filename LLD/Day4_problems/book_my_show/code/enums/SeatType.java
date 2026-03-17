package Day4_problems.book_my_show.code.enums;

public enum SeatType {
    SILVER(100), GOLD(200), PLATINUM(500);

    private final double price;

    SeatType(double price) { this.price = price; }

    public double getPrice() { return price; }
}
