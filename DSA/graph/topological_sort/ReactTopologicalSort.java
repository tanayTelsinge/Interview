import java.util.*;

public class ReactTopologicalSort {

    public static void main(String[] args) {
            // Simulated package.json parsing result
            Map<String, List<String>> dependencies = new HashMap<>();
            dependencies.put("react", Arrays.asList("scheduler", "loose-envify"));
            dependencies.put("scheduler", Arrays.asList("loose-envify"));
            dependencies.put("loose-envify", Collections.emptyList());

            // Convert dependencies to reversed graph (who is depended on by whom)
            Map<String, List<String>> reversedGraph = new HashMap<>();
            for (String pkg : dependencies.keySet()) {
                reversedGraph.putIfAbsent(pkg, new ArrayList<>());
                for (String dep : dependencies.get(pkg)) {
                    reversedGraph.putIfAbsent(dep, new ArrayList<>());
                    reversedGraph.get(dep).add(pkg);
                }
            }

            List<String> sortedPackages = topologicalSort(reversedGraph);
            System.out.println("Topologically sorted packages: " + sortedPackages);
    }

    public static List<String> topologicalSort(Map<String, List<String>> graph) {
        Map<String, Integer> inDegree = new HashMap<>();
        for (String node : graph.keySet()) {
            inDegree.putIfAbsent(node, 0);
            for (String neighbor : graph.get(node)) {
                inDegree.put(neighbor, inDegree.getOrDefault(neighbor, 0) + 1);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (String node : inDegree.keySet()) {
            if (inDegree.get(node) == 0) {
                queue.add(node);
            }
        }

        List<String> sortedList = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sortedList.add(current);

            for (String neighbor : graph.getOrDefault(current, Collections.emptyList())) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sortedList.size() != graph.size()) {
            throw new IllegalArgumentException("Graph has at least one cycle");
        }

        return sortedList;
    }

}
