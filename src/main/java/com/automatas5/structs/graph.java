package com.automatas5.structs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** STUB temporal para pruebas de UI. Borrar cuando llegue el Graph real de Diego. */
public class Graph {

    public Map<String, List<String>> vertexMap = new HashMap<>();
    public List<Character> vertexList = new ArrayList<>();

    public void InsertNode(char value) {
        if (vertexList.contains(value)) {
            System.out.println("[Graph stub] simbolo repetido: " + value);
            return;
        }
        vertexList.add(value);
        vertexMap.put(String.valueOf(value), new ArrayList<>());
        System.out.println("[Graph stub] InsertNode " + value);
    }

    public void ConnectNodes(char origin, char dest) {
        if (!vertexList.contains(origin) || !vertexList.contains(dest)) {
            System.out.println("[Graph stub] nodo inexistente: " + origin + " -> " + dest);
            return;
        }
        vertexMap.get(String.valueOf(origin)).add(String.valueOf(dest));
        System.out.println("[Graph stub] ConnectNodes " + origin + " -> " + dest);
    }

    public void DisconnectNodes(char origin, char dest) {
        if (!vertexList.contains(origin) || !vertexList.contains(dest)) {
            System.out.println("[Graph stub] nodo inexistente: " + origin + " -> " + dest);
            return;
        }
        boolean removed = vertexMap.get(String.valueOf(origin)).remove(String.valueOf(dest));
        System.out.println("[Graph stub] DisconnectNodes " + origin + " -> " + dest
                + (removed ? "" : " (no estaban conectados)"));
    }

    public void ChangeNodeValue(char oldValue, char newValue) {
        if (!vertexList.contains(oldValue)) {
            System.out.println("[Graph stub] nodo inexistente: " + oldValue);
            return;
        }
        if (vertexList.contains(newValue)) {
            System.out.println("[Graph stub] simbolo repetido: " + newValue);
            return;
        }
        vertexList.remove((Character) oldValue);
        vertexList.add(newValue);
        List<String> connections = vertexMap.remove(String.valueOf(oldValue));
        vertexMap.put(String.valueOf(newValue), connections);
        System.out.println("[Graph stub] ChangeNodeValue " + oldValue + " -> " + newValue);
    }
}