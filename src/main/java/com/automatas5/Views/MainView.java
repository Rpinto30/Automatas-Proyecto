package com.automatas5.Views;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.automatas5.Views.Dimension.Viewport3D;
import com.automatas5.process.Automate;
import com.automatas5.process.Persistent;
import com.automatas5.process.Tools;
import com.automatas5.structs.Graph;
import com.automatas5.structs.Matrix;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;

/**
 * Ventana principal 1280x720.
 * Izquierda (60%): editor 3D con resolucion, radio, fuerza y herramientas.
 * Derecha (40%): editor del AFD (GraphView) y resumen del automata.
 */
public class MainView extends Application {

    public enum Tool { MOUSE, BRUSH, ERASER }

    /** Gancho de pintado: radio, fuerza efectiva (ya limitada por herramienta) y herramienta activa. */
    @FunctionalInterface
    public interface PaintListener {
        void onPaint(int radius, double force, Tool tool);
    }

    /** Tamano inicial de la ventana; despues se puede redimensionar libremente. */
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    /** Tamano minimo para que los paneles sigan siendo usables. */
    private static final int MIN_WIDTH = 900;
    private static final int MIN_HEIGHT = 600;

    private static final int MIN_RES = 2;
    private static final int MAX_RES = 100;

    // ---- estado de los widgets ----
    private final ImInt resolution = new ImInt(25);
    private final int[] radius = {2};          // 1..10, solo enteros
    private final float[] force = {0.5f};      // -1..1
    private Tool tool = Tool.MOUSE;

    // ---- piezas conectadas ----
    private final GraphView graphView;
    private final Graph graph;
    private final Matrix matrix;
    /** Une Grafo + Matriz dispersa + Viewport3D (Dijkstra y suavizado). */
    private final Tools tools;
    /** Viewer 3D: se crea en el primer frame porque necesita el handle de la ventana GLFW. */
    private Viewport3D viewport;

    // ---- ganchos (se conectan desde Main / Tools / Viewer) ----
    /** Se llama al confirmar una nueva resolucion (Enter). */
    private Consumer<Integer> onResolutionChange = r -> { };
    /** Se llama mientras se pinta/borra: (radio, fuerza efectiva, herramienta). Conecta con Tools.ModificateTerrain. */
    private PaintListener onPaint = (r, f, t) -> { };
    /** Dibuja el viewer 3D dentro del espacio central: (ancho, alto). */
    private BiConsumer<Float, Float> viewerRenderer;
    /** Contenido extra del dashboard (p. ej. caracteres de la matriz dispersa). */
    private Runnable dashboardExtra = () -> { };


    // ---- estado del pincel (evita repintar el mismo vertice cada frame) ----
    private static final float PICK_RADIUS = 12f;      // px, igual que Viewport3D
    private static final float DRAG_MIN_PIXELS = 6f;   // el mouse debe moverse para pintar otro vertice
    private int[] lastPainted = null;
    /** Resultado de la ultima exportacion .obj (se muestra bajo el viewer). */
    private String exportMessage = "";
    private boolean exportFailed = false;
    private float lastPaintMouseX, lastPaintMouseY;

    public MainView(Graph graph, Matrix matrix_) {
        this.graph = graph;
        this.graphView = new GraphView(graph);
        Persistent.loadGraph(graphView); // init.graph: se lee solo al iniciar
        this.matrix = matrix_;
        this.tools = new Tools(graph, matrix_);
        // por defecto el pincel/borrador ejecutan el automata; setOnPaint() puede reemplazarlo
        this.onPaint = (r, f, t) -> tools.ModificateTerrain(r, f, t != Tool.ERASER);
    }

    // ------------------------------------------------------------------
    // Conexiones
    // ------------------------------------------------------------------

    public void setOnResolutionChange(Consumer<Integer> c) { this.onResolutionChange = c; }
    public void setOnPaint(PaintListener l) { this.onPaint = l; }
    public void setViewerRenderer(BiConsumer<Float, Float> r) { this.viewerRenderer = r; }
    public void setDashboardExtra(Runnable r) { this.dashboardExtra = r; }

