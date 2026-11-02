package com.automatas5.Views;

import imgui.ImGui;
import imgui.app.Application;
import imgui.app.Configuration;

public class MainView extends Application {
    
   
    private Viewport3D viewport;

    
    @Override
    protected void configure(Configuration config) {
        config.setTitle("Mi Ventana con ImGui en Java");
        config.setWidth(1280);
        config.setHeight(720);
    }

   
    @Override
    protected void init(Configuration config) {
        super.init(config); 

        long windowHandle = getHandle(); 

        viewport = new Viewport3D(windowHandle, 32, 20f);
        viewport.init(); 
    }

    //render loop
    @Override
    public void process() {
       
        ImGui.begin("Hola Mundo ImGui");
            ImGui.text("¡Bienvenido a Dear ImGui en Java!");
            if (ImGui.button("Haz clic aquí")) {
                System.out.println("¡Botón presionado!");
                viewport.increaseSubdivisions(2);
            }
        ImGui.end();

       
        if (viewport != null) {
            viewport.showWindow("Terreno"); 
        }

        /* 
        NOTA Opcional: Si en lugar de usar 'showWindow' prefieres controlar la ventana desde aquí,
        puedes hacerlo de esta forma (descoméntalo si 'viewport.render()' solo contiene el ImGui.image):
        
        if (ImGui.begin("Mi ventana 3D")) {
            if (viewport != null) {
                viewport.render();
            }
            ImGui.end();
        }
        */
    }
}