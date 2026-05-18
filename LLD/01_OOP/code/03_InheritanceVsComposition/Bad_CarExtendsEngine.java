package code._03_InheritanceVsComposition;

/**
 * BAD EXAMPLE — Inheritance misused.
 *
 * Car extends Engine: Car IS-NOT-A Engine.
 * This is the most common senior-level mistake interviewers catch.
 *
 * Problems:
 *  1. Car exposes all Engine methods publicly (startEngine, stopEngine, getFuelLevel)
 *     — these shouldn't be part of Car's public API
 *  2. If Engine changes (e.g., we add ElectricEngine), Car breaks — fragile base class problem
 *  3. Can't swap engine type at runtime
 *  4. Can't test Car without a real Engine (no mocking)
 */
class Engine_Bad {
    private int horsepower;
    private double fuelLevel;

    public Engine_Bad(int horsepower) {
        this.horsepower = horsepower;
        this.fuelLevel = 100.0;
    }

    public void startEngine() {
        System.out.println("Engine started");
    }

    public void stopEngine() {
        System.out.println("Engine stopped");
    }

    public double getFuelLevel() { return fuelLevel; }
    public int getHorsepower() { return horsepower; }
}

// WRONG: Car IS-NOT-A Engine
class Car_Bad extends Engine_Bad {
    private String model;

    public Car_Bad(String model, int horsepower) {
        super(horsepower);
        this.model = model;
    }

    public void drive() {
        startEngine();  // reusing Engine behaviour via inheritance
        System.out.println(model + " is driving");
    }

    // Problem: Car now exposes startEngine(), stopEngine(), getFuelLevel()
    // as part of its own API — caller can do car.stopEngine() directly
    // These are implementation details leaking out
}

class BadDemo {
    public static void main(String[] args) {
        Car_Bad car = new Car_Bad("Toyota", 150);
        car.drive();

        // These shouldn't be accessible on a Car object — but they are, because of wrong inheritance
        car.startEngine();   // leaking Engine's internals
        car.stopEngine();    // caller controls internals directly
        System.out.println(car.getFuelLevel()); // exposing engine state on Car
    }
}
