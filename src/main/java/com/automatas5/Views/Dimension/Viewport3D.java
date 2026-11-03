package com.automatas5.Views.Dimension;

import java.nio.ByteBuffer;

import org.lwjgl.glfw.GLFW;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FILL;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_LINE;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_FILL;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glGetIntegerv;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glPointSize;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.glPolygonOffset;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDeleteRenderbuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiButtonFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;

/**
 * 3D viewport that can be embedded in any ImGui window.
 *
 * It renders the terrain into an offscreen framebuffer (FBO) and shows the
 * resulting texture with ImGui.image(), so it behaves like any other ImGui widget.
 *
 * Controls (only active while the mouse is over the viewport):
 *  - Middle mouse drag ............ orbit
 *  - Shift + middle mouse drag .... pan
 *  - Ctrl  + middle mouse drag .... zoom
 *  - Mouse wheel .................. zoom
 *  - Numpad 0 ..................... front view
 *  - Numpad 1 ..................... top view
 *
 * Requires an OpenGL 3.3+ context.
 */
public class Viewport3D {

    private static final String VERTEX_SHADER =
        "#version 330 core\n" +
        "layout(location = 0) in vec3 aPos;\n" +
        "layout(location = 1) in vec3 aNormal;\n" +
        "uniform mat4 uMvp;\n" +
        "out vec3 vNormal;\n" +
        "void main() {\n" +
        "    vNormal = aNormal;\n" +
        "    gl_Position = uMvp * vec4(aPos, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 330 core\n" +
        "in vec3 vNormal;\n" +
        "uniform vec4 uColor;\n" +
        "uniform int uLit;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    if (uLit == 1) {\n" +
        "        vec3 n = normalize(vNormal);\n" +
        "        vec3 l = normalize(vec3(0.4, 1.0, 0.3));\n" +
        "        float d = abs(dot(n, l));\n" +
        "        fragColor = vec4(uColor.rgb * (0.3 + 0.7 * d), uColor.a);\n" +
        "    } else {\n" +
        "        fragColor = uColor;\n" +
        "    }\n" +
        "}\n";

    private final long window; // GLFW window handle (used to poll keyboard)
    private final TerrainMesh terrain;
    private final OrbitCamera camera = new OrbitCamera();

    // GL objects
    private int program, uMvp, uColor, uLit;
    private int fbo, colorTex, depthRbo;
    private int fbWidth, fbHeight;
    private boolean initialized;

    // Display options
    private boolean showWireframe = true;

    // Input state
    private boolean dragging;
    private float lastMouseX, lastMouseY;
    private boolean numpad0WasDown, numpad1WasDown;
    private float aspect = 1f;


    // Posición/tamaño del viewport en pantalla (para el picking)
    private float viewX, viewY, viewW = 1f, viewH = 1f;

    // Selección
    private static final float PICK_RADIUS = 12f; // píxeles
    private boolean hasSelection;
    private int selX, selY;
    private int selVao, selVbo;

