package com.automatas5.Views;

import imgui.ImGui;
import imgui.app.Application;
import imgui.app.Configuration;

public class MainView extends Application {

    // Configura el título y dimensiones de la ventana principal
    @Override
    protected void configure(Configuration config) {
        config.setTitle("Mi Ventana con ImGui en Java");
        config.setWidth(800);
        config.setHeight(600);
    }

    // Este método se ejecuta en cada fotograma (render loop)
    @Override
    public void process() {
        // 1. Iniciar una nueva ventana de ImGui
        ImGui.begin("Hola Mundo ImGui");

        // 2. Agregar elementos dentro de la ventana
        ImGui.text("¡Bienvenido a Dear ImGui en Java!");
        
        if (ImGui.button("Haz clic aquí")) {
            System.out.println("¡Botón presionado!");
        }

        // 3. Cerrar la ventana de ImGui
        ImGui.end();
    }

}