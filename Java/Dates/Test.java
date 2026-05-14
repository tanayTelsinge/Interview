package Dates;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Test {
    
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        System.out.println(today);

        LocalDateTime dt = LocalDateTime.now();
        System.out.println(dt);

        //create custom date
        LocalDate date = LocalDate.of(2025, 1, 29);

        //add sub
        date.plusDays(1);
        date.plusMonths(1);
        date.minusDays(1);
        date.minusMonths(1);

        //Date formatting - date to string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formatted = today.format(formatter);

        System.out.println(formatted);

        //Date formatting - string to date
        String dateStr = "05-04-2026";

        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        
        LocalDate date2 = LocalDate.parse(dateStr, formatter2);

        System.out.println(date2);

    }
}
