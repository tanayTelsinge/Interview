

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class Test {
    

    public static void main(String[] args) {
        FileReader f = new FileReader(new File("D:\\Interview_clone\\javascript\\test.csv"));

        List<String> lines = f.readAllLines();

        System.out.println(lines.size()); //line count

        System.out.println(lines.split(",").size()); //column count


        
    }
}
