package code.ISP.good;

// Human implements all three — it genuinely does all of them
public class HumanWorker implements Workable, Feedable {
    @Override public void work() { System.out.println("Human working"); }
    @Override public void eat() { System.out.println("Human eating lunch"); }
}
