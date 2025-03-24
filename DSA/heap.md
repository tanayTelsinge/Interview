
##Quick Info = https://www.youtube.com/watch?v=0wPlzMU-k00 (alex sambol heap 3 min)

- When to use? Alternative to sorting, instead of nlogn we get nlogk.

![image](https://github.com/user-attachments/assets/f018dae4-dbd4-4d50-8e36-4c364e9cc11f)

### Min-Heap 
```java
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
minHeap.add(3); minHeap.add(1); minHeap.add(5);
System.out.println(minHeap.poll()); // Output: 1 (Smallest)
```


### Max-Heap (Using Comparator in Java)
```java
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
//PriorityQueue<Integer>maxHeap = new PriorityQueue<>((a,b) -> b - a);
maxHeap.add(3); maxHeap.add(1); maxHeap.add(5);
System.out.println(maxHeap.poll()); // Output: 5 (Largest)
```

# Heap-Based FAANG Interview Problems

## 1. Easy: Top K Frequent Elements (LC 347) - Google, Amazon

### Problem Description  
- Given an integer array `nums` and an integer `k`, return the `k` most frequent elements.

### Approach  
- Use a **HashMap** to count occurrences.
- Use a **min-heap** of size `k` to store the most frequent elements.
- Extract the heap contents as the final result.

### Java Code
```java
import java.util.*;

public class TopKFrequent {
    public int[] topKFrequent(int[] nums, int k) {
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }

        PriorityQueue<Integer> minHeap = new PriorityQueue<>(Comparator.comparingInt(freqMap::get));

        for (int num : freqMap.keySet()) {
            minHeap.add(num);
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        int[] result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = minHeap.poll();
        }

        return result;
    }
}
```

### Complexity  
- **Heap operations:** \( O(n \log k) \)
- **Overall:** \( O(n \log k) \)

---

## 2. Medium: K Closest Points to Origin (LC 973) - Google, Facebook

### Problem Description  
- Given an array of points `points[i] = [x, y]`, return the `k` closest points to `(0,0)`.

### Approach  
- Use a **max-heap** of size `k`.
- Store points based on their squared distance from `(0,0)`.
- Extract the heap contents for the result.

### Java Code
```java
import java.util.*;

public class KClosestPoints {
    public int[][] kClosest(int[][] points, int k) {
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
            (a, b) -> (b[0] * b[0] + b[1] * b[1]) - (a[0] * a[0] + a[1] * a[1])
        );

        for (int[] point : points) {
            maxHeap.add(point);
            if (maxHeap.size() > k) {
                maxHeap.poll();
            }
        }

        int[][] result = new int[k][2];
        for (int i = 0; i < k; i++) {
            result[i] = maxHeap.poll();
        }

        return result;
    }
}
```

### Complexity  
- **Heap operations:** \( O(n \log k) \)
- **Overall:** \( O(n \log k) \)

---

## 3. Hard: Merge K Sorted Lists (LC 23) - Google, Amazon

### Problem Description  
- Given `k` sorted linked lists, merge them into one sorted list.

### Approach  
- Use a **min-heap** to store the head nodes.
- Extract the smallest node, add it to the result, and insert its next node.

### Java Code
```java
import java.util.PriorityQueue;

class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { this.val = val; }
}

public class MergeKSortedLists {
    public ListNode mergeKLists(ListNode[] lists) {
        PriorityQueue<ListNode> minHeap = new PriorityQueue<>((a, b) -> a.val - b.val);
        
        for (ListNode node : lists) {
            if (node != null) {
                minHeap.add(node);
            }
        }
        
        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;
        
        while (!minHeap.isEmpty()) {
            ListNode node = minHeap.poll();
            curr.next = node;
            curr = curr.next;
            if (node.next != null) {
                minHeap.add(node.next);
            }
        }
        
        return dummy.next;
    }
}
```

### Complexity  
- **Heap operations:** \( O(N \log k) \)
- **Overall:** \( O(N \log k) \)

---

## 6. Medium: Task Scheduler (LC 621) - Amazon, Google

### Problem Description  
- Given an array of tasks and an integer `n`, return the least number of intervals needed to execute all tasks.

### Approach  
- Use a **max-heap** to store task frequencies.
- Use a queue to maintain cooldown.
- Process tasks in order of frequency while maintaining cooldown.

### Java Code
```java
import java.util.*;

public class TaskScheduler {
    public int leastInterval(char[] tasks, int n) {
        int[] freq = new int[26];
        for (char task : tasks) {
            freq[task - 'A']++;
        }
        
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        for (int f : freq) {
            if (f > 0) maxHeap.add(f);
        }
        
        int cycles = 0;
        while (!maxHeap.isEmpty()) {
            List<Integer> temp = new ArrayList<>();
            for (int i = 0; i <= n; i++) {
                if (!maxHeap.isEmpty()) {
                    temp.add(maxHeap.poll() - 1);
                }
            }
            
            for (int t : temp) {
                if (t > 0) maxHeap.add(t);
            }
            
            cycles += maxHeap.isEmpty() ? temp.size() : n + 1;
        }
        return cycles;
    }
}
```

### Complexity  
- **Heap operations:** \( O(n \log k) \)
- **Overall:** \( O(n) \)

---

## 7. Medium: Hands of Straights (LC 846) - Google, Amazon

### Problem Description  
- Given a hand of cards and an integer `W`, determine if it is possible to rearrange into consecutive groups of `W`.

### Approach  
- Use a **min-heap** to always pick the smallest available card.
- Use a **HashMap** to track card frequencies.

### Java Code
```java
import java.util.*;

public class HandsOfStraights {
    public boolean isNStraightHand(int[] hand, int W) {
        if (hand.length % W != 0) return false;
        
        Map<Integer, Integer> freq = new TreeMap<>();
        for (int card : hand) {
            freq.put(card, freq.getOrDefault(card, 0) + 1);
        }
        
        for (int card : freq.keySet()) {
            while (freq.get(card) > 0) {
                for (int i = 0; i < W; i++) {
                    if (freq.getOrDefault(card + i, 0) == 0) return false;
                    freq.put(card + i, freq.get(card + i) - 1);
                }
            }
        }
        return true;
    }
}
```

### Complexity  
- **Heap operations:** \( O(n \log n) \)
- **Overall:** \( O(n \log n) \)

