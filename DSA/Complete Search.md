![image](https://github.com/user-attachments/assets/70b31475-68bf-4c01-a9de-0f2f3a48d9e5)### Complete Search

- idea is to generate all possible solutions to the problem
using brute force, and then select the best solution or count the number of
solutions, depending on the problem.
-  good technique if there is enough time to go through
all the solutions, because the search is usually easy to implement and it always
gives the correct answer.
-  If complete search is too slow, other techniques, such as
greedy algorithms or dynamic programming, may be needed.

1. Generating subset
   ![image](https://github.com/user-attachments/assets/240ebf3f-9d10-487b-b164-d37599e18b2d)

```java
void search(int k) {
  if (k == n) {
  // process subset
  } else {
    search(k+1);
    subset.push_back(k);
    search(k+1);
    subset.pop_back();
  }
}
```
- Function search is called with parameter k.
- exclude / include logic.
- if k == n, means all elements are processed and subset is generated.

![image](https://github.com/user-attachments/assets/cd39a4f5-283f-44be-aa20-833de2ea5def)

