package interview_exp.EPAM.car_vehicle;

public class Vehicle {

    public String publicMethod() {
        return "Vehicle - public";
    }

    public static String staticMethod() {
        return "Vehicle - static";
    }

    protected String protectedMethod() {
        return "Vehicle - protected";
    }

    public String onlyCarProtectedMethod() {
        return "Vehicle - only vehicle Protected";
    }

    String defaultMethod() {
        return "Vehicle - default";
    }

    private String privateMethod() {
        return "Vehicle - private";
    }

    public void callPrivate() {
        System.out.println("privateMethod: " + privateMethod());
    }
}
