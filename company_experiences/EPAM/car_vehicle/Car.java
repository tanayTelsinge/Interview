package interview_exp.EPAM.car_vehicle;

public class Car extends Vehicle {

    @Override
    public String publicMethod() {
        return "Car - public";
    }

    // static method hiding (not overriding)
    public static String staticMethod() {
        return "Car - static";
    }

    @Override
    protected String protectedMethod() {
        return "Car - protected";
    }

     @Override
    protected String onlyCarProtectedMethod() {
        return "Only Car - protected";
    }

    @Override
    String defaultMethod() {
        return "Car - default";
    }

    // private cannot be overridden - this is a new separate method
    private String privateMethod() {
        return "Car - private";
    }
}
