package com.automatas5.process;

import java.util.List;

import com.automatas5.Views.Dimension.Viewport3D;
import com.automatas5.structs.Graph;
import com.automatas5.structs.Matrix;

public class Tools {

    /** Estado en el que termina el recorrido: la altura 0 por defecto (celda null de la matriz). */
    private static final char GROUND = ':';

    /**
     * true  -> el pincel solo SUBE y el borrador solo BAJA (nunca deshacen lo que ya hay).
     *          Asi, al arrastrar, los circulos que se solapan forman una cresta suave en vez de ir
     *          rebajando la cima anterior.
     * false -> cada celda toma siempre el simbolo del recorrido, sobrescribiendo lo que tuviera.
     */
    private static final boolean BLEND = true;

    private final Graph grafo;
    private final Matrix matrix;
    private Viewport3D viewport;

    /** Ultimo mensaje de error/aviso de ModificateTerrain ("" si todo salio bien). La UI lo muestra. */
    private String lastMessage = "";

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

    public String getLastMessage() {
        return lastMessage;
    }

    /** Version sin herramienta: fuerza >= 0 se trata como pincel, fuerza < 0 como borrador. */
    public boolean ModificateTerrain(int radius, double force) {
        return ModificateTerrain(radius, force, force >= 0.0);
    }

    /**
     * Pincel radial con el automata.
     *
     * 1. Estado inicial = simbolo elegido por la fuerza (Automate.GetCharByForce). Siempre es el del centro.
     * 2. Se calcula con Dijkstra el camino de ese simbolo hasta el estado base ':' (altura 0).
     * 3. Cada celda a distancia euclidiana d del centro (redondeada al anillo 0, 1, 2...) recibe el estado
     *    numero d del camino. El centro es el estado 0 (el inicial); cada anillo avanza un estado.
     * 4. El recorrido termina donde se acaba el radio o el camino: el estado final depende de cuantos estados
     *    alcanza a recorrer. Si el radio es menor que el camino queda un escalon; si es mayor, las celdas
     *    mas alla del ultimo estado no se tocan.
     *
     * @param radius Radio del pincel (1 a 10), en celdas
     * @param force  Fuerza: Borrador [-1.0, 0.0], Lapiz [0.0, 1.0]
     * @param raise  true = pincel (sube), false = borrador (baja); solo importa con BLEND
     * @return true si se modifico el terreno; si es false, getLastMessage() explica por que.
     */
    public boolean ModificateTerrain(int radius, double force, boolean raise) {
        lastMessage = "";
        if (viewport == null || grafo == null || matrix == null) {
            lastMessage = "El viewer 3D aun no esta listo.";
            return false;
        }

        char inicio = Automate.GetCharByForce(force);
        if (inicio == '\0') {
            lastMessage = "Fuerza fuera de rango.";
            return false;
        }
        // el borrador nunca puede empezar por encima del suelo (fuerza 0 daria '!')
        if (!raise && Automate.GetHeightByChar(inicio) > 0) inicio = GROUND;

        int[] selectedVertex = viewport.getSelectedVertex();
        if (selectedVertex == null || selectedVertex.length < 2) {
            lastMessage = "No hay un vertice seleccionado.";
            return false;
        }
        int xi = selectedVertex[0];
        int yi = selectedVertex[1];

        if (grafo.SearchNode(inicio) == null) {
            lastMessage = "El automata no tiene el estado inicial '" + inicio + "'.";
            return false;
        }
        if (grafo.SearchNode(GROUND) == null) {
            lastMessage = "El automata no tiene el estado base '" + GROUND + "'.";
            return false;
        }

        List<Character> camino = grafo.GetDijsktra(inicio, GROUND);
        if (camino == null || camino.isEmpty()) {
            lastMessage = "No hay ruta en el automata de '" + inicio + "' a '" + GROUND + "'.";
            return false;
        }

        // Recorrido radial: anillo = distancia euclidiana redondeada
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int anillo = (int) Math.round(Math.hypot(dx, dy));
                if (anillo > radius) continue;          // fuera del circulo
                if (anillo >= camino.size()) continue;  // el automata no llega tan lejos
                aplicarCambioVertice(xi + dx, yi + dy, camino.get(anillo), raise);
            }
        }
        return true;
    }

    /**
     * Fija el vertice a la altura ABSOLUTA del simbolo y guarda el simbolo en la matriz.
     * ':' (altura 0) se guarda como celda vacia (null) para que la matriz siga siendo dispersa.
     */
    private void aplicarCambioVertice(int x, int y, char simbolo, boolean raise) {
        int altura = Automate.GetHeightByChar(simbolo);

        if (BLEND) {
            int actual = viewport.getVertexHeight(x, y);
            if (raise ? altura <= actual : altura >= actual) return; // no deshacer lo que ya hay
        }

        // Fuera de la malla: setVertexHeight devuelve false y no tocamos la matriz
        if (!viewport.setVertexHeight(x, y, altura)) return;

        if (x >= 0 && x < matrix.getSize() && y >= 0 && y < matrix.getSize()) {
            if (simbolo == ':') matrix.removeInCoord(x, y);
            else matrix.insertInCoord(x, y, simbolo);
        }
    }
}