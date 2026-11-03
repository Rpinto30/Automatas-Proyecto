package com.automatas5.Views;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.automatas5.structs.Graph;

import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;

/**
 * Editor visual del AFD (grafo de Diego).
 * Opciones: Crear nodo, Conectar y Propiedades.
 *
 * Los nodos de la UI se llaman q0, q1, ... y cada uno tiene un valor (simbolo)
 * unico del alfabeto. El peso de las aristas NO se calcula aqui: lo hace Graph.
 */
public class GraphView {

    /** Alfabeto por defecto (mismos simbolos que Automate.GetHeightByChar). */
    public static final char[] ALPHABET = {'#', '@', '%', '*', ';', ':', '!', '-', ',', '.', ' '};

    private static final float NODE_R = 18f;
    private static final float PROPS_HEIGHT = 150f;
    private static final String DIALOG_ID = "Conectar estado";
    /** Linea verde de diagnostico en el canvas (modo, hover, mouse, nodos). Poner en false al terminar. */
    private static final boolean DEBUG = true;

    /** SELECT = ninguna opcion activa (solo seleccionar y arrastrar). Las otras tres son excluyentes. */
    private enum Mode { SELECT, CREATE, CONNECT, PROPS }

    private static class UINode {
        final String name;
        char value;
        float x, y; // relativas al origen del canvas
        final List<String> links = new ArrayList<>();

        UINode(String name, char value, float x, float y) {
            this.name = name;
            this.value = value;
            this.x = x;
            this.y = y;
        }
    }

    private final Graph graph;
    private final List<UINode> nodes = new ArrayList<>();

    private Mode mode = Mode.SELECT;
    private UINode selected;
    private UINode dragging;
    private UINode connectOrigin;
    private int nextId = 0;
    private float clickX = -1, clickY = -1; // ultimo clic dentro del canvas (diagnostico)
    private String lastEvent = "(sin eventos)"; // solo para el diagnostico
    private String rawClick = "-";               // solo para el diagnostico

    private final ImString connectTarget = new ImString(16);
    private String connectError = "";
    private boolean openConnectDialog = false;
    private String statusMsg = "";

    public GraphView(Graph graph) {
        this.graph = graph;
    }

    // ------------------------------------------------------------------
    // API para el dashboard / MainView
    // ------------------------------------------------------------------

    public int getStateCount() {
        return nodes.size();
    }

