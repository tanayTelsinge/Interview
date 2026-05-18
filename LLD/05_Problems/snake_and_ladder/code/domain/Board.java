package Day4_problems.snake_and_ladder.code.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {

    private int size;
    // maps landing position → final position (covers both snakes and ladders)
    private Map<Integer, Integer> cellEffects;

    public Board(int size, List<Snake> snakes, List<Ladder> ladders) {
        this.size = size;
        this.cellEffects = new HashMap<>();
        for (Snake snake : snakes)
            cellEffects.put(snake.getHead(), snake.getTail());
        for (Ladder ladder : ladders)
            cellEffects.put(ladder.getBottom(), ladder.getTop());
    }

    // Returns the final position after applying any snake or ladder effect
    public int resolvePosition(int position) {
        return cellEffects.getOrDefault(position, position);
    }

    public int getSize() { return size; }
}
