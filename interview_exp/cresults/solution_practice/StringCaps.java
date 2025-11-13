import java.util.Arrays;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StringCaps {


    public static void main(String[] args) {
         String s = "This is meyam";

        String[] arr = s.split(" ");

        StringBuilder res = new StringBuilder();
        for (String str : arr) {
            res.append(str.toUpperCase().charAt(0) + str.substring(1, str.length() - 1) + str.toUpperCase().charAt(str.length() - 1));
            res.append(" ");
        }
        System.out.println(res);
    }
}