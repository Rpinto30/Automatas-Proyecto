package com.automatas5.process;

import java.util.ArrayList;
import java.util.Collections;
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

    /** Ultima ruta Dijkstra elegida (estado inicial -> ':'), para mostrarla en la UI. Vacia si no hubo. */
    private List<Character> lastPath = Collections.emptyList();

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

    /** Ruta mas rapida usada en la ultima pincelada (del estado inicial al estado base ':'). */
    public List<Character> getLastPath() {
        return lastPath;
    }

    public void clearLastPath() {
        lastPath = Collections.emptyList();
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
     * 3. El camino (n estados) se REPARTE sobre los R anillos del radio: el anillo k (distancia euclidiana
     *    redondeada al centro, 0..R) usa el estado round(k * (n-1) / R). Asi el centro es siempre el estado
     *    inicial y el borde del radio siempre el estado base, para cualquier R:
     *      - R mayor que n-1: cada estado se mantiene varios anillos (terrazas).
     *      - R igual a n-1: un estado por anillo.
     *      - R menor que n-1: se saltan estados (pendiente mas empinada).
     *
     * @param radius Radio del pincel, en celdas
     * @param force  Fuerza: Borrador [-1.0, 0.0], Lapiz [0.0, 1.0]
     * @param raise  true = pincel (sube), false = borrador (baja); solo importa con BLEND
     * @return true si se modifico el terreno; si es false, getLastMessage() explica por que.
     */
    public boolean ModificateTerrain(int radius, double force, boolean raise) {
        lastMessage = "";
        lastPath = Collections.emptyList();
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

        lastPath = Collections.unmodifiableList(new ArrayList<>(camino));

        // Reparto de los n estados sobre los anillos 0..R
        final int n = camino.size();
        final int R = Math.max(0, radius);
        final int[] estadoDelAnillo = new int[R + 1];
        for (int k = 0; k <= R; k++) {
            estadoDelAnillo[k] = (R == 0) ? 0 : (int) Math.round((double) k * (n - 1) / R);
        }

        // Recorrido radial: anillo = distancia euclidiana redondeada
        // (solo se recorre la parte del cuadrado que cae dentro de la malla)
        final int size = matrix.getSize();
        for (int y = Math.max(0, yi - R); y <= Math.min(size - 1, yi + R); y++) {
            for (int x = Math.max(0, xi - R); x <= Math.min(size - 1, xi + R); x++) {
                int anillo = (int) Math.round(Math.hypot(x - xi, y - yi));
                if (anillo > R) continue; // fuera del circulo
                aplicarCambioVertice(x, y, camino.get(estadoDelAnillo[anillo]), raise);
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