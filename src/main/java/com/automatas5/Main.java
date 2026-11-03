package com.automatas5;
import com.automatas5.structs.Graph; // TODO - debe importarse el nombre con el que la clase Graph chambee
import  com.automatas5.Views.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Iniciando aplicación...");
        Graph graph = new Graph(); // TODO - crear un grafo vacío o con datos de prueba
        MainView view = new MainView(graph);
        
        // 1) Resolucion: se llama al dar Enter en el campo
        view.setOnResolutionChange(res -> {
            System.out.println("Nueva resolucion: " + res + "x" + res);
            // luego: matrix = new Matrix(res); viewer.setResolution(res);
        });

       

        view.launch(); // siempre al final: bloquea hasta cerrar la ventana
    }
}
