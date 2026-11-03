package com.automatas5.structs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import com.automatas5.process.Automate;

public class graph {

    // Adjacency list and vertex list
    public Map<String, List<String>> vertexMap = new HashMap<>();
    public List<Character> vertexList = new ArrayList<>();

    // Valid automaton alphabet symbols
    private static final String VALID_ALPHABET = "#@%*; :!-,. ";

    // Returns node if found, otherwise null
    public Character SearchNode(char name) {
        Character v = vertexList.stream()
                .filter(u -> u.equals(name))
                .findFirst()
                .orElse(null);
        return v;
    }

    // Checks if symbol is unique and belongs to the alphabet
    public boolean validate(char value) {
        boolean isUnique = (SearchNode(value) == null);
        boolean isInAlphabet = (VALID_ALPHABET.indexOf(value) != -1);
        return isUnique && isInAlphabet;
    }

    // Inserts a new unique node into the graph
    public void InsertNode(char value) {
        if (!validate(value)) {
            return;
        }
        vertexList.add(value);
        vertexMap.put(String.valueOf(value), new ArrayList<>());
    }

    // Creates a directed edge between two existing nodes
    public void ConnectNodes(char origin, char dest) {
        if (SearchNode(origin) != null && SearchNode(dest) != null) {
            String originKey = String.valueOf(origin);
            String destVal = String.valueOf(dest);

            List<String> neighbors = vertexMap.get(originKey);
            if (neighbors != null && !neighbors.contains(destVal)) {
                neighbors.add(destVal);
            }
        }
    }

    // Computes shortest path using edge weight: height(A) + height(B)
    public List<Character> GetDijsktra(Character origin, Character dest) {
        if (origin == null || dest == null || SearchNode(origin) == null || SearchNode(dest) == null) {
            return Collections.emptyList();
        }

        Map<Character, Integer> distances = new HashMap<>();
        Map<Character, Character> predecessors = new HashMap<>();
        Set<Character> visited = new HashSet<>();

        for (Character node : vertexList) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(origin, 0);

        PriorityQueue<Character> pq = new PriorityQueue<>(
                Comparator.comparingInt(n -> distances.getOrDefault(n, Integer.MAX_VALUE))
        );
        pq.add(origin);

        while (!pq.isEmpty()) {
            Character current = pq.poll();

            if (!visited.add(current)) {
                continue;
            }

            if (current.equals(dest)) {
                break;
            }

            List<String> neighbors = vertexMap.getOrDefault(String.valueOf(current), Collections.emptyList());
            for (String neighborStr : neighbors) {
                if (neighborStr.isEmpty()) continue;
                char neighbor = neighborStr.charAt(0);

                if (visited.contains(neighbor)) {
                    continue;
                }

                int edgeWeight = Automate.GetHeightByChar(current) + Automate.GetHeightByChar(neighbor);
                int newDist = distances.get(current) + edgeWeight;

                if (newDist < distances.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    distances.put(neighbor, newDist);
                    predecessors.put(neighbor, current);
                    pq.remove(neighbor);
                    pq.add(neighbor);
                }
            }
        }

        // Reconstruct shortest path from origin to dest
        List<Character> path = new ArrayList<>();
        if (!origin.equals(dest) && !predecessors.containsKey(dest)) {
            return path;
        }

        for (Character step = dest; step != null; step = predecessors.get(step)) {
            path.add(step);
            if (step.equals(origin)) break;
        }
        Collections.reverse(path);
        return path;
    }

    // Overload for standard spelling and primitive char parameters
    public List<Character> GetDijkstra(char origin, char dest) {
        return GetDijsktra(origin, dest);
    }

    // Returns list of node objects
    public List<Character> GetNodes() {
        return vertexList;
    }

    // Returns list of node names as strings
    public List<String> GetNameNodes() {
        return vertexList.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }
}