    public String getStatesLabel() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(nodes.get(i).name);
        }
        return sb.append("}").toString();
    }

    public String getLanguageLabel() {
        StringBuilder sb = new StringBuilder("{ ");
        for (int i = 0; i < ALPHABET.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(label(ALPHABET[i]));
        }
        return sb.append(" }").toString();
    }

    // ------------------------------------------------------------------
    // Persistencia (la usa Persistent: init.graph)
    // ------------------------------------------------------------------

    /** Copia inmutable de un nodo de la UI. */
    public static class NodeState {
        public final String name;
        public final char value;
        public final float x, y;

        public NodeState(String name, char value, float x, float y) {
            this.name = name;
            this.value = value;
            this.x = x;
            this.y = y;
        }
    }

    public List<NodeState> getNodeStates() {
        List<NodeState> out = new ArrayList<>();
        for (UINode n : nodes) out.add(new NodeState(n.name, n.value, n.x, n.y));
        return out;
    }

    /** Aristas (no dirigidas) como pares {nombreA, nombreB}, cada una una sola vez. */
    public List<String[]> getEdges() {
        List<String[]> out = new ArrayList<>();
        for (UINode a : nodes) {
            for (String other : a.links) {
                UINode b = find(other);
                if (b != null && nodes.indexOf(a) < nodes.indexOf(b)) {
                    out.add(new String[] { a.name, b.name });
                }
            }
        }
        return out;
    }

    /**
     * Reemplaza el automata actual por el recibido (nodos de la UI + Graph sincronizado).
     * Ignora lo invalido: simbolos fuera del alfabeto, repetidos, nombres duplicados o aristas a nodos inexistentes.
     * @return cantidad de nodos cargados
     */
    public int loadState(List<NodeState> states, List<String[]> edges) {
        // limpiar lo que hubiera, tambien en el Graph
        for (UINode old : new ArrayList<>(nodes)) {
            final char v = old.value;
            safeGraph(() -> graph.DeleteNode(v));
        }
        nodes.clear();
        selected = null;
        dragging = null;
        connectOrigin = null;
        mode = Mode.SELECT;
        statusMsg = "";
        nextId = 0;

        String alphabet = new String(ALPHABET);
        Pattern idPattern = Pattern.compile("q(\\d+)");

        for (NodeState st : states) {
            if (st.name == null || st.name.isEmpty()) continue;
            if (find(st.name) != null) continue;                 // nombre repetido
            if (alphabet.indexOf(st.value) < 0) continue;        // fuera del alfabeto
            if (isValueUsed(st.value, null)) continue;           // simbolo repetido

            nodes.add(new UINode(st.name, st.value, st.x, st.y));
            syncInsert(st.value);

            Matcher m = idPattern.matcher(st.name);
            if (m.matches()) {
                try { nextId = Math.max(nextId, Integer.parseInt(m.group(1)) + 1); }
                catch (NumberFormatException ignored) { /* id enorme: se ignora */ }
            }
        }
        nextId = Math.max(nextId, nodes.size());

        for (String[] e : edges) {
            if (e == null || e.length < 2) continue;
            UINode a = find(e[0]);
            UINode b = find(e[1]);
            if (a == null || b == null || a == b || a.links.contains(b.name)) continue;
            a.links.add(b.name);
            b.links.add(a.name);
            syncConnect(a.value, b.value);
        }
        return nodes.size();
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    public void render(float width, float height) {
        boolean showProps = (mode == Mode.PROPS);
        renderToolbar();

        float canvasH = height - ImGui.getFrameHeightWithSpacing() - 6f;
        if (showProps) canvasH -= PROPS_HEIGHT + 8f;
        if (!statusMsg.isEmpty()) canvasH -= ImGui.getTextLineHeightWithSpacing();
        canvasH = Math.max(canvasH, 80f);

        renderCanvas(width, canvasH);

        if (!statusMsg.isEmpty()) {
            ImGui.textColored(1f, 0.6f, 0.3f, 1f, statusMsg);
        }

        if (showProps) renderProperties(PROPS_HEIGHT);

        renderConnectDialog();
    }

    private void renderToolbar() {
        if (modeButton("Crear nodo", mode == Mode.CREATE)) {
            toggleMode(Mode.CREATE);
        }
        ImGui.sameLine();
        if (modeButton("Conectar", mode == Mode.CONNECT)) {
            toggleMode(Mode.CONNECT);
            // si ya hay un nodo seleccionado, abrir el dialogo directo
            if (mode == Mode.CONNECT && selected != null) {
                startConnect(selected);
            }
        }
        ImGui.sameLine();
        if (modeButton("Propiedades", mode == Mode.PROPS)) {
            toggleMode(Mode.PROPS);
        }
    }

    /** Solo una opcion activa a la vez: elegir otra desactiva la anterior; volver a pulsar la activa la apaga. */
    private void toggleMode(Mode m) {
        mode = (mode == m) ? Mode.SELECT : m;
        statusMsg = "";
    }

    private boolean modeButton(String text, boolean active) {
        if (active) ImGui.pushStyleColor(ImGuiCol.Button, ImColor.rgba(60, 90, 200, 255));
        boolean clicked = ImGui.button(text);
        if (active) ImGui.popStyleColor();
        return clicked;
    }

    private void renderCanvas(float w, float h) {
        ImVec2 origin = ImGui.getCursorScreenPos();
        float ox = origin.x, oy = origin.y;

        ImGui.invisibleButton("##graph_canvas", w, h);
        // hover independiente de la logica de ventanas de ImGui:
        // el mouse esta dentro del rectangulo del canvas y no hay un dialogo modal abierto
        float mouseX = ImGui.getMousePosX(), mouseY = ImGui.getMousePosY();
        boolean inRect = mouseX >= ox && mouseX < ox + w && mouseY >= oy && mouseY < oy + h;
        boolean hovered = (ImGui.isItemHovered() || inRect) && !ImGui.isPopupOpen(DIALOG_ID);
        if (ImGui.isMouseClicked(0)) {
            rawClick = "(" + (int) mouseX + "," + (int) mouseY + ") enRect=" + inRect;
        }

        float mx = ImGui.getMousePosX() - ox;
        float my = ImGui.getMousePosY() - oy;

        if (hovered && ImGui.isMouseClicked(0)) {
            UINode hit = nodeAt(mx, my);
            lastEvent = "clic (" + (int) mx + "," + (int) my + ") modo=" + mode
                    + " hit=" + (hit == null ? "-" : hit.name);
            if (DEBUG) System.out.println("[GraphView] " + lastEvent);
            clickX = mx;
            clickY = my;
            switch (mode) {
                case CREATE -> {
                    if (hit == null) createNode(mx, my);
                    else selected = hit;
                }
                case CONNECT -> {
                    if (hit != null) {
                        selected = hit;
                        startConnect(hit);
                    }
                }
                case SELECT, PROPS -> {
                    selected = hit;
                    dragging = hit;
                }
            }
        }

        if (dragging != null) {
            if (ImGui.isMouseDown(0)) {
                dragging.x = clamp(dragging.x + ImGui.getIO().getMouseDeltaX(), NODE_R, w - NODE_R);
                dragging.y = clamp(dragging.y + ImGui.getIO().getMouseDeltaY(), NODE_R, h - NODE_R);
            } else {
                dragging = null;
            }
        }

        // ---- dibujo ----
        ImDrawList dl = ImGui.getWindowDrawList();
        dl.pushClipRect(ox, oy, ox + w, oy + h, true);
        dl.addRectFilled(ox, oy, ox + w, oy + h, ImColor.rgba(28, 28, 32, 255));
        dl.addRect(ox, oy, ox + w, oy + h, ImColor.rgba(120, 120, 130, 255));

        int edgeCol = ImColor.rgba(190, 190, 200, 255);
        int edgeHi = ImColor.rgba(255, 200, 60, 255);
        for (UINode a : nodes) {
            for (String linkName : a.links) {
                UINode b = find(linkName);
                // dibujar cada arista una sola vez
                if (b != null && a.name.compareTo(b.name) < 0) {
                    boolean hi = (a == selected || b == selected); // aristas del nodo seleccionado
                    dl.addLine(ox + a.x, oy + a.y, ox + b.x, oy + b.y, hi ? edgeHi : edgeCol, hi ? 3f : 2f);
                }
            }
        }

        ImVec2 ts = new ImVec2();
        for (UINode n : nodes) {
            float cx = ox + n.x, cy = oy + n.y;
            boolean sel = (n == selected);
            // vecino = nodo conectado al seleccionado
            boolean near = !sel && selected != null && selected.links.contains(n.name);
            dl.addCircleFilled(cx, cy, NODE_R,
                    near ? ImColor.rgba(40, 110, 130, 255) : ImColor.rgba(40, 60, 120, 255));
            int ring = sel ? ImColor.rgba(255, 220, 80, 255)
                    : near ? ImColor.rgba(90, 220, 220, 255)
                    : ImColor.rgba(220, 220, 230, 255);
            dl.addCircle(cx, cy, NODE_R, ring, 0, (sel || near) ? 3f : 2f);

            String v = String.valueOf(n.value == ' ' ? '_' : n.value);
            ImGui.calcTextSize(ts, v);
            dl.addText(cx - ts.x / 2f, cy - ts.y / 2f, ImColor.rgba(255, 255, 255, 255), v);

            ImGui.calcTextSize(ts, n.name);
            dl.addText(cx - ts.x / 2f, cy - NODE_R - ts.y - 2f, ImColor.rgba(200, 200, 210, 255), n.name);
        }

        if (nodes.isEmpty()) {
            dl.addText(ox + 10f, oy + 10f, ImColor.rgba(150, 150, 160, 255),
                    "Usa \"Crear nodo\" y haz clic en el canvas");
        }
        if (DEBUG) {
            int green = ImColor.rgba(120, 255, 120, 255);
            if (clickX >= 0) { // cruz roja donde cayo el ultimo clic dentro del canvas
                int red = ImColor.rgba(255, 60, 60, 255);
                dl.addLine(ox + clickX - 7f, oy + clickY, ox + clickX + 7f, oy + clickY, red, 2f);
                dl.addLine(ox + clickX, oy + clickY - 7f, ox + clickX, oy + clickY + 7f, red, 2f);
            }
            dl.addText(ox + 6f, oy + h - 50f, green, "clicRaw=" + rawClick + " canvas=(" + (int) ox + ","
                    + (int) oy + ")-(" + (int) (ox + w) + "," + (int) (oy + h) + ")");
            dl.addText(ox + 6f, oy + h - 34f, green, "ultimo: " + lastEvent);
            dl.addText(ox + 6f, oy + h - 18f, green,
                    "modo=" + mode + " hover=" + hovered + " mouse=(" + (int) mx + "," + (int) my + ")"
                            + " nodos=" + nodes.size() + " sel=" + (selected == null ? "-" : selected.name));
        }
        dl.popClipRect();
    }

    private void renderProperties(float h) {
        ImGui.beginChild("##props", 0, h, true);
        if (selected == null) {
            ImGui.textDisabled("Selecciona un nodo para ver sus propiedades");
            ImGui.endChild();
            return;
        }

        ImGui.text("Nodo " + selected.name);

        // ---- valor (combobox con el alfabeto) ----
        ImGui.text("Valor");
        ImGui.sameLine();
        ImGui.setNextItemWidth(110f);
        if (ImGui.beginCombo("##valor", label(selected.value))) {
            for (char c : ALPHABET) {
                boolean usedByOther = isValueUsed(c, selected);
                if (usedByOther) ImGui.beginDisabled();
                if (ImGui.selectable(label(c), c == selected.value)) {
                    changeValue(selected, c);
                }
                if (usedByOther) ImGui.endDisabled();
            }
            ImGui.endCombo();
        }

        // ---- conexiones ----
        ImGui.text("Conexiones");
        ImGui.beginChild("##conns", 0, 50f, false);
        String toRemove = null;
        for (int i = 0; i < selected.links.size(); i++) {
            String other = selected.links.get(i);
            if (ImGui.smallButton("x##rm" + i)) toRemove = other;
            ImGui.sameLine();
            ImGui.text(selected.name + " <---> " + other);
        }
        ImGui.endChild();
        if (toRemove != null) disconnect(selected, toRemove);

        if (ImGui.button("Agregar")) startConnect(selected);

        ImGui.endChild();
    }

    private void renderConnectDialog() {
        if (openConnectDialog) {
            ImGui.openPopup(DIALOG_ID);
            openConnectDialog = false;
        }
        if (ImGui.beginPopupModal(DIALOG_ID, ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text("Ingresa estado al que deseas conectar");
            ImGui.setNextItemWidth(220f);
            ImGui.inputText("##target", connectTarget);

            if (!connectError.isEmpty()) {
                ImGui.textColored(1f, 0.35f, 0.35f, 1f, connectError);
            }

            if (ImGui.button("Cancelar")) {
                closeDialog();
            }
            ImGui.sameLine();
            if (ImGui.button("Aceptar")) {
                if (tryConnect()) closeDialog();
            }
            ImGui.endPopup();
        }
    }

    // ------------------------------------------------------------------
    // Logica del editor
    // ------------------------------------------------------------------

    private void createNode(float x, float y) {
        Character free = firstFreeSymbol();
        if (free == null) {
            statusMsg = "No quedan simbolos libres: cada nodo necesita un valor unico.";
            return;
        }
        UINode n = new UINode("q" + nextId++, free, x, y);
        nodes.add(n);
        lastEvent = "creado " + n.name + " valor=" + n.value + " en (" + (int) x + "," + (int) y + ")";
        if (DEBUG) System.out.println("[GraphView] " + lastEvent);
        selected = n;
        statusMsg = "";
        syncInsert(n.value);
    }

    private void startConnect(UINode origin) {
        connectOrigin = origin;
        connectTarget.set("");
        connectError = "";
        openConnectDialog = true;
    }

    private void closeDialog() {
        ImGui.closeCurrentPopup();
        connectOrigin = null;
        connectError = "";
        if (mode == Mode.CONNECT) mode = Mode.SELECT;
    }

    private boolean tryConnect() {
        String name = connectTarget.get().trim();
        UINode dest = find(name);
        if (connectOrigin == null) {
            connectError = "No hay nodo de origen.";
            return false;
        }
        if (name.isEmpty() || dest == null) {
            connectError = "El estado \"" + name + "\" no existe.";
            return false;
        }
        if (dest == connectOrigin) {
            connectError = "No se puede conectar un estado consigo mismo.";
            return false;
        }
        if (connectOrigin.links.contains(dest.name)) {
            connectError = "Esos estados ya estan conectados.";
            return false;
        }
        connectOrigin.links.add(dest.name);
        dest.links.add(connectOrigin.name);
        syncConnect(connectOrigin.value, dest.value);
        return true;
    }

    private void disconnect(UINode a, String otherName) {
        UINode b = find(otherName);
        a.links.remove(otherName);
        if (b != null) {
            b.links.remove(a.name);
            syncDisconnect(a.value, b.value);
        }
    }

    private void changeValue(UINode n, char newValue) {
        if (isValueUsed(newValue, n)) return; // regla: sin valores repetidos
        char old = n.value;
        if (!syncChangeValue(old, newValue)) return; // el grafo lo rechazo: la UI no cambia
        n.value = newValue;
    }

    private boolean isValueUsed(char c, UINode except) {
        for (UINode n : nodes) {
            if (n != except && n.value == c) return true;
        }
        return false;
    }

    private Character firstFreeSymbol() {
        for (char c : ALPHABET) {
            if (!isValueUsed(c, null)) return c;
        }
        return null;
    }

    private UINode find(String name) {
        for (UINode n : nodes) {
            if (n.name.equalsIgnoreCase(name)) return n;
        }
        return null;
    }

    private UINode nodeAt(float mx, float my) {
        // de atras hacia adelante: gana el nodo dibujado encima
        for (int i = nodes.size() - 1; i >= 0; i--) {
            UINode n = nodes.get(i);
            float dx = mx - n.x, dy = my - n.y;
            if (dx * dx + dy * dy <= NODE_R * NODE_R) return n;
        }
        return null;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String label(char c) {
        return c == ' ' ? "(espacio)" : String.valueOf(c);
    }

    // ------------------------------------------------------------------
    // Puente con el Graph de Diego: si cambia su API, solo se toca aqui
    // ------------------------------------------------------------------

    /** Ejecuta una llamada al Graph sin que un fallo (null, placeholder, excepcion) rompa la UI. */
    private void safeGraph(Runnable call) {
        if (graph == null) return;
        try {
            call.run();
        } catch (RuntimeException ex) {
            statusMsg = "Graph: " + ex;
            ex.printStackTrace();
        }
    }

    /** Quita la arista from -> to directo del mapa de adyacencia (el Graph real no trae DisconnectNodes). */
    private void removeEdge(char from, char to) {
        List<String> out = graph.vertexMap.get(String.valueOf(from));
        if (out != null) out.remove(String.valueOf(to));
    }

    private void syncInsert(char value) {
        safeGraph(() -> graph.InsertNode(value));
    }

    private void syncConnect(char a, char b) {
        // el grafo de ejemplo es simetrico (# -> @,- ; @ -> #,- ...), se conecta en ambos sentidos
        safeGraph(() -> {
            graph.ConnectNodes(a, b);
            graph.ConnectNodes(b, a);
        });
    }

    private void syncDisconnect(char a, char b) {
        safeGraph(() -> {
            removeEdge(a, b);
            removeEdge(b, a);
        });
    }

    /** @return true si el grafo acepto el cambio (ModifyNode valida unicidad y alfabeto). */
    private boolean syncChangeValue(char oldValue, char newValue) {
        if (graph == null) return true;
        try {
            if (graph.ModifyNode(oldValue, newValue)) return true;
            statusMsg = "El grafo rechazo el cambio de '" + oldValue + "' a '" + newValue + "'.";
            return false;
        } catch (RuntimeException ex) {
            statusMsg = "Graph: " + ex;
            ex.printStackTrace();
            return false;
        }
    }
}