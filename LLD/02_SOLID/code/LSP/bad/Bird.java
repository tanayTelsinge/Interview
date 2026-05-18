package Day2_SOLID.code.LSP.bad;

/**
 * BAD EXAMPLE — LSP Violated.
 *
 * Penguin extends Bird but throws UnsupportedOperationException for fly().
 * This breaks the contract: anywhere a Bird is expected, a Penguin can't be substituted.
 *
 * Detection: if you're writing "throw new UnsupportedOperationException()" in a subclass,
 * it's an LSP violation signal — the hierarchy itself is wrong.
 */
class Bird {
    public void eat() { System.out.println("Bird is eating"); }
    public void fly() { System.out.println("Bird is flying"); }
}

class Eagle extends Bird {
    @Override
    public void fly() { System.out.println("Eagle soaring at 3000ft"); } // fine
}

class Penguin extends Bird {
    @Override
    public void fly() {
        throw new UnsupportedOperationException("Penguins can't fly!"); // LSP VIOLATED
    }
}

class BirdTrainer {
    // This method expects any Bird to be flyable — Penguin breaks it
    public void makeBirdFly(Bird bird) {
        bird.fly(); // throws exception when bird is Penguin — LSP violated
    }
}

class LSPViolationDemo {
    public static void main(String[] args) {
        BirdTrainer trainer = new BirdTrainer();
        trainer.makeBirdFly(new Eagle());   // works fine
        trainer.makeBirdFly(new Penguin()); // RUNTIME EXCEPTION — LSP broken
    }
}
