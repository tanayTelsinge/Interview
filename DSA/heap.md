
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
maxHeap.add(3); maxHeap.add(1); maxHeap.add(5);
System.out.println(maxHeap.poll()); // Output: 5 (Largest)
```


