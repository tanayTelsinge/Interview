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


