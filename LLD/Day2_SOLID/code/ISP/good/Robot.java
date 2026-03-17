package code.ISP.good;

// Robot implements only what it actually does — no empty methods
public class Robot implements Workable {
    @Override public void work() { System.out.println("Robot working 24/7"); }
    // No eat(), no sleep() — correct. No empty stubs needed.
}

class WorkManager {
    // Works with Workable — both Human and Robot qualify
    public void assignWork(Workable worker) {
        worker.work();
    }

    // Only Feedable workers go to the cafeteria — Robot can't be passed here
    public void sendToCafeteria(Feedable worker) {
        worker.eat();
    }
}

class ISPDemo {
    public static void main(String[] args) {
        WorkManager manager = new WorkManager();
        HumanWorker human = new HumanWorker();
        Robot robot = new Robot();

        manager.assignWork(human);  // fine
        manager.assignWork(robot);  // fine

        manager.sendToCafeteria(human);  // fine
        // manager.sendToCafeteria(robot); // COMPILE ERROR — Robot is not Feedable. Correct!
    }
}
