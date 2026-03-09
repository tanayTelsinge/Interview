package interview_exp.EPAM;
import java.util.ArrayList;
import java.util.List;

public class Practice {
    
    public static void main(String[] args) {
        
        List<String> list = new ArrayList<>();

        String s = "Test";
        int a = 10;
        a = 4; //local variable a is not effectively final
        list.add(s);

        List<String> res = list.stream().filter(str -> str.length() == a).toList();
        res.forEach(System.out::println);
    }
}
