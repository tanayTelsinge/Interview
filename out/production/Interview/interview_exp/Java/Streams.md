## Java Streams Overview
Java Streams are a feature introduced in Java 8 that allow functional-style operations on collections of elements. Streams support operations such as filtering, mapping, and reducing, making data processing concise and readable.

### Stream Operations by Type

#### Intermediate Operations
- **filter(Predicate<T> predicate)**: Filters elements based on a condition.
  ```java
  List<Integer> evens = numbers.stream().filter(n -> n % 2 == 0).collect(Collectors.toList());
  ```
- **map(Function<T,R> mapper)**: Transforms each element.
  ```java
  List<String> upper = names.stream().map(String::toUpperCase).collect(Collectors.toList());
  ```
- **flatMap(Function<T,Stream<R>> mapper)**: Flattens nested streams.
  ```java
  List<String> words = sentences.stream().flatMap(s -> Arrays.stream(s.split(" "))).collect(Collectors.toList());
  ```
- **distinct()**: Removes duplicates.
  ```java
  List<Integer> unique = numbers.stream().distinct().collect(Collectors.toList());
  ```
- **sorted() / sorted(Comparator<T> comparator)**: Sorts elements.
  ```java
  List<String> sortedNames = names.stream().sorted().collect(Collectors.toList());
  List<String> sortedByLength = names.stream().sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
  ```
- **peek(Consumer<T> action)**: Performs an action on each element as it passes through.
  ```java
  names.stream().peek(System.out::println).collect(Collectors.toList());
  ```
- **limit(long maxSize)**: Limits the number of elements.
  ```java
  List<Integer> firstThree = numbers.stream().limit(3).collect(Collectors.toList());
  ```
- **skip(long n)**: Skips the first n elements.
  ```java
  List<Integer> skipped = numbers.stream().skip(2).collect(Collectors.toList());
  ```

#### Terminal Operations
- **forEach(Consumer<T> action)**: Performs an action for each element.
  ```java
  names.stream().forEach(System.out::println);
  ```
- **collect(Collector<T,A,R> collector)**: Converts the stream into a collection.
  ```java
  Set<String> nameSet = names.stream().collect(Collectors.toSet());
  ```
- **reduce(BinaryOperator<T> accumulator)**: Combines elements to produce a single result.
  ```java
  int sum = numbers.stream().reduce(0, Integer::sum);
  ```
- **toArray()**: Converts the stream to an array.
  ```java
  String[] nameArray = names.stream().toArray(String[]::new);
  ```
- **min(Comparator<T> comparator)**: Finds the minimum element.
  ```java
  Optional<Integer> min = numbers.stream().min(Integer::compare);
  ```
- **max(Comparator<T> comparator)**: Finds the maximum element.
  ```java
  Optional<Integer> max = numbers.stream().max(Integer::compare);
  ```
- **count()**: Returns the number of elements.
  ```java
  long count = names.stream().count();
  ```

#### Short-Circuiting Operations
- **anyMatch(Predicate<T> predicate)**: Returns true if any element matches the condition.
  ```java
  boolean hasBob = names.stream().anyMatch(name -> name.equals("Bob"));
  ```
- **allMatch(Predicate<T> predicate)**: Returns true if all elements match the condition.
  ```java
  boolean allShort = names.stream().allMatch(name -> name.length() < 10);
  ```
- **noneMatch(Predicate<T> predicate)**: Returns true if no elements match the condition.
  ```java
  boolean noneEmpty = names.stream().noneMatch(String::isEmpty);
  ```
- **findFirst()**: Returns the first element, if present.
  ```java
  Optional<String> first = names.stream().findFirst();
  ```
- **findAny()**: Returns any element, if present.
  ```java
  Optional<String> any = names.stream().findAny();
  ```

### Example Usage
```java
List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "Bob");
List<String> filtered = names.stream()
    .filter(name -> name.startsWith("B"))
    .distinct()
    .collect(Collectors.toList());
System.out.println(filtered); // Output: [Bob]
```

## Complex Interview Questions on Java Streams

1. **How would you find the second highest number in a list using streams?**
2. **How can you group elements of a list by a property using streams? Provide an example.**
3. **Explain the difference between `map` and `flatMap` with examples.**
4. **How do you handle exceptions inside stream operations?**
5. **How can you parallelize stream operations, and what are the caveats?**
6. **Write a stream pipeline to filter, sort, and collect objects based on multiple conditions.**
7. **How do you remove duplicates from a list of custom objects using streams?**
8. **What is the difference between intermediate and terminal operations? Give examples.**
9. **How would you count the frequency of each word in a list of sentences using streams?**
10. **How do you convert a list of objects to a map using streams?**
11. **What are the performance implications of using streams vs. traditional loops?**
12. **How do you find the first non-repeated character in a string using streams?**
13. **How can you use streams to merge two lists and remove duplicates?**
14. **Explain lazy evaluation in streams with an example.**
15. **How do you use `Collectors.partitioningBy` and `Collectors.groupingBy`?**

## Solutions to Complex Interview Questions on Java Streams

1. **Find the second highest number in a list using streams**
   ```java
   Optional<Integer> secondHighest = numbers.stream()
       .distinct()
       .sorted(Comparator.reverseOrder())
       .skip(1)
       .findFirst();
   ```
