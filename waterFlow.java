
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

class Graph {

    HashMap<String, List<Pipe>> pipeMap;

    Graph() {
        pipeMap = new HashMap<>();
    }

    public void addPipe(Pipe pipe) {
        if (!pipeMap.containsKey(pipe.startNode)) {
            List<Pipe> list = new ArrayList<>();
            list.add(pipe);
            pipeMap.put(pipe.startNode, list);
        } else {
            pipeMap.get(pipe.startNode).add(pipe);
        }

    }

    public void printPipes() {
        for (Map.Entry<String, List<Pipe>> pair : pipeMap.entrySet()) {
            List<Pipe> pipes = pair.getValue();

            for (Pipe pipe : pipes) {
                System.out.println(pipe.startNode + " -> " + pipe.endNode);
            }
        }
    }

    private class PipeComparator implements Comparator<Pipe> {

        @Override
        public int compare(Pipe x, Pipe y) {
            // Assume neither Pipe is null.
            return x.endNode.compareTo(y.endNode);
        }
    }

    public void sortPipes() {
        Comparator<Pipe> pipeComparator = new PipeComparator();
        for (Map.Entry<String, List<Pipe>> pair : pipeMap.entrySet()) {
            List<Pipe> pipes = pair.getValue();
            Collections.sort(pipes, pipeComparator);
        }
    }
}

class OffPeriod {

    final int start;
    final int end;

    OffPeriod(int start, int end) {
        this.start = start;
        this.end = end;
    }
}

class Pipe {

    final String startNode;
    final String endNode;
    final int length;
    final List<OffPeriod> offPeriods;

    public Pipe(String startNode, String endNode, int length) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.length = length;
        offPeriods = new ArrayList<>();
    }

    public void addOffPeriod(OffPeriod offPeriod) {
        offPeriods.add(offPeriod);
    }

    public boolean isAvailable(int currentTime) {
        for (OffPeriod offPeriod : offPeriods) {
            if (currentTime % 24 >= offPeriod.start && currentTime % 24 <= offPeriod.end) {
                return false;
            }
        }
        return true;
    }
}

public class waterFlow {

    private class Answer {

        final String endNode;
        final int endTime;

        public Answer(String endNode, int endTime) {
            this.endNode = endNode;
            this.endTime = endTime;
        }
    }

