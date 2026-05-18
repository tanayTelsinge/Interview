package Day4_problems.snake_and_ladder.code.strategies;

import java.util.Random;

public class StandardDice implements DiceStrategy {

    private final Random random = new Random();

    @Override
    public int roll() {
        return random.nextInt(6) + 1;
    }
}
