package Day4_problems.splitwise.code.strategies;

import java.util.ArrayList;
import java.util.List;

import Day4_problems.splitwise.code.domain.Split;
import Day4_problems.splitwise.code.domain.User;

public class PercentageSplitStrategy implements SplitStrategy {

    @Override
    public List<Split> split(double totalAmount, List<User> users, List<Double> values) {
        double totalPercent = values.stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(totalPercent - 100.0) > 0.001) {
            throw new IllegalArgumentException(
                "Percentages must sum to 100. Got: " + totalPercent);
        }
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            splits.add(new Split(users.get(i), totalAmount * values.get(i) / 100));
        }
        return splits;
    }
}
