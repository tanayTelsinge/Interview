package Day4_problems.elevator_system.code.domain;

import Day4_problems.elevator_system.code.enums.Direction;

import java.util.UUID;

/**
 * Represents an external request made from a floor panel (UP / DOWN button press).
 */
public class Request {

    private final String requestId;
    private final int sourceFloor;
    private final Direction direction;

    public Request(int sourceFloor, Direction direction) {
        this.requestId  = UUID.randomUUID().toString();
        this.sourceFloor = sourceFloor;
        this.direction   = direction;
    }

    public String getRequestId()  { return requestId; }
    public int    getSourceFloor(){ return sourceFloor; }
    public Direction getDirection(){ return direction; }
}
