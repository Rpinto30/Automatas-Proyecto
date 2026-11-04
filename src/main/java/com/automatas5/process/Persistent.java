package com.automatas5.process;

import com.automatas5.Views.Dimension.TerrainMesh;
import com.automatas5.Views.GraphView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Persistencia del proyecto.
 *
 * 1) init.graph (como imgui.ini): configuracion del automata. Se CARGA al iniciar y se GUARDA al cerrar.
 *    Es texto plano, una instruccion por linea:
 *
 *        # comentario
 *        version 1
 *        node q0 58 120.0 80.0 ; ':'      -> nombre, simbolo (codigo Unicode), x, y en el canvas
 *        edge q0 q1                       -> conexion entre dos nodos
 *
 *    El simbolo va como numero porque ' ' y '#' romperian un formato legible; lo que sigue a los
 *    primeros 5 campos de un "node" se ignora (es solo ayuda para humanos).
 *
 * 2) Exportacion del terreno a Wavefront .obj (vertices + normales + triangulos).
 */
public final class Persistent {

    /** Se busca en el directorio de trabajo, igual que imgui.ini. */
    public static final Path GRAPH_FILE = Paths.get("init.graph");
    public static final Path EXPORT_DIR = Paths.get("exports");

    private static final int FORMAT_VERSION = 1;

    private Persistent() { }

    // ------------------------------------------------------------------
    // init.graph
    // ------------------------------------------------------------------

    /** Carga init.graph en la vista. @return true si el archivo existia y se leyo. */
    public static boolean loadGraph(GraphView view) {
        return loadGraph(view, GRAPH_FILE);
    }

    public static boolean loadGraph(GraphView view, Path file) {
        if (!Files.isRegularFile(file)) return false; // primera ejecucion: se arranca vacio

        List<GraphView.NodeState> nodes = new ArrayList<>();
        List<String[]> edges = new ArrayList<>();

        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = in.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] t = line.split("\\s+");
                try {
                    switch (t[0]) {
                        case "node" -> nodes.add(new GraphView.NodeState(
                                t[1],
                                (char) Integer.parseInt(t[2]),
                                Float.parseFloat(t[3]),
                                Float.parseFloat(t[4])));
                        case "edge" -> edges.add(new String[] { t[1], t[2] });
                        default -> { /* "version" y lineas desconocidas se ignoran */ }
                    }
                } catch (RuntimeException ex) {
                    System.err.println("[Persistent] " + file + ":" + lineNo + " linea invalida, se ignora: " + line);
                }
            }
        } catch (IOException ex) {
            System.err.println("[Persistent] No se pudo leer " + file + ": " + ex);
            return false;
        }

        int loaded = view.loadState(nodes, edges);
        System.out.println("[Persistent] " + file + " cargado: " + loaded + " nodos, " + edges.size() + " aristas");
        return true;
    }

    /** Archivos .graph de la carpeta de trabajo y de graphs/ (si existe), ordenados por nombre. */
    public static List<Path> listGraphFiles() {
        List<Path> out = new ArrayList<>();
        Path cwd = Paths.get(".");
        for (Path dir : new Path[] { cwd, Paths.get("graphs") }) {
            if (!Files.isDirectory(dir)) continue;
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.graph")) {
                for (Path p : ds) out.add(dir.equals(cwd) ? p.getFileName() : p);
            } catch (IOException ex) {
                System.err.println("[Persistent] No se pudo listar " + dir + ": " + ex);
            }
        }
        Collections.sort(out);
        return out;
    }

    /** Guarda el automata en init.graph. @return true si se escribio. */
    public static boolean saveGraph(GraphView view) {
        return saveGraph(view, GRAPH_FILE);
    }

    public static boolean saveGraph(GraphView view, Path file) {
        Path tmp = Paths.get(file.toString() + ".tmp");
        try {
            try (BufferedWriter out = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                out.write("# init.graph - configuracion del automata (se carga al iniciar, se guarda al cerrar)\n");
                out.write("version " + FORMAT_VERSION + "\n");
                for (GraphView.NodeState n : view.getNodeStates()) {
                    String hint = (n.value == ' ') ? "(espacio)" : "'" + n.value + "'";
                    out.write(String.format(Locale.ROOT, "node %s %d %s %s ; %s%n",
                            n.name, (int) n.value, Float.toString(n.x), Float.toString(n.y), hint));
                }
                for (String[] e : view.getEdges()) {
                    out.write("edge " + e[0] + " " + e[1] + "\n");
                }
            }
            // se escribe a un temporal y se renombra: un cierre a medias no deja el archivo corrupto
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[Persistent] " + file + " guardado");
            return true;
        } catch (IOException ex) {
            System.err.println("[Persistent] No se pudo guardar " + file + ": " + ex);
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Exportacion .obj
    // ------------------------------------------------------------------

    /** Exporta a exports/terreno_yyyyMMdd_HHmmss.obj y devuelve la ruta creada. */
    public static Path exportObj(TerrainMesh terrain) throws IOException {
        Files.createDirectories(EXPORT_DIR);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path file = EXPORT_DIR.resolve("terreno_" + stamp + ".obj");
        exportObj(terrain, file);
        return file;
    }

    /**
     * Escribe la malla tal como se ve en el viewer: X/Z = worldX/worldZ, Y = altura del vertice.
     * Triangulos en sentido antihorario visto desde arriba (+Y), con normales suaves por vertice.
     */
    public static void exportObj(TerrainMesh terrain, Path file) throws IOException {
        final int cells = terrain.getSubdivisions();
        final int n = cells + 1;
        final float step = terrain.getPlaneSize() / cells;

        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("# Automatas - terreno exportado\n");
            out.write("# celdas: " + cells + "x" + cells + ", vertices: " + n * n + "\n");
            out.write("o terreno\n");

            // vertices (indice OBJ = iz * n + ix + 1)
            for (int iz = 0; iz < n; iz++) {
                for (int ix = 0; ix < n; ix++) {
                    out.write(String.format(Locale.ROOT, "v %.4f %.4f %.4f\n",
                            terrain.worldX(ix), terrain.getHeight(ix, iz), terrain.worldZ(iz)));
                }
            }

            // normales por diferencias centrales (en los bordes, diferencia unilateral)
            for (int iz = 0; iz < n; iz++) {
                for (int ix = 0; ix < n; ix++) {
                    int xl = Math.max(ix - 1, 0), xr = Math.min(ix + 1, cells);
                    int zl = Math.max(iz - 1, 0), zr = Math.min(iz + 1, cells);
                    float dhdx = (terrain.getHeight(xr, iz) - terrain.getHeight(xl, iz)) / ((xr - xl) * step);
                    float dhdz = (terrain.getHeight(ix, zr) - terrain.getHeight(ix, zl)) / ((zr - zl) * step);
                    float nx = -dhdx, ny = 1f, nz = -dhdz;
                    float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    out.write(String.format(Locale.ROOT, "vn %.4f %.4f %.4f\n", nx / len, ny / len, nz / len));
                }
            }

            // caras: dos triangulos por celda
            for (int iz = 0; iz < cells; iz++) {
                for (int ix = 0; ix < cells; ix++) {
                    int a = iz * n + ix + 1;   // (ix,   iz)
                    int b = a + 1;             // (ix+1, iz)
                    int d = a + n;             // (ix,   iz+1)
                    int c = d + 1;             // (ix+1, iz+1)
                    out.write("f " + a + "//" + a + " " + c + "//" + c + " " + b + "//" + b + "\n");
                    out.write("f " + a + "//" + a + " " + d + "//" + d + " " + c + "//" + c + "\n");
                }
            }
        }
    }
}