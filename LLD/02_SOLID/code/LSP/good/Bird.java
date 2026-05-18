package Day2_SOLID.code.LSP.good;

/**
 * GOOD — LSP fixed via interface segregation.
 *
 * Fix: restructure the hierarchy.
 * - Bird = base (eat, sleep — all birds do this)
 * - FlyingBird interface = only birds that CAN fly implement this
 * - Penguin extends Bird, does NOT implement FlyingBird — correct
 * - Eagle extends Bird AND implements FlyingBird — correct
 *
 * Now BirdTrainer works with FlyingBird reference — Penguin can never
 * be passed where a FlyingBird is expected. Compile-time safety.
 */
public abstract class Bird {
    public void eat() { System.out.println(getClass().getSimpleName() + " is eating"); }
    public void sleep() { System.out.println(getClass().getSimpleName() + " is sleeping"); }
}
