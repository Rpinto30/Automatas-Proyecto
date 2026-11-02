package com.automatas5.process;

import com.automatas5.Views.Viewport3D;
import com.automatas5.structs.Graph;
import com.automatas5.structs.Matrix;
import java.util.List;

public class Tools {

    private final Graph grafo;
    private final Matrix matrix;
    private Viewport3D viewport;

    // Constructor
    public Tools(Graph grafo, Matrix matrix) {
        this.grafo = grafo;
        this.matrix = matrix;
    }

    // Constructor sobrecargado opcional 
    public Tools(Graph grafo, Matrix matrix, Viewport3D viewport) {
        this.grafo = grafo;
        this.matrix = matrix;
        this.viewport = viewport;
    }

    public void setViewport(Viewport3D viewport) {
        this.viewport = viewport;
    }

    /**
     * Modifica el terreno aplicando el radio, la fuerza y el camino de Dijkstra.
     *
     * @param radius Radio del pincel (1 a 10)
     * @param force  Fuerza del pincel: Borrador [-1.0, 0.0], Lápiz [0.0, 1.0]
     */
    public void ModificateTerrain(int radius, double force) {
        if (viewport == null || grafo == null || matrix == null) {
            return;
        }

        // Obtener el símbolo de destino con el método real de Automate.java
        char dest = Automate.GetCharByForce(force);
        if (dest == '\0') {
            return;
        }

        // Obtener el camino de símbolos desde # hasta dest con el Dijkstra
        List<Character> caminoDijkstra = grafo.GetDijsktra('#', dest);
        if (caminoDijkstra == null || caminoDijkstra.isEmpty()) {
            return;
        }

        // Obtener las coordenadas (xi, yi) del vértice seleccionado en el Viewport3D
        int[] selectedVertex = viewport.getSelectedVertex();
        if (selectedVertex == null || selectedVertex.length < 2) {
            return;
        }
        int xi = selectedVertex[0];
        int yi = selectedVertex[1];

        int ultimoIndice = caminoDijkstra.size() - 1;

        // Recorrido de puntos según el radio y aplicación del suavizado

        // A. CENTRO (xi, yi) : aplica el último carácter de la lista de Dijkstra
        char charCentro = caminoDijkstra.get(ultimoIndice);
        aplicarCambioVertice(xi, yi, charCentro);

        // B. EJES CARDINALES (0, pi/2, pi, 3pi/2): recorre de 1 hasta r
        int[][] direccionesEjes = {
            { 1,  0}, // Derecha (0)
            { 0,  1}, // Arriba (pi/2)
            {-1,  0}, // Izquierda (pi)
            { 0, -1}  // Abajo (3pi/2)
        };

        for (int d = 1; d <= radius; d++) {
            char caracterActual = obtenerCaracterSuavizado(caminoDijkstra, d);
            for (int[] dir : direccionesEjes) {
                int nx = xi + (dir[0] * d);
                int ny = yi + (dir[1] * d);
                aplicarCambioVertice(nx, ny, caracterActual);
            }
        }

        // C. ÁNGULOS MEDIOS (pi/4, 3pi/4, 5pi/4, 7pi/4): recorre de 1 hasta r - 1
        int[][] direccionesMedios = {
            { 1,  1}, // pi/4
            {-1,  1}, // 3pi/4
            {-1, -1}, // 5pi/4
            { 1, -1}  // 7pi/4
        };

        for (int d = 1; d <= radius - 1; d++) {
            char caracterActual = obtenerCaracterSuavizado(caminoDijkstra, d);
            for (int[] dir : direccionesMedios) {
                int nx = xi + (dir[0] * d);
                int ny = yi + (dir[1] * d);
                aplicarCambioVertice(nx, ny, caracterActual);
            }
        }
    }

    /**
     * Obtiene el carácter de la lista de Dijkstra retrocediendo según la distancia al centro.
     */
    private char obtenerCaracterSuavizado(List<Character> camino, int distancia) {
        int ultimoIndice = camino.size() - 1;
        int indice = Math.max(0, ultimoIndice - distancia);
        return camino.get(indice);
    }

    /**
     * Obtiene la altura con Automate.GetHeightByChar y actualiza el Viewport3D y la Matrix.
     */
    private void aplicarCambioVertice(int x, int y, char simbolo) {
        // Obtener altura entera desde Automate.java
        int altura = Automate.GetHeightByChar(simbolo);

        // Modificar el vértice en el terreno 3D 
        viewport.addVertexHeight(x, y, altura);

        // Validar bordes de Matrix 
        int size = matrix.getSize();
        if (x >= 0 && x < size && y >= 0 && y < size) {
            matrix.insertInCoord(x, y, simbolo);
        }
    }
}