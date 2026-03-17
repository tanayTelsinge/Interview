package Day2_SOLID.code.LSP.good;

// Eagle IS-A Bird AND CAN fly — both correct
public class Eagle extends Bird implements FlyingBird {
    @Override
    public void fly() { System.out.println("Eagle soaring at 3000ft"); }
}
