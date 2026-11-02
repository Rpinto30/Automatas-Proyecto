package com.automatas5.Views;

import com.automatas5.Views.Dimension.Viewport3D;
import com.automatas5.structs.Matrix;

import imgui.ImGui;
import imgui.app.Application;
import imgui.app.Configuration;

public class MainView extends Application {
    
    private Viewport3D viewport;
    private final imgui.type.ImInt heightInput = new imgui.type.ImInt(4);
    
    //Structs
    private int cells;
    private Matrix matrix; 

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

        this.cells = 16;
        matrix = new Matrix(this.cells);

        viewport = new Viewport3D(windowHandle, this.cells, 20f);
        viewport.init(); 
    }

    //render loop
    @Override
    public void process() {
        ImGui.begin("Herramientas");

        int[] v = viewport.getSelectedVertex();
        if (v != null) {
            ImGui.text("Vértice: columna " + v[0] + ", fila " + v[1]);
            ImGui.text("Altura actual: " + viewport.getVertexHeight(v[0], v[1]));
        } else {
            ImGui.text("Ningún vértice seleccionado (clic izquierdo en el terreno)");
        }

        ImGui.inputInt("Altura", heightInput);

        if (v != null) {
            if (ImGui.button("Fijar altura")) {
                viewport.setVertexHeight(v[0], v[1], heightInput.get());
            }
            ImGui.sameLine();
            if (ImGui.button("Subir")) {
                viewport.addVertexHeight(v[0], v[1], heightInput.get());
            }
        }
        ImGui.end();

        viewport.showWindow("Terreno");
    }

    @Override
    protected void dispose() {
        if (viewport != null) viewport.dispose();
        super.dispose();
    }
}