package com.automatas5.Views;

import com.automatas5.Views.Dimension.Viewport3D;
import com.automatas5.structs.Graph;
import imgui.ImGui;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    private static final int MIN_RES = 2;
    private static final int MAX_RES = 100;

    // ---- estado de los widgets ----
    private final ImInt resolution = new ImInt(25);
    private final int[] radius = {5};          // 1..10, solo enteros
    private final float[] force = {0.5f};      // -1..1
    private Tool tool = Tool.MOUSE;

    // ---- piezas conectadas ----
    private final GraphView graphView;
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

    public MainView(Graph graph) {
        this.graphView = new GraphView(graph);
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
    }

    @Override
    protected void postRun() {
        System.out.println("[MainView] postRun: cerrando");
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
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(WIDTH, HEIGHT);
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
            onResolutionChange.accept(resolution.get());
        }
        ImGui.nextColumn();

        ImGui.text("Radio");
        ImGui.setNextItemWidth(-1);
        ImGui.sliderInt("##radio", radius, 1, 10);
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
        float viewH = ImGui.getContentRegionAvailY() - toolbarH;
        float viewW = ImGui.getContentRegionAvailX();

        // Viewport3D se dibuja llenando el espacio del child (sin scroll)
        ImGui.beginChild("##viewer", viewW, viewH, false,
                ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
        if (viewerRenderer != null) {
            viewerRenderer.accept(viewW, viewH); // reemplazo opcional del viewer
        } else {
            if (viewport == null) {
                viewport = new Viewport3D(getHandle(), resolution.get(), 10f);
            }
            viewport.render();
        }
        boolean viewerHovered = ImGui.isWindowHovered();
        ImGui.endChild();

        // pintar / borrar mientras se mantiene el clic sobre el viewer
        if (tool != Tool.MOUSE && viewerHovered && ImGui.isMouseDown(0)) {
            onPaint.onPaint(radius[0], getEffectiveForce(), tool);
        }

        // ---- panel de herramientas (stack horizontal) ----
        renderToolbar(viewW);
    }

    private void renderToolbar(float panelW) {
        float btnW = 100f;
        float startX = (panelW - btnW * 3 - ImGui.getStyle().getItemSpacingX() * 2) / 2f;
        ImGui.setCursorPosX(Math.max(0f, startX));

        toolButton("Mouse", Tool.MOUSE, btnW);
        ImGui.sameLine();
        toolButton("Pincel", Tool.BRUSH, btnW);
        ImGui.sameLine();
        toolButton("Borrador", Tool.ERASER, btnW);
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
        // Estado actual: conectar cuando exista en el automata
        ImGui.text("Estado actual: -");
        ImGui.separator();
        dashboardExtra.run(); // aqui va el recorrido de la matriz dispersa
    }
}