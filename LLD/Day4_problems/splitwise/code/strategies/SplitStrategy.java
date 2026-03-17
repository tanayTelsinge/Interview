package Day4_problems.splitwise.code.strategies;

import java.util.List;

import Day4_problems.splitwise.code.domain.Split;
import Day4_problems.splitwise.code.domain.User;

public interface SplitStrategy {
    // values: null for EQUAL, exact amounts for EXACT, percentages for PERCENTAGE
    List<Split> split(double totalAmount, List<User> users, List<Double> values);
}
