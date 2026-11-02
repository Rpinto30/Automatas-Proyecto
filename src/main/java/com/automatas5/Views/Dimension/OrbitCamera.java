package com.automatas5.Views.Dimension;


/**
 * Orbit camera similar to Blender's turntable navigation.
 *
 * Conventions: Y axis is up, the terrain lies on the XZ plane.
 *  - yaw   = rotation around the Y axis
 *  - pitch = elevation angle (0 = horizon, PI/2 = looking straight down)
 *
 * All matrices are column-major float[16], ready for glUniformMatrix4fv.
 */
public class OrbitCamera {

    private static final float FOV = (float) Math.toRadians(45.0);
    private static final float NEAR = 0.05f;
    private static final float FAR = 5000f;
    private static final float HALF_PI = (float) (Math.PI / 2.0);
    private static final float MIN_DISTANCE = 0.1f;
    private static final float MAX_DISTANCE = 4000f;

    private float yaw = 0.6f;
    private float pitch = 0.5f;
    private float distance = 15f;
    private float targetX, targetY, targetZ;

    // ------------------------------------------------------------------
    // Navigation (Blender style)
    // ------------------------------------------------------------------

    /** Middle mouse drag. dx, dy = mouse delta in pixels. */
    public void orbit(float dx, float dy) {
        yaw -= dx * 0.01f;
        pitch = clamp(pitch + dy * 0.01f, -HALF_PI, HALF_PI);
    }

    /** Shift + middle mouse drag. */
    public void pan(float dx, float dy, int viewportHeight) {
        float[] b = basis();
        float worldPerPixel = 2f * distance * (float) Math.tan(FOV / 2f) / Math.max(1, viewportHeight);
        targetX += (-dx * b[0] + dy * b[3]) * worldPerPixel;
        targetY += (-dx * b[1] + dy * b[4]) * worldPerPixel;
        targetZ += (-dx * b[2] + dy * b[5]) * worldPerPixel;
    }

    /** Mouse wheel / Ctrl + middle mouse drag. factor < 1 zooms in, > 1 zooms out. */
    public void zoom(float factor) {
        distance = clamp(distance * factor, MIN_DISTANCE, MAX_DISTANCE);
    }

    // ------------------------------------------------------------------
    // Preset views
    // ------------------------------------------------------------------

    /** Front view: looking along -Z. The XZ plane is seen edge-on. */
    public void viewFront(float planeSize, float aspect) {
        yaw = 0f;
        pitch = 0f;
        frame(planeSize, aspect);
    }

    /** Top view: looking straight down. X goes right, -Z goes up on screen. */
    public void viewTop(float planeSize, float aspect) {
        yaw = 0f;
        pitch = HALF_PI;
        frame(planeSize, aspect);
    }

    /** Centers the target on the plane and picks a distance so it fits on screen. */
    public void frame(float planeSize, float aspect) {
        targetX = 0f;
        targetY = 0f;
        targetZ = 0f;
        float fit = (planeSize * 0.5f) / (float) Math.tan(FOV / 2f);
        distance = clamp(fit / Math.min(1f, aspect) * 1.2f, MIN_DISTANCE, MAX_DISTANCE);
    }

    // ------------------------------------------------------------------
    // Matrices
    // ------------------------------------------------------------------

    public float[] viewMatrix() {
        float[] b = basis();
        float rx = b[0], ry = b[1], rz = b[2];
        float ux = b[3], uy = b[4], uz = b[5];
        float dx = b[6], dy = b[7], dz = b[8]; // target -> eye

        float ex = targetX + dx * distance;
        float ey = targetY + dy * distance;
        float ez = targetZ + dz * distance;

        float[] m = new float[16];
        m[0] = rx; m[4] = ry; m[8] = rz;
        m[1] = ux; m[5] = uy; m[9] = uz;
        m[2] = dx; m[6] = dy; m[10] = dz;
        m[12] = -(rx * ex + ry * ey + rz * ez);
        m[13] = -(ux * ex + uy * ey + uz * ez);
        m[14] = -(dx * ex + dy * ey + dz * ez);
        m[15] = 1f;
        return m;
    }

    public float[] projectionMatrix(float aspect) {
        float f = 1f / (float) Math.tan(FOV / 2f);
        float[] m = new float[16];
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (FAR + NEAR) / (NEAR - FAR);
        m[11] = -1f;
        m[14] = (2f * FAR * NEAR) / (NEAR - FAR);
        return m;
    }

    /** Column-major product a * b. */
    public static float[] multiply(float[] a, float[] b) {
        float[] out = new float[16];
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                float sum = 0f;
                for (int k = 0; k < 4; k++) {
                    sum += a[k * 4 + r] * b[c * 4 + k];
                }
                out[c * 4 + r] = sum;
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /**
     * Returns {right(3), up(3), dirToEye(3)}.
     * The up vector is computed from yaw/pitch (not from a fixed world up),
     * so the top view (pitch = 90 degrees) is not degenerate.
     */
    private float[] basis() {
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        float cyaw = (float) Math.cos(yaw), syaw = (float) Math.sin(yaw);

        float dx = cp * syaw, dy = sp, dz = cp * cyaw;     // target -> eye
        float ux = -sp * syaw, uy = cp, uz = -sp * cyaw;   // camera up
        float fx = -dx, fy = -dy, fz = -dz;                // forward

        float rx = fy * uz - fz * uy;                      // right = forward x up
        float ry = fz * ux - fx * uz;
        float rz = fx * uy - fy * ux;

        return new float[] { rx, ry, rz, ux, uy, uz, dx, dy, dz };
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