    public static void main(String[] args) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(args[1]))) {
            PrintWriter outputWriter = new PrintWriter("output.txt");
            
            int numOfTests = Integer.valueOf(reader.readLine());
            for (int i = 0; i < numOfTests; i++) {              
                String method = reader.readLine();
                String sourceNode = reader.readLine();

                String[] destinationNodes = reader.readLine().split("\\s+");
                Map<String, Integer> destinationMap = new HashMap<>();
                int destinationCount = 0;
                for (String destination : destinationNodes) {
                    destinationMap.put(destination, destinationCount);
                    destinationCount++;
                }
                reader.readLine(); // middle nodes
                int numOfPipes = Integer.valueOf(reader.readLine());

                Graph graph = new Graph();
                for (int j = 0; j < numOfPipes; j++) {
                    String[] pipeFormat = reader.readLine().split("\\s+");
                    String startNode = pipeFormat[0];
                    String endNode = pipeFormat[1];
                    int length = Integer.valueOf(pipeFormat[2]);
                    int numOfOffPeriods = Integer.valueOf(pipeFormat[3]);
                    Pipe pipe = new Pipe(startNode, endNode, length);
                    for (int k = 0; k < numOfOffPeriods; k++) {
                        String[] period = pipeFormat[4 + k].split("-");
                        pipe.addOffPeriod(new OffPeriod(Integer.valueOf(period[0]), Integer.valueOf(period[1])));
                    }
                    graph.addPipe(pipe);
                }
                graph.sortPipes(); // In order to expand node alphabetically 
//                graph.printPipes();

                int startTime = Integer.valueOf(reader.readLine());
                reader.readLine(); // Blank line

                Answer answer = null;
                waterFlow task = new waterFlow();
                switch (method) {
                    case "BFS":
                        answer = task.BFS(graph, sourceNode, destinationMap, startTime);
                        break;
                    case "UCS":
                        answer = task.UCS(graph, sourceNode, destinationMap, startTime);
                        break;
                    case "DFS":
//                        answer = task.DFS(graph, sourceNode, destinationMap, startTime);
                        answer = task.recursiveDFS(graph, sourceNode, new HashSet<>(), destinationMap, startTime);
                        break;
                    default:
                        System.out.println("WTF");
                }

                // output
                System.out.print(method + " ");
                if (answer != null && !"".equals(answer.endNode)) {
                    outputWriter.println(answer.endNode + " " + String.valueOf(answer.endTime));
                    System.out.println(answer.endNode + " " + String.valueOf(answer.endTime));
                } else {
                    System.out.println("None");
                    outputWriter.println("None");
                }
                outputWriter.flush();
            }

        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    private class TreeNode {

        String name;
        int arrivalTime;

        public TreeNode(String name, int arrivalTime) {
            this.name = name;
            this.arrivalTime = arrivalTime;
        }
    }

    private boolean isFrontiercontains(Collection<TreeNode> frontier, String expandedNode) {
        for (TreeNode node : frontier) {
            if (node.name.equals(expandedNode)) {
                return true;
            }
        }
        return false;
    }

    // NOTE: It's not possible for a node to be both source and destination.
    // NOTE: The pipe length will be 1 for both BFS and DFS 
    // (i.e. pipe length is ignored by these algorithms and is always assumed to be 1). 
    // Also, ignore the off-periods for these algorithms, i.e. when using BFS-DFS; 
    // assume that all pipes work all the time.
    // ????????????????
    // For BFS-DFS, whenever you want to insert nodes to your frontier, 
    // insert them in alphabetical order and then remove them according to the algorithm. 
    // Also for UCS, upon choosing a node from the frontier, in case of ties, 
    // choose the one that comes first in alphabetical order.
    private Answer BFS(Graph graph, String sourceNode, Map<String, Integer> destinationMap, int startTime) {
        Queue<TreeNode> frontierQueue = new LinkedList<>();
        Set<String> exploredSet = new HashSet<>();
        frontierQueue.add(new TreeNode(sourceNode, startTime));

        while (!frontierQueue.isEmpty()) {
            TreeNode node = frontierQueue.remove();
            exploredSet.add(node.name);
            // expand

            List<Pipe> pipes = graph.pipeMap.get(node.name);
            if (pipes != null) {
                for (Pipe pipe : pipes) {
                    String expandedNode = pipe.endNode;
                    if (!isFrontiercontains(frontierQueue, expandedNode) && !exploredSet.contains(expandedNode)) {
                        if (destinationMap.containsKey(expandedNode)) { // reach the goal
                            return new Answer(expandedNode, (node.arrivalTime + 1) % 24);
                        } else {
                            frontierQueue.add(new TreeNode(expandedNode, node.arrivalTime + 1));
                        }
                    }
                }
            }
        }
        return new Answer("", startTime);
    }

    private class TreeNodeComparator implements Comparator<TreeNode> {

        @Override
        public int compare(TreeNode x, TreeNode y) {
            // Assume neither TreeNode is null.
            return x.arrivalTime - y.arrivalTime;
        }
    }

    private Answer UCS(Graph graph, String sourceNode, Map<String, Integer> destinationMap, int startTime) {
        Comparator<TreeNode> treeNodeComparator = new TreeNodeComparator();
        Queue<TreeNode> frontierQueue = new PriorityQueue<>(treeNodeComparator);
        Set<String> exploredSet = new HashSet<>();
        frontierQueue.add(new TreeNode(sourceNode, startTime));

        while (!frontierQueue.isEmpty()) {
            TreeNode node = frontierQueue.remove(); // This would remove the least cost TreeNode.
            exploredSet.add(node.name);
            if (destinationMap.containsKey(node.name)) { // reach the goal
                return new Answer(node.name, node.arrivalTime % 24);
            }
            
            // expand
            List<Pipe> pipes = graph.pipeMap.get(node.name);
            if (pipes != null) {
                for (Pipe pipe : pipes) {
                    if (pipe.isAvailable(node.arrivalTime)) { // be careful time > 24
                        String expandedNode = pipe.endNode;
                        if (!exploredSet.contains(expandedNode)) {
                            // Because this is a PriorityQueue, the higher cost TreeNode would be put behind.
                            frontierQueue.add(new TreeNode(expandedNode, node.arrivalTime + pipe.length));
                        }
                    }
                }
            }
        }
        return new Answer("", startTime);
    }

    private Answer DFS(Graph graph, String sourceNode, Map<String, Integer> destinationMap, int startTime) {
        Deque<TreeNode> frontierStack = new ArrayDeque<>();
        Set<String> exploredSet = new HashSet<>();
        frontierStack.push(new TreeNode(sourceNode, startTime));

        while (!frontierStack.isEmpty()) {
            TreeNode node = frontierStack.pop();
            exploredSet.add(node.name);
            if (destinationMap.containsKey(node.name)) { // check when explored
                return new Answer(node.name, node.arrivalTime % 24); // reach the goal
            }

            // expand
            List<Pipe> pipes = graph.pipeMap.get(node.name);
            if (pipes != null) {
                for (int i = pipes.size() - 1; i >= 0; i--) { // reverse-alphabetical order
                    Pipe pipe = pipes.get(i);
                    String expandedNode = pipe.endNode;
                    if (!exploredSet.contains(expandedNode)) {
                        frontierStack.push(new TreeNode(expandedNode, node.arrivalTime + 1));
                    }
                }
            }
        }
        return new Answer("", startTime);
    }

    // Backtracking
    private Answer recursiveDFS(Graph graph, String sourceNode, Set<String> exploredSet, Map<String, Integer> destinationMap, int startTime) {
        if (destinationMap.containsKey(sourceNode)) { // reach the goal
            return new Answer(sourceNode, startTime % 24);
        }

        Answer answer = new Answer("", 0);
        List<Pipe> pipes = graph.pipeMap.get(sourceNode);
        if (pipes != null) {
            for (Pipe pipe : pipes) {
                String expandedNode = pipe.endNode;
                if (!exploredSet.contains(expandedNode)) {
                    exploredSet.add(expandedNode);
                    answer = recursiveDFS(graph, pipe.endNode, exploredSet, destinationMap, startTime + 1);
                    if (!"".equals(answer.endNode)) {
                        break;
                    }
                }
            }
        }
        return answer;
    }
}