    /**
     * @param window       GLFW window handle (needed to read the numpad / modifier keys)
     * @param subdivisions initial subdivisions per side of the plane
     * @param planeSize    world-space size of the plane
     */
    public Viewport3D(long window, int subdivisions, float planeSize) {
        this.window = window;
        this.terrain = new TerrainMesh(subdivisions, planeSize);
        this.camera.frame(planeSize, 1f);
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Creates shaders and GPU buffers. Call once, with the GL context current. */
    public void init() {
        if (initialized) return;

        int vs = compile(GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compile(GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Program link error: " + glGetProgramInfoLog(program));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);

        uMvp = glGetUniformLocation(program, "uMvp");
        uColor = glGetUniformLocation(program, "uColor");
        uLit = glGetUniformLocation(program, "uLit");

        selVao = glGenVertexArrays();
        selVbo = glGenBuffers();
        glBindVertexArray(selVao);
        glBindBuffer(GL_ARRAY_BUFFER, selVbo);
        int stride = 6 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glBindVertexArray(0);

        terrain.initGL();
        initialized = true;
        
    }

    /** Releases all GPU resources. */
    public void dispose() {
        if (!initialized) return;
        terrain.dispose();
        glDeleteProgram(program);
        glDeleteVertexArrays(selVao);
        glDeleteBuffers(selVbo);
        
        if (fbo != 0) {
            glDeleteFramebuffers(fbo);
            glDeleteTextures(colorTex);
            glDeleteRenderbuffers(depthRbo);
            fbo = 0;
        }
        initialized = false;
    }

    // ------------------------------------------------------------------
    // ImGui integration
    // ------------------------------------------------------------------

    /**
     * Convenience: opens an ImGui window with no padding / no scrollbars and
     * draws the viewport inside it. Call between ImGui.newFrame() and ImGui.render().
     */
    public void showWindow(String title) {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f);
        boolean open = ImGui.begin(title, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
        ImGui.popStyleVar();
        if (open) {
            render();
        }
        ImGui.end();
    }

    /**
     * Draws the viewport filling the available space of the current ImGui
     * window / child. Use this to embed it inside your own layout.
     */
    public void render() {
        if (!initialized) init();

        ImVec2 avail = ImGui.getContentRegionAvail();
        int w = Math.max(1, (int) avail.x);
        int h = Math.max(1, (int) avail.y);
        aspect = (float) w / h;

        resizeFramebuffer(w, h);

        // Show the FBO texture (flipped vertically) and cover it with an
        // invisible button so clicks don't drag the ImGui window.
        ImVec2 pos = ImGui.getCursorScreenPos();
        float px = pos.x, py = pos.y;
        viewX = px; viewY = py; viewW = w; viewH = h;
        ImGui.image(colorTex, w, h, 0f, 1f, 1f, 0f);
        ImGui.setCursorScreenPos(px, py);
        ImGui.invisibleButton("##viewport3d", w, h,
            ImGuiButtonFlags.MouseButtonLeft | ImGuiButtonFlags.MouseButtonRight | ImGuiButtonFlags.MouseButtonMiddle);
        boolean hovered = ImGui.isItemHovered();

        handleInput(hovered, h);
        renderScene();
        drawSelectionOverlay(hovered); 
    }

    // ------------------------------------------------------------------
    // Public API: views
    // ------------------------------------------------------------------

    /** Numpad 0: front view (the plane is seen edge-on, handy to check heights). */
    public void viewFront() {
        camera.viewFront(terrain.getPlaneSize(), aspect);
    }

    /** Numpad 1: top view. */
    public void viewTop() {
        camera.viewTop(terrain.getPlaneSize(), aspect);
    }

    public OrbitCamera getCamera() { return camera; }

    public void setWireframe(boolean enabled) { this.showWireframe = enabled; }

    public boolean isWireframe() { return showWireframe; }

    // ------------------------------------------------------------------
    // Public API: subdivision and terrain editing (delegates to TerrainMesh)
    // ------------------------------------------------------------------

    public void increaseSubdivisions(int newSize) { terrain.increaseSubdivisions(newSize); }

    public void decreaseSubdivisions(int newSize) { terrain.decreaseSubdivisions(newSize); }

    public void setSubdivisions(int cells) { terrain.setSubdivisions(cells); }

    public int getSubdivisions() { return terrain.getSubdivisions(); }

    public void setPlaneSize(float size) { terrain.setPlaneSize(size); }

    /** Sets the height of vertex (ix, iz). Hook for the sculpting tools. */
    public void setHeight(int ix, int iz, float height) { terrain.setHeight(ix, iz, height); }

    /** Adds delta to the height of vertex (ix, iz). Hook for the sculpting tools. */
    public void addHeight(int ix, int iz, float delta) { terrain.addHeight(ix, iz, delta); }

    public float getHeight(int ix, int iz) { return terrain.getHeight(ix, iz); }

    /** Direct access to the mesh (worldX/worldZ helpers, bounds checks, etc.). */
    public TerrainMesh getTerrain() { return terrain; }

    /**
     * Coloca el vértice (x = columna, y = fila) a una altura entera.
     * 0 = altura inicial; positivas y negativas permitidas.
     * @return false si la coordenada está fuera de la malla.
     */
    public boolean setVertexHeight(int x, int y, int height) {
        return terrain.setVertexHeight(x, y, height);
    }

    /** Altura actual del vértice (x = columna, y = fila), redondeada a entero. */
    public int getVertexHeight(int x, int y) {
        return Math.round(terrain.getHeight(x, y));
    }

    /** @return {columna, fila} del vértice seleccionado, o null si no hay selección. */
    public int[] getSelectedVertex() {
        if (!hasSelection || !terrain.inBounds(selX, selY)) return null;
        return new int[] { selX, selY };
    }

    public void clearSelection() { hasSelection = false; }

    /** Selecciona por codigo el vertice (x = columna, y = fila). @return false si esta fuera de la malla. */
    public boolean selectVertex(int x, int y) {
        if (!terrain.inBounds(x, y)) return false;
        selX = x;
        selY = y;
        hasSelection = true;
        return true;
    }

    /**
     * Convierte la posición actual del mouse en una coordenada de vértice.
     * @param radiusPixels distancia máxima (en píxeles) entre el mouse y el vértice
     * @return {columna, fila} o null si el mouse no está sobre el viewport
     *         o no hay ningún vértice dentro del radio.
     */
    public int[] pickVertex(float radiusPixels) {
        ImVec2 m = ImGui.getMousePos();
        return pickVertexAt(m.x, m.y, radiusPixels);
    }

    public boolean addVertexHeight(int x, int y, int delta) {
        return terrain.addVertexHeight(x, y, delta);
    }
    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    /**
 * Igual que pickVertex, pero con coordenadas de pantalla explícitas
 * (las mismas que ImGui.getMousePos()).
 */
public int[] pickVertexAt(float mouseX, float mouseY, float radiusPixels) {
    float lx = mouseX - viewX;
    float ly = mouseY - viewY;
    if (lx < 0 || ly < 0 || lx > viewW || ly > viewH) return null;

    float[] mvp = OrbitCamera.multiply(camera.projectionMatrix(aspect), camera.viewMatrix());

    int cells = terrain.getSubdivisions();
    int n = cells + 1;
    float size = terrain.getPlaneSize();
    float step = size / cells;
    float half = size / 2f;

    float bestD2 = radiusPixels * radiusPixels;
    int bestX = -1, bestY = -1;

    for (int iz = 0; iz < n; iz++) {
        float z = -half + iz * step;
        for (int ix = 0; ix < n; ix++) {
            float x = -half + ix * step;
            float y = terrain.getHeight(ix, iz);

            float cw = mvp[3] * x + mvp[7] * y + mvp[11] * z + mvp[15];
            if (cw <= 0f) continue; // detrás de la cámara
            float cx = mvp[0] * x + mvp[4] * y + mvp[8]  * z + mvp[12];
            float cy = mvp[1] * x + mvp[5] * y + mvp[9]  * z + mvp[13];

            float sx = (cx / cw * 0.5f + 0.5f) * viewW;
            float sy = (0.5f - cy / cw * 0.5f) * viewH; // y de pantalla va hacia abajo

            float dx = sx - lx, dy = sy - ly;
            float d2 = dx * dx + dy * dy;
            if (d2 < bestD2) {
                bestD2 = d2;
                bestX = ix;
                bestY = iz;
            }
        }
    }
    return bestX < 0 ? null : new int[] { bestX, bestY };
}


    private void handleInput(boolean hovered, int viewportHeight) {
        ImVec2 mouse = ImGui.getMousePos();
        float mx = mouse.x, my = mouse.y;

        // Mouse drag (middle button). The drag keeps going if the cursor leaves the viewport.
        if (hovered && ImGui.isMouseClicked(2)) {
            dragging = true;
        } else if (dragging && !ImGui.isMouseDown(2)) {
            dragging = false;
        } else if (dragging) {
            float dx = mx - lastMouseX;
            float dy = my - lastMouseY;
            boolean shift = isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
            boolean ctrl = isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
            if (shift) {
                camera.pan(dx, dy, viewportHeight);
            } else if (ctrl) {
                camera.zoom(1f + dy * 0.01f);
            } else {
                camera.orbit(dx, dy);
            }
        }
        lastMouseX = mx;
        lastMouseY = my;

        // Wheel zoom
        if (hovered) {
            float wheel = ImGui.getIO().getMouseWheel();
            if (wheel != 0f) {
                camera.zoom((float) Math.pow(0.9, wheel));
            }
        }

        // Numpad shortcuts (edge-triggered). State is tracked even when not
        // hovered, so holding the key and moving in doesn't trigger it.
        boolean k0 = isKeyDown(GLFW.GLFW_KEY_KP_0);
        boolean k1 = isKeyDown(GLFW.GLFW_KEY_KP_1);
        if (hovered && k0 && !numpad0WasDown) viewFront();
        if (hovered && k1 && !numpad1WasDown) viewTop();
        numpad0WasDown = k0;
        numpad1WasDown = k1;
    }

    private boolean isKeyDown(int glfwKey) {
        return GLFW.glfwGetKey(window, glfwKey) == GLFW.GLFW_PRESS;
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void renderScene() {
        // Save the GL state we touch, so ImGui's backend is not affected.
        int prevFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
        int prevProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int[] prevViewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, prevViewport);
        boolean prevScissor = glIsEnabled(GL_SCISSOR_TEST);
        boolean prevBlend = glIsEnabled(GL_BLEND);
        boolean prevDepth = glIsEnabled(GL_DEPTH_TEST);

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, fbWidth, fbHeight);
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        glClearColor(0.22f, 0.22f, 0.24f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glUseProgram(program);
        float[] mvp = OrbitCamera.multiply(camera.projectionMatrix(aspect), camera.viewMatrix());
        glUniformMatrix4fv(uMvp, false, mvp);

        // Solid pass (pushed slightly back so the wireframe doesn't z-fight)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1f, 1f);
        glUniform4f(uColor, 0.62f, 0.66f, 0.72f, 1f);
        glUniform1i(uLit, 1);
        terrain.draw();
        glDisable(GL_POLYGON_OFFSET_FILL);

        // Wireframe pass
        if (showWireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glUniform4f(uColor, 0.08f, 0.08f, 0.10f, 1f);
            glUniform1i(uLit, 0);
            terrain.draw();
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }

        // Restore state
        glUseProgram(prevProgram);
        glBindVertexArray(prevVao);
        glBindFramebuffer(GL_FRAMEBUFFER, prevFbo);
        glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        setEnabled(GL_SCISSOR_TEST, prevScissor);
        setEnabled(GL_BLEND, prevBlend);
        setEnabled(GL_DEPTH_TEST, prevDepth);

        if (hasSelection && terrain.inBounds(selX, selY)) {
            float[] v = {
                terrain.worldX(selX), terrain.getHeight(selX, selY), terrain.worldZ(selY),
                0f, 1f, 0f
            };
            glBindVertexArray(selVao);
            glBindBuffer(GL_ARRAY_BUFFER, selVbo);
            glBufferData(GL_ARRAY_BUFFER, v, GL_DYNAMIC_DRAW);
            glDisable(GL_DEPTH_TEST);
            glPointSize(10f);
            glUniform4f(uColor, 1f, 0.6f, 0.1f, 1f);
            glUniform1i(uLit, 0);
            glDrawArrays(GL_POINTS, 0, 1);
        }

    }

    private void resizeFramebuffer(int w, int h) {
        if (fbo != 0 && w == fbWidth && h == fbHeight) return;
        fbWidth = w;
        fbHeight = h;

        int prevFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
        int prevTex = glGetInteger(GL_TEXTURE_BINDING_2D);

        if (fbo == 0) {
            fbo = glGenFramebuffers();
            colorTex = glGenTextures();
            depthRbo = glGenRenderbuffers();
        }

        glBindTexture(GL_TEXTURE_2D, colorTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindRenderbuffer(GL_RENDERBUFFER, depthRbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, w, h);

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRbo);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Viewport3D framebuffer is not complete");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, prevFbo);
        glBindTexture(GL_TEXTURE_2D, prevTex);
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Shader compile error: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private static void setEnabled(int cap, boolean enabled) {
        if (enabled) glEnable(cap);
        else glDisable(cap);
    }

    /** Posición en pantalla (coordenadas ImGui absolutas) del vértice, o null si está detrás de la cámara. */
private float[] projectVertex(int ix, int iz, float[] mvp) {
    float x = terrain.worldX(ix);
    float y = terrain.getHeight(ix, iz);
    float z = terrain.worldZ(iz);

    float cw = mvp[3] * x + mvp[7] * y + mvp[11] * z + mvp[15];
    if (cw <= 0f) return null;
    float cx = mvp[0] * x + mvp[4] * y + mvp[8]  * z + mvp[12];
    float cy = mvp[1] * x + mvp[5] * y + mvp[9]  * z + mvp[13];

    return new float[] {
        viewX + (cx / cw * 0.5f + 0.5f) * viewW,
        viewY + (0.5f - cy / cw * 0.5f) * viewH
    };
}

private void drawSelectionOverlay(boolean hovered) {
    ImDrawList dl = ImGui.getWindowDrawList();
    float[] mvp = OrbitCamera.multiply(camera.projectionMatrix(aspect), camera.viewMatrix());

    // Vista previa: vértice bajo el mouse (amarillo, hueco)
    if (hovered) {
        int[] h = pickVertex(PICK_RADIUS);
        if (h != null) {
            float[] p = projectVertex(h[0], h[1], mvp);
            if (p != null) {
                dl.addCircle(p[0], p[1], 7f, ImGui.getColorU32(1f, 1f, 0.2f, 1f), 16, 2f);
                dl.addText(p[0] + 10, p[1] - 18, ImGui.getColorU32(1f, 1f, 0.6f, 1f),
                        "(" + h[0] + ", " + h[1] + ")");
            }
            if (ImGui.isMouseClicked(0)) {   // <-- clic = seleccionar
                selX = h[0];
                selY = h[1];
                hasSelection = true;
            }
        }
    }

    // Seleccionado (naranja, relleno)
    if (hasSelection && terrain.inBounds(selX, selY)) {
        float[] p = projectVertex(selX, selY, mvp);
        if (p != null) {
            dl.addCircleFilled(p[0], p[1], 6f, ImGui.getColorU32(1f, 0.6f, 0.1f, 1f), 16);
            dl.addCircle(p[0], p[1], 7f, ImGui.getColorU32(0f, 0f, 0f, 1f), 16, 2f);
        }
    }
}
}