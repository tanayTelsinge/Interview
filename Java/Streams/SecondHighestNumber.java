package Streams;

import java.util.Arrays;
import java.util.*;

public class SecondHighestNumber {
    
    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(10,20, 20, 30, 40,40, 50);
        var res = nums.stream().sorted(Collections.reverseOrder()).skip(1).findFirst().get();
        System.out.println(res);

        //if every element is distinct return tru else false

        String s = "larij";

        for(int i = 0; i < s.length() / 2; i++) {
            
        }
    }
}
