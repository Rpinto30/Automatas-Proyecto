package com.automatas5.Views;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiButtonFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30.*;

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

        terrain.initGL();
        initialized = true;
    }

    /** Releases all GPU resources. */
    public void dispose() {
        if (!initialized) return;
        terrain.dispose();
        glDeleteProgram(program);
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
        ImGui.image(colorTex, w, h, 0f, 1f, 1f, 0f);
        ImGui.setCursorScreenPos(px, py);
        ImGui.invisibleButton("##viewport3d", w, h,
            ImGuiButtonFlags.MouseButtonLeft | ImGuiButtonFlags.MouseButtonRight | ImGuiButtonFlags.MouseButtonMiddle);
        boolean hovered = ImGui.isItemHovered();

        handleInput(hovered, h);
        renderScene();
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

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

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
}
