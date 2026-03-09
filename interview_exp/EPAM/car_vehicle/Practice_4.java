package interview_exp.EPAM.car_vehicle;

/*
 * Method Override Scenarios: Vehicle v = new Car();
 *
 * +---------------------+-------------+------------------+-----------------------------+
 * | Modifier            | Overridable | Method Called    | Reason                      |
 * +---------------------+-------------+------------------+-----------------------------+
 * | public              | Yes         | Car              | Runtime polymorphism        |
 * | public static       | No (hidden) | Vehicle          | Resolved by reference type  |
 * | protected           | Yes         | Car              | Runtime polymorphism     
   | only Car protected  - compile time error Cannot reduce the visibility of the inherited method from VehicleJava
 * | default (package)   | Yes         | Car              | Runtime polymorphism        |
 * | private             | No          | Vehicle          | Not visible to subclass     |
 * +---------------------+-------------+------------------+-----------------------------+
 *
 * Rule: static and private break polymorphism. All others resolve at runtime.
 */
public class Practice_4 {
    public static void main(String[] args) {
        Vehicle v = new Car();

        // 1. public - runtime polymorphism -> Car's method called
        System.out.println("public:    " + v.publicMethod());       // Car - public

        // 2. static - method hiding -> reference type decides -> Vehicle's method called
        System.out.println("static:    " + Vehicle.staticMethod()); // Vehicle - static
        System.out.println("static:    " + Car.staticMethod());     // Car - static
        System.out.println("static with v:    " + v.staticMethod());     // Car - static
        // 3. protected - runtime polymorphism -> Car's method called
        System.out.println("protected: " + v.protectedMethod());    // Car - protected

        // 4. default (package-private) - runtime polymorphism -> Car's method called
        System.out.println("default:   " + v.defaultMethod());      // Car - default

        // 5. private - cannot be overridden -> Vehicle's method always called
        v.callPrivate();                                             // Vehicle - private
    }
}