    /** Viewer 3D (null hasta que se dibuja el primer frame). Util para Tools: getSelectedVertex(), addVertexHeight()... */
    public Viewport3D getViewport() { return viewport; }

    public Tools getTools() { return tools; }
    public Graph getGraph() { return graph; }
    public Matrix getMatrix() { return matrix; }

    public int getResolution() { return resolution.get(); }
    public int getRadius() { return radius[0]; }
    public Tool getTool() { return tool; }

    /** Lapiz: fuerza en [0, 1]; Borrador: fuerza en [-1, 0]; Mouse: 0. */
    public double getEffectiveForce() {
        return switch (tool) {
            case BRUSH -> Math.max(0f, force[0]);
            case ERASER -> Math.min(0f, force[0]);
            case MOUSE -> 0.0;
        };
    }

    // ------------------------------------------------------------------
    // Ciclo de vida de la aplicacion
    // ------------------------------------------------------------------

    /** Abre la ventana y arranca el loop de ImGui (bloquea hasta que se cierra la ventana). */
    public void launch() {
        if (launched) {
            System.out.println("[MainView] launch() ya se habia ejecutado; se ignora la segunda llamada");
            return;
        }
        launched = true;
        System.out.println("[MainView] launch()");
        Application.launch(this);
        System.out.println("[MainView] launch() termino (ventana cerrada)");
    }

    private boolean launched = false;

    @Override
    protected void configure(Configuration config) {
        config.setTitle("Automatas - Editor de gradientes");
    }

