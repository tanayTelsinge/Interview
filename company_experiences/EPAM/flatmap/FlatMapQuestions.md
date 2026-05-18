# flatMap Coding Questions

## 1. Flatten a List of Lists
```
Input:  [[1, 2, 3], [4, 5], [6, 7, 8, 9]]
Output: [1, 2, 3, 4, 5, 6, 7, 8, 9]
```

## 2. Get All Unique Words from a List of Sentences
```
Input:  ["hello world", "world of java", "java is great"]
Output: [hello, world, of, java, is, great]  // distinct
```

## 3. Get All Phone Numbers of All Employees
```
Each Employee has List<String> phoneNumbers
Return one flat list of all phone numbers across all employees
```

## 4. Find All Characters from a List of Strings (distinct, sorted)
```
Input:  ["abc", "bcd", "cde"]
Output: [a, b, c, d, e]
```

## 5. Flatten a Map's Values into a Single List
```
Map<String, List<Integer>> scores = {
    "Alice": [85, 90],
    "Bob":   [78, 88, 92]
};
Output: [85, 90, 78, 88, 92]
```

## 6. Get All Items from All Orders from All Customers
```
Customer has List<Order>, Order has List<Item>
Get all items across all customers and all orders
```

## 7. Expand Each Number to a Range
```
Input:  [1, 3, 5]
Output: [1, 2, 2, 3, 4, 5, 6]  // each n -> stream of [n, n+1]
```

## 8. Find All Pairs - Cartesian Product
```
Input:  [1, 2], [3, 4]
Output: [[1,3], [1,4], [2,3], [2,4]]
```

## 9. Count Total Words Across All Sentences
```
Input:  ["I love Java", "FlatMap is powerful"]
Output: 5
```

## 10. Get All Unique Team Names from a Nested Org Structure
```
Company -> List<Department> -> List<Team>
Get all unique team names across the entire organization
```
