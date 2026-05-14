package Dates;

public class Period {
    String date;
    int value;

    public Period(String date, int val) {
        this.value = val;
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public int getValue() {
        return value;
    }
}
