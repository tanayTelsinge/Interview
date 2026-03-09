package Streams;

import java.util.List;
import java.util.stream.Collectors;

public class AvgSalaryPerEmployee {

    public static void main(String[] args) {
        
        List<Employee> empList = List.of(
            new Employee("John", 50000, "New York"),
            new Employee("Jane", 60000, "Los Angeles"),
            new Employee("Doe", 55000, "Chicago")
        );

        // Given a List<Employee> with fields 
        // name, city, salary — write a stream to get average salary of employees grouped by city.

        var result = empList.stream()
                            .collect(
                                Collectors
                                .groupingBy(Employee::getCity,Collectors.averagingDouble(Employee::getSalary))
                            );

        System.out.println(result);
    }   
}