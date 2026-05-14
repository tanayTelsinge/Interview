package DSA.heap.hard;

import java.util.PriorityQueue;

// LC 23 - Merge K Sorted Lists
// Pattern: Min-heap seeded with k list heads; always pull smallest, push its next
// Time: O(N log k)  N = total nodes, k = number of lists
class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { this.val = val; }
}

public class MergeKSortedLists {
    public ListNode mergeKLists(ListNode[] lists) {
        PriorityQueue<ListNode> minHeap = new PriorityQueue<>((a, b) -> a.val - b.val);

        for (ListNode node : lists) {
            if (node != null) minHeap.add(node);
        }

        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;

        while (!minHeap.isEmpty()) {
            ListNode node = minHeap.poll();
            curr.next = node;
            curr = curr.next;
            if (node.next != null) minHeap.add(node.next);
        }

        return dummy.next;
    }
}
