package Day4_problems.snake_and_ladder.code.domain;

public class Snake {

    private int head;
    private int tail;

    public Snake(int head, int tail) {
        this.head = head;
        this.tail = tail;
    }

    public int getHead() { return head; }
    public int getTail() { return tail; }
}
