package Dates;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Comparator;

public class PeriodProblemCiti {

    public static void main(String[] args) {

        List<Period> list = new ArrayList<>();
        list.add(new Period("10-01-2024", 100));
        list.add(new Period("05-01-2024", 60));
        list.add(new Period("20-01-2024", 150));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        List<Period> sortedList = list.stream().sorted(Comparator.comparing(x -> LocalDate.parse(x.getDate(), formatter))).collect(Collectors.toList());
        
        String[] arr = new String[2];
        int max = 0;

        for(int i = 1; i < sortedList.size(); i++) {
            Period prev = sortedList.get(i - 1);
            Period curr = sortedList.get(i);

            int diff = Math.abs(curr.getValue() - prev.getValue());
            if (diff > max) {
                arr[0] = prev.getDate();
                arr[1] = curr.getDate();
                max = diff;
            }
        }

        Arrays.stream(arr).forEach(System.out::println);
    }
}