    @Override
    protected void preRun() {
        // ventana de 1280x720 (el tamano por defecto de Application es distinto)
        GLFW.glfwSetWindowSize(getHandle(), WIDTH, HEIGHT);
        // ventana redimensionable, con un minimo
        GLFW.glfwSetWindowAttrib(getHandle(), GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwSetWindowSizeLimits(getHandle(), MIN_WIDTH, MIN_HEIGHT, GLFW.GLFW_DONT_CARE, GLFW.GLFW_DONT_CARE);
    }

    @Override
    protected void postRun() {
        System.out.println("[MainView] postRun: cerrando");
        // init.graph: se escribe solo al terminar la sesion
        Persistent.saveGraph(graphView);
        // liberar los recursos de OpenGL del viewer al cerrar la ventana
        if (viewport != null) viewport.dispose();
    }

    /** Se ejecuta una vez por frame. */
    @Override
    public void process() {
        render();
    }

    // ------------------------------------------------------------------
    // Render (llamar una vez por frame)
    // ------------------------------------------------------------------

    public void render() {
        // el panel principal ocupa siempre toda la ventana (se adapta al redimensionar)
        float winW = Math.max(1f, ImGui.getIO().getDisplaySizeX());
        float winH = Math.max(1f, ImGui.getIO().getDisplaySizeY());
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(winW, winH);
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse;

        if (ImGui.begin("Automatas", flags)) {
            float totalW = ImGui.getContentRegionAvailX();
            float totalH = ImGui.getContentRegionAvailY();
            float leftW = totalW * 0.60f - 4f;
            float rightW = totalW * 0.40f - 4f;

            ImGui.beginChild("##left", leftW, totalH, true);
            renderLeft(leftW, totalH);
            ImGui.endChild();

            ImGui.sameLine();

            ImGui.beginChild("##right", rightW, totalH, true);
            renderRight(rightW, totalH);
            ImGui.endChild();
        }

        ImGui.end();
    }

    // ------------------------------------------------------------------
    // Panel izquierdo
    // ------------------------------------------------------------------

    private void renderLeft(float w, float h) {
        // ---- fila de parametros: Resolucion | Radio | Fuerza ----
        ImGui.columns(3, "##params", false);

        ImGui.text("Resolucion");
        ImGui.setNextItemWidth(-1);
        if (ImGui.inputInt("##res", resolution, 0, 0, ImGuiInputTextFlags.EnterReturnsTrue)) {
            resolution.set(Math.max(MIN_RES, Math.min(MAX_RES, resolution.get())));
            if (viewport != null) viewport.setSubdivisions(resolution.get());
            rebuildMatrixFromTerrain();
            onResolutionChange.accept(resolution.get());
        }
        ImGui.nextColumn();

        ImGui.text("Radio");
        ImGui.setNextItemWidth(-1);
        ImGui.sliderInt("##radio", radius, 1, 11);
        ImGui.nextColumn();

        ImGui.text("Fuerza");
        ImGui.setNextItemWidth(-1);
        // el rango depende de la herramienta: lapiz 0..1, borrador -1..0, mouse -1..1
        float lo = (tool == Tool.BRUSH) ? 0f : -1f;
        float hi = (tool == Tool.ERASER) ? 0f : 1f;
        ImGui.sliderFloat("##fuerza", force, lo, hi, "%.2f");
        // Ctrl+clic permite teclear un valor; se recorta al rango valido
        force[0] = Math.max(lo, Math.min(hi, force[0]));
        ImGui.nextColumn();

        ImGui.columns(1);

        // ---- espacio del editor 3D ----
        float toolbarH = 48f;
        float infoH = ImGui.getTextLineHeightWithSpacing() * 2f + 4f;
        float viewH = Math.max(60f, ImGui.getContentRegionAvailY() - toolbarH - infoH);
        float viewW = ImGui.getContentRegionAvailX();

        // Viewport3D se dibuja llenando el espacio del child (sin scroll)
        ImGui.beginChild("##viewer", viewW, viewH, false,
                ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
        if (viewerRenderer != null) {
            viewerRenderer.accept(viewW, viewH); // reemplazo opcional del viewer
        } else {
            if (viewport == null) {
                viewport = new Viewport3D(getHandle(), resolution.get(), 10f);
                tools.setViewport(viewport);
                rebuildMatrixFromTerrain(); // matriz = (celdas + 1) x (celdas + 1) vertices
            }
            viewport.render();
        }
        boolean viewerHovered = ImGui.isWindowHovered();
        ImGui.endChild();

        handlePainting(viewerHovered);

        // ---- info del vertice seleccionado + mensajes del automata ----
        renderSelectionInfo();

        // ---- panel de herramientas (stack horizontal) ----
        renderToolbar(viewW);
    }

    /**
     * Pinta/borra con clic o arrastre. Cada vertice se procesa UNA vez por pasada: el Dijkstra parte del
     * simbolo que ya tiene el vertice, asi que repetirlo cada frame destruiria el suavizado.
     */
    private void handlePainting(boolean viewerHovered) {
        boolean down = ImGui.isMouseDown(0);
        if (!down) {
            lastPainted = null;
            return;
        }
        if (viewport == null || tool == Tool.MOUSE || !viewerHovered) return;

        boolean click = ImGui.isMouseClicked(0);
        ImVec2 m = ImGui.getMousePos();
        float mx = m.x, my = m.y;

        boolean moved = lastPainted == null
                || Math.hypot(mx - lastPaintMouseX, my - lastPaintMouseY) >= DRAG_MIN_PIXELS;
        if (!click && !moved) return;

        int[] h = viewport.pickVertex(PICK_RADIUS);
        if (h == null) return;
        boolean same = lastPainted != null && lastPainted[0] == h[0] && lastPainted[1] == h[1];
        if (same && !click) return;

        viewport.selectVertex(h[0], h[1]);
        exportMessage = "";
        onPaint.onPaint(radius[0], getEffectiveForce(), tool);
        lastPainted = h;
        lastPaintMouseX = mx;
        lastPaintMouseY = my;
    }

    /** Linea 1: vertice, altura y simbolo. Linea 2: aviso del automata (ruta inexistente, estado faltante...). */
    private void renderSelectionInfo() {
        int[] v = (viewport != null) ? viewport.getSelectedVertex() : null;
        if (v != null) {
            ImGui.text("Vertice (" + v[0] + ", " + v[1] + ")  altura " + viewport.getVertexHeight(v[0], v[1])
                    + "  simbolo " + symbolLabel(symbolAt(v[0], v[1])));
        } else {
            ImGui.text("Sin vertice seleccionado (clic en el terreno)");
        }
        String msg = tools.getLastMessage();
        if (!msg.isEmpty()) ImGui.textColored(1f, 0.6f, 0.3f, 1f, msg);
        else if (!exportMessage.isEmpty()) {
            if (exportFailed) ImGui.textColored(1f, 0.4f, 0.4f, 1f, exportMessage);
            else ImGui.textColored(0.5f, 0.9f, 0.5f, 1f, exportMessage);
        } else ImGui.text("");
    }

    /** Simbolo guardado en la matriz dispersa; celda vacia (null) = ':' (altura 0). */
    private char symbolAt(int x, int y) {
        if (x < 0 || y < 0 || x >= matrix.getSize() || y >= matrix.getSize()) return ':';
        char c = matrix.getInCoord(x, y);
        return c == '\0' ? ':' : c;
    }

    private static String symbolLabel(char c) {
        return c == ' ' ? "(espacio)" : "'" + c + "'";
    }

    /**
     * Deja la matriz dispersa con (celdas + 1)^2 vertices y la rellena desde las alturas del terreno
     * (el resample bilineal del viewer al cambiar la resolucion no conserva los simbolos).
     */
    private void rebuildMatrixFromTerrain() {
        matrix.clear();
        int cells = (viewport != null) ? viewport.getSubdivisions() : resolution.get();
        int n = cells + 1;
        matrix.setNewSize(n);
        if (viewport == null) return;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                char c = Automate.GetCharByHeight(viewport.getVertexHeight(x, y));
                if (c != ':') matrix.insertInCoord(x, y, c);
            }
        }
        lastPainted = null;
    }

