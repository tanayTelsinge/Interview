package Day4_problems.splitwise.code.strategies;

import java.util.List;
import java.util.stream.Collectors;

import Day4_problems.splitwise.code.domain.Split;
import Day4_problems.splitwise.code.domain.User;

public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public List<Split> split(double totalAmount, List<User> users, List<Double> values) {
        double perPerson = totalAmount / users.size();
        return users.stream()
                .map(user -> new Split(user, perPerson))
                .collect(Collectors.toList());
    }
}
