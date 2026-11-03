package com.automatas5.Views;


import static org.lwjgl.opengl.GL30.*;

/**
 * Subdividable plane stored as a heightmap.
 *
 * - The plane lies on the XZ plane, centered at the origin, and spans
 *   [-planeSize/2, planeSize/2] on both axes. Height is the Y coordinate.
 * - "cells" is the number of subdivisions per side, so there are
 *   (cells + 1) x (cells + 1) vertices. Vertex indices (ix, iz) go from 0 to cells.
 * - Edits (setHeight / addHeight) only touch CPU data and mark the mesh as dirty;
 *   the GPU buffer is updated once per frame in draw(), so many edits per frame are cheap.
 *
 * GL calls (initGL, draw, dispose) must run on the thread that owns the GL context.
 */
public class TerrainMesh {

    public static final int MIN_CELLS = 1;
    public static final int MAX_CELLS = 512;

    private int cells;
    private float planeSize;
    private float[] heights;   // row-major: index = iz * (cells + 1) + ix
    private float[] vertices;  // interleaved: x, y, z, nx, ny, nz

    private int vao, vbo, ebo;
    private int indexCount;
    private boolean initialized;
    private boolean heightsDirty = true;
    private boolean topologyDirty = true;

    public TerrainMesh(int cells, float planeSize) {
        if (planeSize <= 0f) throw new IllegalArgumentException("planeSize must be > 0");
        this.cells = clampCells(cells);
        this.planeSize = planeSize;
        int n = this.cells + 1;
        this.heights = new float[n * n];
    }

    // ------------------------------------------------------------------
    // Subdivision API
    // ------------------------------------------------------------------

    /** Doubles the subdivisions (e.g. 16 -> 32). Existing heights are preserved. */
    public void increaseSubdivisions(int newSize) {
        setSubdivisions(newSize);
    }

    /** Halves the subdivisions (e.g. 32 -> 16), minimum 1. Heights are resampled. */
    public void decreaseSubdivisions(int newSize) {
        setSubdivisions(newSize);
    }

    /**
     * Sets an exact number of subdivisions per side (clamped to [MIN_CELLS, MAX_CELLS]).
     * Existing heights are bilinearly resampled to the new grid, so the sculpt is kept.
     */
    public void setSubdivisions(int newCells) {
        newCells = clampCells(newCells);
        if (newCells == cells) return;

        int oldN = cells + 1;
        int newN = newCells + 1;
        float[] resampled = new float[newN * newN];

        for (int iz = 0; iz < newN; iz++) {
            for (int ix = 0; ix < newN; ix++) {
                float gx = (float) ix / newCells * cells;
                float gz = (float) iz / newCells * cells;
                resampled[iz * newN + ix] = sample(heights, oldN, gx, gz);
            }
        }

        this.heights = resampled;
        this.cells = newCells;
        this.topologyDirty = true;
        this.heightsDirty = true;
    }

    /** Changes the world-space size of the plane (it stays centered at the origin). */
    public void setPlaneSize(float newSize) {
        if (newSize <= 0f) throw new IllegalArgumentException("planeSize must be > 0");
        this.planeSize = newSize;
        this.heightsDirty = true;
    }

    public int getSubdivisions() { return cells; }

    public float getPlaneSize() { return planeSize; }

    // ------------------------------------------------------------------
    // Terrain edit API (hook for the sculpting tools)
    // ------------------------------------------------------------------

    /** Sets the height of vertex (ix, iz). Out-of-range indices are ignored. */
    public void setHeight(int ix, int iz, float height) {
        if (!inBounds(ix, iz)) return;
        heights[iz * (cells + 1) + ix] = height;
        heightsDirty = true;
    }

    /** Adds delta to the height of vertex (ix, iz). Out-of-range indices are ignored. */
    public void addHeight(int ix, int iz, float delta) {
        if (!inBounds(ix, iz)) return;
        heights[iz * (cells + 1) + ix] += delta;
        heightsDirty = true;
    }

    /** Returns the height of vertex (ix, iz), or 0 if out of range. */
    public float getHeight(int ix, int iz) {
        if (!inBounds(ix, iz)) return 0f;
        return heights[iz * (cells + 1) + ix];
    }

    public boolean inBounds(int ix, int iz) {
        return ix >= 0 && ix <= cells && iz >= 0 && iz <= cells;
    }

    /** World X coordinate of vertex column ix. */
    public float worldX(int ix) {
        return -planeSize / 2f + ix * (planeSize / cells);
    }

