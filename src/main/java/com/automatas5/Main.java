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

        // 2) Pintar/borrar: se llama en cada frame mientras se mantiene el clic
        view.setOnPaint((radio, fuerza, herramienta) -> {
            System.out.println("Pintando r=" + radio + " f=" + fuerza + " h=" + herramienta);
            // luego: tools.ModificateTerrain(radio, fuerza);
        });

        // 3) Viewer 3D: recibe el ancho y alto disponibles
        view.setViewerRenderer((w, h) -> {
            // luego: viewer.render(w, h);
            imgui.ImGui.button("Aqui va el viewer " + w.intValue() + "x" + h.intValue(), w, h);
        });

        // 4) Contenido extra del dashboard (matriz dispersa)
        view.setDashboardExtra(() -> {
            imgui.ImGui.text("Matriz dispersa: (pendiente)");
            // luego: recorrer la matriz y mostrar los caracteres
        });

        view.launch(); // siempre al final: bloquea hasta cerrar la ventana
        view.launch();
    }
}
