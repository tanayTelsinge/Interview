

/**
 * GOOD EXAMPLE — Composition over Inheritance.
 *
 * Car HAS-A Engine (injected via constructor).
 *
 * Benefits:
 *  1. Engine details are hidden — Car exposes only drive/stop
 *  2. Engine can be swapped at runtime (PetrolEngine, ElectricEngine, HybridEngine)
 *  3. Easy to test — inject a mock engine in unit tests
 *  4. Changing Engine class doesn't break Car (loose coupling)
 */

// Engine as an interface — program to abstraction, not implementation
interface Engine {
    void start();
    void stop();
    int getHorsepower();
}

// Concrete implementations — swappable
class PetrolEngine implements Engine {
    private final int horsepower;

    public PetrolEngine(int horsepower) { this.horsepower = horsepower; }

    @Override public void start() { System.out.println("Petrol engine started — vroom!"); }
    @Override public void stop() { System.out.println("Petrol engine stopped"); }
    @Override public int getHorsepower() { return horsepower; }
}

class ElectricEngine implements Engine {
    private final int horsepower;

    public ElectricEngine(int horsepower) { this.horsepower = horsepower; }

    @Override public void start() { System.out.println("Electric engine started — silent hum"); }
    @Override public void stop() { System.out.println("Electric engine stopped"); }
    @Override public int getHorsepower() { return horsepower; }
}

// Car HAS-A Engine — correct composition
class Car {
    private final String model;
    private final Engine engine;   // composed, not extended

    // Engine injected via constructor — dependency injection
    public Car(String model, Engine engine) {
        this.model = model;
        this.engine = engine;
    }

    public void drive() {
        engine.start();   // delegates to engine
        System.out.println(model + " is driving with " + engine.getHorsepower() + "hp");
    }

    public void park() {
        engine.stop();
        System.out.println(model + " parked");
    }

    // Engine internals are NOT exposed — clean Car API
    public String getModel() { return model; }
}

class CompositionDemo {
    public static void main(String[] args) {
        // Petrol car
        Car petrolCar = new Car("Toyota Camry", new PetrolEngine(150));
        petrolCar.drive();
        petrolCar.park();

        System.out.println();

        // Same Car class, different engine — swapped at construction time
        Car electricCar = new Car("Tesla Model 3", new ElectricEngine(300));
        electricCar.drive();
        electricCar.park();

        // petrolCar.start() — doesn't exist, Engine internals are hidden. Correct!
    }
}
