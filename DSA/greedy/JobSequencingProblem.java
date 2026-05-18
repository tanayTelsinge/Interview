package DSA.greedy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


class Profit {
    int profit;
    int deadline;

    public Profit(int profit, int deadline) {
        this.profit = profit;
        this.deadline = deadline;
    }

    public int getProfit() {
        return profit;
    }

    public int getDeadline() {
        return deadline;
    }
}
public class JobSequencingProblem {

    
    public static void main(String[] args) {
        int[] profit = {100, 90, 50};
        int[] deadline = {1, 1, 2};

        List<Profit> profits = new ArrayList<>();
        for(int i = 0; i < profit.length; i++) {
            profits.add(new Profit(profit[i], deadline[i] - 1));
        }

        int max = Arrays.stream(deadline).max().getAsInt();

        profits.sort(Comparator.comparingInt(Profit::getProfit).reversed());

        int[] arr = new int[max];
        int totalProfit = 0;
        int noOfJobsDone = 0;
        for(Profit pro : profits) {
            int dline = pro.getDeadline();
            for(int i = dline; i >= 0; i--) {
                if (arr[i] == 0) {
                    arr[i] = pro.getProfit();
                    totalProfit+= pro.getProfit();
                    noOfJobsDone++;
                    break;
                }
            }
        }
        
        System.out.println(" totalProfit " + totalProfit);
        System.out.println(" noOfJobsDone : " + noOfJobsDone);
    }
}