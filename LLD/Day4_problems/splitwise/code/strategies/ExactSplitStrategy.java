package Day4_problems.splitwise.code.strategies;

import java.util.ArrayList;
import java.util.List;

import Day4_problems.splitwise.code.domain.Split;
import Day4_problems.splitwise.code.domain.User;

public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public List<Split> split(double totalAmount, List<User> users, List<Double> values) {
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - totalAmount) > 0.001) {
            throw new IllegalArgumentException(
                "Exact amounts must sum to total. Expected: " + totalAmount + ", Got: " + sum);
        }
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            splits.add(new Split(users.get(i), values.get(i)));
        }
        return splits;
    }
}
