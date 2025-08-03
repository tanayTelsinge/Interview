package interview_exp.Java;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Streams {

    public static void main(String[] args) {

        List<String> names = Arrays.asList("Tanay", "Ram", "Shyam", "Lam");

        List<Integer> numbers = List.of(1,2,3,4,5,6,7);

        //Find the second highest number in a list using streams
        int secondHighest = numbers.stream().sorted(Collections.reverseOrder()).skip(1).findFirst().get();
        System.out.println(secondHighest);

        //How can you group elements of a list by a property using streams? Provide an example.
        List<Person> persons = populatePersons();

        //group by age
        Map<Integer, List<Person>> groupByAge = persons.stream().collect(Collectors.groupingBy(Person::getAge));
        System.out.println(groupByAge);

        //Explain the difference between map and flatMap with examples

        List<String> upperCasedNames = persons.stream().map(person -> person.getName().toUpperCase()).toList();
        System.out.println(upperCasedNames);
        //to flatten internal list
        List<String> totalHobbies = persons.stream().flatMap(person -> person.getHobbies().stream()).collect(Collectors.toList());
        System.out.println(totalHobbies);


        //count frequency of each word in a sentence

        String sentenceOne = "I am okay and you are not okay";
        String sentenceTwo = "You are fine I am not fine";
        List<String> sentences = List.of(sentenceOne, sentenceTwo);
        Map<String, Long> test = Stream.of(sentenceOne.split(" ")).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        System.out.println(test);

        //if between list sentence find frequency then
        Map<String, Long> wordCount = sentences.stream().flatMap(s -> Stream.of(s.split(" "))).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        System.out.println(wordCount);

        //How do you group a list of strings by their length and return a count per group?
        Map<Integer, Long> groupByLengthCount = names.stream().collect(Collectors.groupingBy(String::length, Collectors.counting()));

        System.out.println(groupByLengthCount);

        // How would you collect a stream of items into an unmodifiable list?
        List<String> unmodifiableList = names.stream().collect(Collectors.collectingAndThen(
                Collectors.toList(),
                Collections::unmodifiableList
        ));

        //unmodifiableList.add("test");

        //You want to map a list of strings to a map where the key is string length and values are comma-separated strings. How would you handle key collisions?

        //Find second non repeating character in stress. s = "stress"; solution - r

        String s = "stress";

        Character first = s.chars().mapToObj(c -> (char) c).collect(Collectors.groupingBy(
                Function.identity(),
                LinkedHashMap::new,
                Collectors.counting()
        )).entrySet().stream().filter(entry -> entry.getValue() == 1).skip(1).findFirst().get().getKey();

        System.out.println(first);
    }


    public static List<Person> populatePersons() {
        List<Person> persons = new ArrayList<>();
        Person first = new Person("Tanay", 20);
        Person second = new Person("Ramesh", 30);
        Person third = new Person("Raj", 20);

        List<String> hobbiesFirst = List.of("Tennis", "Cricket");
        List<String> hobbiesSecond = List.of("Chess");
        List<String> hobbiesThird = List.of("Gaming");

        first.setHobbies(hobbiesFirst);
        second.setHobbies(hobbiesSecond);
        third.setHobbies(hobbiesThird);

        persons.add(first);
        persons.add(second);
        persons.add(third);

        return persons;
    }
}