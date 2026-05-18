package Day2_SOLID.code.LSP.good;

// Penguin IS-A Bird but does NOT implement FlyingBird — correct, no fake fly()
public class Penguin extends Bird {
    public void swim() { System.out.println("Penguin swimming expertly"); }
}

class BirdTrainer {
    // Now expects FlyingBird — Penguin can NEVER be passed here (compile-time safety)
    public void makeBirdFly(FlyingBird bird) {
        bird.fly(); // safe — only FlyingBird implementations get here
    }

    // Penguin can be used as a Bird — LSP satisfied
    public void feedBird(Bird bird) {
        bird.eat(); // works for Eagle, Penguin, any Bird — LSP satisfied
    }
}

class LSPFixDemo {
    public static void main(String[] args) {
        Eagle eagle = new Eagle();
        Penguin penguin = new Penguin();
        BirdTrainer trainer = new BirdTrainer();

        trainer.makeBirdFly(eagle);           // fine
        // trainer.makeBirdFly(penguin);      // COMPILE ERROR — Penguin is not FlyingBird
                                              // LSP enforced at compile time, not runtime!

        trainer.feedBird(eagle);              // works — Eagle IS-A Bird
        trainer.feedBird(penguin);            // works — Penguin IS-A Bird
    }
}
