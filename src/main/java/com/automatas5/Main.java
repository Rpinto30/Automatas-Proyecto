package com.automatas5;
import com.automatas5.Views.MainView;
import com.automatas5.structs.Graph;
import  com.automatas5.structs.Matrix;

public class Main {
    public static void main(String[] args) {
        System.out.println("Iniciando aplicación...");
        Matrix matrix = new Matrix(25);
        Graph graph = new Graph();
        MainView view = new MainView(graph, matrix);
        
        // 1) Resolucion: se llama al dar Enter en el campo
        view.setOnResolutionChange(res -> {
            System.out.println("Nueva resolucion: " + res + "x" + res);
            // luego: matrix = new Matrix(res); viewer.setResolution(res);
        });

       

        view.launch(); // siempre al final: bloquea hasta cerrar la ventana
    }
}