    /** World Z coordinate of vertex row iz. */
    public float worldZ(int iz) {
        return -planeSize / 2f + iz * (planeSize / cells);
    }

    // ------------------------------------------------------------------
    // GL lifecycle
    // ------------------------------------------------------------------

    public void initGL() {
        if (initialized) return;
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        int stride = 6 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);

        glBindVertexArray(0);
        initialized = true;
        topologyDirty = true;
        heightsDirty = true;
    }

    /** Uploads pending changes (if any) and draws the mesh with the current program. */
    public void draw() {
        if (!initialized) initGL();
        syncToGpu();
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public void dispose() {
        if (!initialized) return;
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        initialized = false;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private void syncToGpu() {
        if (!topologyDirty && !heightsDirty) return;

        rebuildVertices();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        if (topologyDirty) {
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
            int[] indices = buildIndices();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
            indexCount = indices.length;
        } else {
            glBufferSubData(GL_ARRAY_BUFFER, 0L, vertices);
        }
        glBindVertexArray(0);

        topologyDirty = false;
        heightsDirty = false;
    }

    /** Recomputes positions and normals (central differences) from the heightmap. */
    private void rebuildVertices() {
        int n = cells + 1;
        if (vertices == null || vertices.length != n * n * 6) {
            vertices = new float[n * n * 6];
        }
        float step = planeSize / cells;
        float half = planeSize / 2f;

        for (int iz = 0; iz < n; iz++) {
            int z0 = Math.max(iz - 1, 0);
            int z1 = Math.min(iz + 1, cells);
            for (int ix = 0; ix < n; ix++) {
                int x0 = Math.max(ix - 1, 0);
                int x1 = Math.min(ix + 1, cells);

                float slopeX = (heights[iz * n + x0] - heights[iz * n + x1]) / ((x1 - x0) * step);
                float slopeZ = (heights[z0 * n + ix] - heights[z1 * n + ix]) / ((z1 - z0) * step);
                float len = (float) Math.sqrt(slopeX * slopeX + 1f + slopeZ * slopeZ);

                int o = (iz * n + ix) * 6;
                vertices[o] = -half + ix * step;
                vertices[o + 1] = heights[iz * n + ix];
                vertices[o + 2] = -half + iz * step;
                vertices[o + 3] = slopeX / len;
                vertices[o + 4] = 1f / len;
                vertices[o + 5] = slopeZ / len;
            }
        }
    }

    private int[] buildIndices() {
        int n = cells + 1;
        int[] indices = new int[cells * cells * 6];
        int k = 0;
        for (int iz = 0; iz < cells; iz++) {
            for (int ix = 0; ix < cells; ix++) {
                int a = iz * n + ix;
                int b = a + 1;
                int c = a + n;
                int d = c + 1;
                indices[k++] = a; indices[k++] = c; indices[k++] = b;
                indices[k++] = b; indices[k++] = c; indices[k++] = d;
            }
        }
        return indices;
    }

    /** Bilinear sample of an n x n grid at fractional grid coordinates. */
    private static float sample(float[] h, int n, float gx, float gz) {
        int x0 = Math.min((int) gx, n - 2);
        int z0 = Math.min((int) gz, n - 2);
        float fx = gx - x0;
        float fz = gz - z0;

        float h00 = h[z0 * n + x0];
        float h10 = h[z0 * n + x0 + 1];
        float h01 = h[(z0 + 1) * n + x0];
        float h11 = h[(z0 + 1) * n + x0 + 1];

        float top = h00 + (h10 - h00) * fx;
        float bottom = h01 + (h11 - h01) * fx;
        return top + (bottom - top) * fz;
    }

    private static int clampCells(int c) {
        return Math.max(MIN_CELLS, Math.min(MAX_CELLS, c));
    }

    /**
     * Coloca el vértice (x = columna, y = fila) a la altura indicada.
     * 0 es la altura inicial; se aceptan valores positivos y negativos.
     * @return true si el vértice existe y fue modificado, false si está fuera de rango.
     */
    public boolean setVertexHeight(int x, int y, int height) {
        if (!inBounds(x, y)) return false;
        heights[y * (cells + 1) + x] = height;
        heightsDirty = true;   // la GPU se actualiza en el siguiente draw()
        return true;
    }

    public boolean addVertexHeight(int x, int y, int delta) {
    if (!inBounds(x, y)) return false;
    heights[y * (cells + 1) + x] += delta;
    heightsDirty = true;
    return true;
}
}
