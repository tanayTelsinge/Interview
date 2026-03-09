package Streams;

public class Employee {
    
    private String name;
    private double salary;
    private String city;

    public Employee(String name, double salary, String city) {
        this.name = name;
        this.city = city;
        this.salary = salary;
    }

    public String getCity() {
        return city;
    }

    public String getName() {
        return name;
    }

    public double getSalary() {
        return salary;
    }

}

