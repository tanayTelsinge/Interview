- What is lambda expression in java?
- A functional programming feature to write concise code.
- () -> {}  
- can be used with functional interfaces (interfaces with a single abstract method).
- Example: Runnable r = () -> System.out.println("Hello");

- What is return type of lambda expression?
- The return type is inferred from the context, based on the functional interface it implements.
- For example, if the functional interface method returns an int, the lambda expression will return an int.

- Coding problem
- String str = "Java is great and Java is fun"; //find duplicate words
- Longest substring without repeating characters
- String s = "abcabcbb";

- Got wrong usage of keyset
- Longest substring without repeating characters -  failed to track index of last and first occurrence of character, so failed to update left pointer correctly.