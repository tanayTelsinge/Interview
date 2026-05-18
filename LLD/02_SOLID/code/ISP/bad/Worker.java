package code.ISP.bad;

/**
 * BAD EXAMPLE — ISP Violated.
 *
 * Fat Worker interface forces Robot to implement eat() and sleep()
 * which make no sense for a machine.
 * Empty implementations = ISP violation signal.
 */
interface Worker {
    void work();
    void eat();   // Robots don't eat — forced to implement meaninglessly
    void sleep(); // Robots don't sleep — forced to implement meaninglessly
}

class HumanWorker implements Worker {
    @Override public void work() { System.out.println("Human working"); }
    @Override public void eat() { System.out.println("Human eating lunch"); }
    @Override public void sleep() { System.out.println("Human sleeping"); }
}

class Robot implements Worker {
    @Override public void work() { System.out.println("Robot working 24/7"); }
    @Override public void eat() { /* Robots don't eat — empty, meaningless */ }
    @Override public void sleep() { /* Robots don't sleep — empty, meaningless */ }
    // These empty methods are the ISP violation signal
}