    private void renderToolbar(float panelW) {
        float btnW = 100f;
        float exportW = 120f;
        float gap = ImGui.getStyle().getItemSpacingX();
        float total = btnW * 3 + exportW + gap * 3;
        ImGui.setCursorPosX(Math.max(0f, (panelW - total) / 2f));

        toolButton("Mouse", Tool.MOUSE, btnW);
        ImGui.sameLine();
        toolButton("Pincel", Tool.BRUSH, btnW);
        ImGui.sameLine();
        toolButton("Borrador", Tool.ERASER, btnW);
        ImGui.sameLine();
        // no es una herramienta: ejecuta una accion y no cambia la herramienta activa
        if (ImGui.button("Exportar .obj", exportW, 34f)) exportTerrain();
    }

    /** Exporta el terreno actual a exports/terreno_<fecha>.obj y deja el resultado en exportMessage. */
    private void exportTerrain() {
        if (viewport == null) {
            exportMessage = "El viewer 3D aun no esta listo.";
            exportFailed = true;
            return;
        }
        try {
            Path file = Persistent.exportObj(viewport.getTerrain());
            exportMessage = "Exportado: " + file;
            exportFailed = false;
        } catch (IOException ex) {
            exportMessage = "No se pudo exportar: " + ex.getMessage();
            exportFailed = true;
        }
    }

    private void toolButton(String text, Tool t, float w) {
        boolean active = (tool == t);
        if (active) ImGui.pushStyleColor(ImGuiCol.Button, 0xFFC8782D); // ABGR
        if (ImGui.button(text, w, 34f)) selectTool(t);
        if (active) ImGui.popStyleColor();
    }

    private void selectTool(Tool t) {
        tool = t;
        // mantener la fuerza dentro del rango de la herramienta
        if (t == Tool.BRUSH && force[0] < 0f) force[0] = -force[0];
        if (t == Tool.ERASER && force[0] > 0f) force[0] = -force[0];
    }

    // ------------------------------------------------------------------
    // Panel derecho
    // ------------------------------------------------------------------

