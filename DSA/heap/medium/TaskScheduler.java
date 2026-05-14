package DSA.heap.medium;

import java.util.*;

// LC 621 - Task Scheduler
// Pattern: Max-heap for greedy frequency-based scheduling with cooldown
// Time: O(n)  Space: O(1) — at most 26 task types
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
