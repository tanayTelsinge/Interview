# What is Topological Sort?

Topological Sort is a linear ordering of vertices in a Directed Acyclic Graph (DAG) such that for every directed edge (u, v), vertex u comes before vertex v in the ordering.


# Why?

- came in 1950s.
- needed ordering of tasks , ordering of numbers present already with sorts.
- widely adopted by compilers to schedule tasks, resolve dependencies, and optimize code execution.
### Its used in package managers 
- Each package and version is a node.
- Dependencies are edges.
- The tool builds a dependency graph, then runs a topological sort to decide install order.
- If it finds a cycle (e.g., A depends on B, B depends on A) → installation fails.

### In Spreadsheet 
- cells depend on other cells, eg. A1 = B1 + C1, so B1 and C1 should be evaluated before A1.
- Topological sort helps in determining the order of evaluation.
- In relational DBs
###  Execution plans are DAGs, 
- in case of queries with multiple joins and subqueries. 

- Topological sort helps in determining the order of operations to optimize query execution.
- In Task Scheduling (Airflow), we define task1 -> task2 -> task3.
Airflow uses topological sort to determine the order of task execution based on dependencies.

- Summarizing

Build systems → compile files in order.

Package managers → install dependencies in order.
```
scheduler → react
loose-envify → react
loose-envify → scheduler

Sorted install order: loose-envify → scheduler → react
```
---

Spreadsheets → compute cells in order.

---
Databases → execute queries in order.

```
Scan table A → Filter → Join with B → Aggregate
```
---
Workflows → run tasks in order.

```
Task A → Task C
Task B → Task C
Run Task A and Task B in parallel, then Task C
```

### Example

```

scheduler → react
loose-envify → react
loose-envify → scheduler

React depends on scheduler and loose-envify.
Scheduler depends on loose-envify.
loose-envify has no dependencies.

Step 1: In-degrees
Node          In-Degree
loose-envify   0
scheduler      1
react          2

Step 2: Start with nodes with in-degree 0  
Queue: [loose-envify]
Topological Order: []
Step 3: Process the queue
While Queue not empty:
  Dequeue loose-envify
  Add to Topological Order: [loose-envify]
  Decrease in-degree of neighbors:
    scheduler in-degree becomes 0
    react in-degree becomes 1
  Enqueue scheduler
    Queue: [scheduler]
While Queue not empty:
    Dequeue scheduler
    Add to Topological Order: [loose-envify, scheduler]
    Decrease in-degree of neighbors:
        react in-degree becomes 0
    Enqueue react
        Queue: [react]
While Queue not empty:

    Dequeue react
    Add to Topological Order: [loose-envify, scheduler, react]
    No neighbors to process
    Queue: []
Final Topological Order: [loose-envify, scheduler, react]

```

```java

Topological Sort is Easy -- The General Template
*
What we need ?
1. HashMap<Node, Indegree> inDegree: A in-degree map, which record each nodes in-degree.
2. HashMap<Node, List<Node>children> topoMap: A topo-map which record the Node's children

What we do ?
1. Init: Init the two HashMaps.
2. Build Map: Put the child into parent's list ; Increase child's in-degree by 1.
3. Find Node with 0 in-degree: Iterate the inDegree map, find the Node has 0 inDegree. (If none, there must be a circle)
4. Decrease the children's inDegree by 1: Decrease step3's children's inDegree by 1.
5. Remove this Node: Remove step3's Node from inDegree. Break current iteration.
6. Do 3-5 until inDegree is Empty: Use a while loop
```


```java
// example - Course Schedule II
class Solution {
    public int[] findOrder(int numCourses, int[][] prerequisites) {
        // Topological sort
        // Edge case
        if(numCourses <= 0) return new int[0];
        
        //1. Init Map
        HashMap<Integer, Integer> inDegree = new HashMap<>();
        HashMap<Integer, List<Integer>> topoMap = new HashMap<>();
        for(int i = 0; i < numCourses; i++) {
            inDegree.put(i, 0);
            topoMap.put(i, new ArrayList<Integer>());
        }
        
        //2. Build Map
        for(int[] pair : prerequisites) {
            int curCourse = pair[0], preCourse = pair[1];
            topoMap.get(preCourse).add(curCourse);  // put the child into it's parent's list
            inDegree.put(curCourse, inDegree.get(curCourse) + 1); // increase child inDegree by 1
        }
        //3. find course with 0 indegree, minus one to its children's indegree, until all indegree is 0
        int[] res = new int[numCourses];
        int base = 0;
        while(!inDegree.isEmpty()) {
            boolean flag = false;   // use to check whether there is cycle
            for(int key : inDegree.keySet()) {  // find nodes with 0 indegree
                if(inDegree.get(key) == 0) {
                    res[base ++] = key;
                    List<Integer> children = topoMap.get(key);  // get the node's children, and minus their inDegree
                    for(int child : children) 
                        inDegree.put(child, inDegree.get(child) - 1);
                    inDegree.remove(key);      // remove the current node with 0 degree and start over
                    flag = true;
                    break;
                }
            }
            if(!flag)  // there is a circle --> All Indegree are not 0
                return new int[0];
        }
        return res;
    }
}

```