    private void renderRight(float w, float h) {
        float editorH = h * 0.58f;

        ImGui.beginChild("##editor", 0, editorH, false);
        graphView.render(ImGui.getContentRegionAvailX(), editorH);
        ImGui.endChild();

        ImGui.separator();
        renderDashboard();
    }

    private void renderDashboard() {
        ImGui.text("Automata Gen");
        ImGui.text("Q = " + graphView.getStatesLabel());
        ImGui.textWrapped("lenguaje (sigma) = " + graphView.getLanguageLabel());
        ImGui.text("Cantidad de estados: " + graphView.getStateCount());
        // Estado actual = simbolo del vertice seleccionado
        int[] sel = (viewport != null) ? viewport.getSelectedVertex() : null;
        ImGui.text("Estado actual: " + (sel != null ? symbolLabel(symbolAt(sel[0], sel[1])) : "-"));
        ImGui.separator();
        dashboardExtra.run();
        renderMatrixView();
    }

    /**
     * Panel inferior derecho: cadena de la matriz dispersa = vista superior del terreno con simbolos.
     * Filas = y (de arriba hacia abajo), columnas = x; celda vacia (null) se muestra como ':'.
     * Cada celda se dibuja en una rejilla fija (la fuente no es monoespaciada) y solo las visibles.
     */
    private void renderMatrixView() {
        int n = matrix.getSize();
        ImGui.text("Matriz dispersa " + n + "x" + n + " (vista superior, null = ':')");

        float cellW = ImGui.calcTextSize("W").x + 3f;
        float cellH = ImGui.getTextLineHeight() + 1f;

        ImGui.beginChild("##matrixView", 0, 0, true, ImGuiWindowFlags.HorizontalScrollbar);

        ImVec2 o = ImGui.getCursorScreenPos();
        float ox = o.x, oy = o.y;
        ImVec2 wp = ImGui.getWindowPos();
        float wx = wp.x, wy = wp.y;
        float ww = ImGui.getWindowWidth(), wh = ImGui.getWindowHeight();

        ImGui.dummy(n * cellW, n * cellH); // fija el area desplazable

        int c0 = Math.max(0, (int) Math.floor((wx - ox) / cellW));
        int c1 = Math.min(n - 1, (int) Math.ceil((wx + ww - ox) / cellW));
        int r0 = Math.max(0, (int) Math.floor((wy - oy) / cellH));
        int r1 = Math.min(n - 1, (int) Math.ceil((wy + wh - oy) / cellH));

        ImDrawList dl = ImGui.getWindowDrawList();
        int textCol = ImGui.getColorU32(0.92f, 0.92f, 0.92f, 1f);
        int dimCol = ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1f);

        for (int y = r0; y <= r1; y++) {
            for (int x = c0; x <= c1; x++) {
                char c = matrix.getInCoord(x, y);
                boolean empty = (c == '\0');
                if (empty) c = ':';

                float px = ox + x * cellW, py = oy + y * cellH;
                int h = Automate.GetHeightByChar(c);
                if (h != 0) {
                    float t = Math.abs(h) / 5f * 0.55f;   // azul = bajo, naranja = alto (tambien marca el ' ')
                    int bg = (h < 0) ? ImGui.getColorU32(0.2f, 0.4f, 0.9f, t)
                                     : ImGui.getColorU32(0.95f, 0.5f, 0.2f, t);
                    dl.addRectFilled(px, py, px + cellW, py + cellH, bg);
                }
                dl.addText(px + 1.5f, py, empty ? dimCol : textCol, String.valueOf(c));
            }
        }

        // vertice seleccionado en el 3D
        int[] sel = (viewport != null) ? viewport.getSelectedVertex() : null;
        if (sel != null && sel[0] < n && sel[1] < n) {
            float px = ox + sel[0] * cellW, py = oy + sel[1] * cellH;
            dl.addRect(px, py, px + cellW, py + cellH, ImGui.getColorU32(1f, 0.6f, 0.1f, 1f));
        }

        ImGui.endChild();
    }
}