2. **Group elements of a list by a property using streams**
   ```java
   Map<Integer, List<String>> groupedByLength = names.stream()
       .collect(Collectors.groupingBy(String::length));
   ```
3. **Difference between map and flatMap**
   ```java
   // map example
   List<Integer> lengths = names.stream().map(String::length).collect(Collectors.toList());
   // flatMap example
   List<String> words = sentences.stream().flatMap(s -> Arrays.stream(s.split(" "))).collect(Collectors.toList());
   ```
4. **Handle exceptions inside stream operations**
   ```java
   List<Integer> parsed = strings.stream()
       .map(s -> {
           try { return Integer.parseInt(s); }
           catch (NumberFormatException e) { return 0; }
       })
       .collect(Collectors.toList());
   ```
5. **Parallelize stream operations and caveats**
   ```java
   List<String> result = names.parallelStream()
       .filter(name -> name.length() > 3)
       .collect(Collectors.toList());
   // Caveat: Not always faster, thread-safety issues with shared mutable state
   ```
6. **Filter, sort, and collect objects based on multiple conditions**
   ```java
   List<String> filteredSorted = names.stream()
       .filter(name -> name.startsWith("A") && name.length() > 3)
       .sorted()
       .collect(Collectors.toList());
   ```
7. **Remove duplicates from a list of custom objects using streams**
   ```java
   List<Person> uniquePersons = persons.stream()
       .collect(Collectors.collectingAndThen(
           Collectors.toMap(Person::getId, Function.identity(), (a, b) -> a),
           m -> new ArrayList<>(m.values())
       ));
   ```
8. **Difference between intermediate and terminal operations**
   - Intermediate: `filter`, `map`, `sorted` (return Stream)
   - Terminal: `collect`, `forEach`, `reduce` (produce result)
   ```java
   // Intermediate
   Stream<String> s = names.stream().filter(name -> name.length() > 3);
   // Terminal
   List<String> l = s.collect(Collectors.toList());
   ```
9. **Count frequency of each word in a list of sentences using streams**
   ```java
   Map<String, Long> freq = sentences.stream()
       .flatMap(s -> Arrays.stream(s.split(" ")))
       .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
   ```
10. **Convert a list of objects to a map using streams**
    ```java
    Map<Integer, String> idToName = persons.stream()
        .collect(Collectors.toMap(Person::getId, Person::getName));
    ```
11. **Performance implications of streams vs. loops**
    - Streams: More readable, can be parallelized, but may have overhead for small collections.
    - Loops: Faster for simple tasks, less abstraction.
12. **Find the first non-repeated character in a string using streams**
    ```java
    String input = "swiss";
    Optional<Character> firstNonRepeated = input.chars()
        .mapToObj(c -> (char) c)
        .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
        .entrySet().stream()
        .filter(e -> e.getValue() == 1)
        .map(Map.Entry::getKey)
        .findFirst();
    ```
13. **Merge two lists and remove duplicates using streams**
    ```java
    List<String> merged = Stream.concat(list1.stream(), list2.stream())
        .distinct()
        .collect(Collectors.toList());
    ```
14. **Lazy evaluation in streams**
    ```java
    Stream<String> s = names.stream().filter(name -> {
        System.out.println(name);
        return name.length() > 3;
    });
    // No output until terminal operation
    s.count(); // triggers evaluation
    ```
15. **Use Collectors.partitioningBy and groupingBy**
    ```java
    Map<Boolean, List<String>> partitioned = names.stream()
        .collect(Collectors.partitioningBy(name -> name.length() > 3));
    Map<Integer, List<String>> grouped = names.stream()
        .collect(Collectors.groupingBy(String::length));
    ```

## More on Streams: groupingBy, IntStream, Function::identity

### Collectors.groupingBy
`Collectors.groupingBy` is used to group elements of a stream by a classifier function.
```java
Map<Integer, List<String>> groupedByLength = names.stream()
    .collect(Collectors.groupingBy(String::length));
```
You can also use downstream collectors:
```java
Map<Integer, Set<String>> groupedSet = names.stream()
    .collect(Collectors.groupingBy(String::length, Collectors.toSet()));
```

### IntStream
`IntStream` is a primitive specialization of Stream for `int` values. It provides methods for working with integer streams efficiently.
```java
IntStream.range(1, 5).forEach(System.out::println); // prints 1 2 3 4
int sum = IntStream.of(1, 2, 3, 4).sum();
```
You can also convert collections to IntStream:
```java
int totalLength = names.stream().mapToInt(String::length).sum();
```

### Function::identity
`Function.identity()` returns a function that always returns its input argument. Useful in collectors and mapping operations.
```java
Map<String, String> nameMap = names.stream()
    .collect(Collectors.toMap(Function.identity(), Function.identity()));
```
Or for deduplication:
```java
List<String> uniqueNames = names.stream()
    .collect(Collectors.toMap(Function.identity(), Function.identity(), (a, b) -> a))
    .values().stream().collect(Collectors.toList());
```

### More Examples and Variations
- Grouping by custom property:
  ```java
  Map<Character, List<String>> groupedByFirstChar = names.stream()
      .collect(Collectors.groupingBy(name -> name.charAt(0)));
  ```
- Using IntStream for statistics:
  ```java
  int maxLen = names.stream().mapToInt(String::length).max().orElse(0);
  ```
- Using Function::identity for deduplication in sets:
  ```java
  Set<String> unique = new HashSet<>(names);
  ```

