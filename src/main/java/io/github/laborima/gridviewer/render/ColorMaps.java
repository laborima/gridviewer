package io.github.laborima.gridviewer.render;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * The bundled colour maps: cmocean (designed for oceanography), perceptual matplotlib maps and
 * the original 2006 Grid Viewer palettes. Also picks a sensible default per variable.
 */
public final class ColorMaps {

    private static final String BASE = "/io/github/laborima/gridviewer/";
    private static final Map<String, ColorMap> ALL = load();
    private static final Properties RANGES = loadRanges();

    private ColorMaps() {
    }

    public static List<ColorMap> all() {
        return List.copyOf(ALL.values());
    }

    /** Colour map by name (case-insensitive); falls back to {@code thermal}. */
    public static ColorMap get(String name) {
        ColorMap m = name == null ? null : ALL.get(name.toLowerCase(Locale.ROOT));
        return m != null ? m : ALL.get("thermal");
    }

    /** Guesses a colour map from the variable name, standard name and units. */
    public static ColorMap suggest(String name, String longName, String units) {
        String s = (name + " " + longName).toLowerCase(Locale.ROOT);
        String u = units.toLowerCase(Locale.ROOT);
        if (s.contains("anomal") || s.contains("ssh") || s.contains("sea_surface_height") || s.contains("sossheig")
                || s.contains("sla") || s.contains("vozocrtx") || s.contains("vomecrty")
                || s.contains("velocity") || s.contains("tau") || s.contains("flux")) {
            return get("balance");
        }
        if (s.contains("sal") || s.contains("vosaline") || u.equals("psu") || u.equals("1e-3")) {
            return get("haline");
        }
        if (s.contains("temp") || s.contains("sst") || s.contains("votemper") || u.contains("celsius")
                || u.equals("degc") || u.equals("°c") || u.equals("k") || u.equals("kelvin")) {
            return get("thermal");
        }
        if (s.contains("speed") || s.contains("current") || s.contains("eke")) {
            return get("speed");
        }
        if (s.contains("chl") || s.contains("chlorophyll")) {
            return get("algae");
        }
        if (s.contains("ice") || s.contains("ileadfra") || s.contains("siconc")) {
            return get("ice");
        }
        if (s.contains("bathy") || s.contains("depth") || s.contains("elevation") || s.contains("topo")) {
            return get("deep");
        }
        if (s.contains("dens") || s.contains("sigma") || s.contains("rho")) {
            return get("dense");
        }
        return get("viridis");
    }

    /** True for diverging maps, whose automatic range is centred on zero. */
    public static boolean isDiverging(ColorMap map) {
        return switch (map.name().replace(" (reversed)", "")) {
            case "balance", "delta", "rdbu-r", "coolwarm", "legacy-blue-red", "legacy-blue-red-2", "legacy-sla" -> true;
            default -> false;
        };
    }

    /** Range from the 2006 defaults for NEMO variable names, or {@code null}. */
    public static double[] defaultRange(String variableName) {
        String r = RANGES.getProperty(variableName);
        if (r == null) {
            return null;
        }
        String[] p = r.split(":");
        return new double[] {Double.parseDouble(p[0]), Double.parseDouble(p[1])};
    }

    private static Map<String, ColorMap> load() {
        Map<String, ColorMap> maps = new LinkedHashMap<>();
        for (String name : readLines(BASE + "palettes/index.txt")) {
            String source = "";
            List<int[]> stops = new ArrayList<>();
            for (String line : readLines(BASE + "palettes/" + name + ".txt")) {
                if (line.startsWith("#")) {
                    source = line.substring(1).strip();
                    continue;
                }
                String[] t = line.strip().split("\\s+");
                stops.add(new int[] {Integer.parseInt(t[0]), Integer.parseInt(t[1]), Integer.parseInt(t[2])});
            }
            maps.put(name, ColorMap.fromStops(name, source, stops.toArray(int[][]::new)));
        }
        return maps;
    }

    private static Properties loadRanges() {
        Properties p = new Properties();
        try (InputStream in = ColorMaps.class.getResourceAsStream(BASE + "default-ranges.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return p;
    }

    private static List<String> readLines(String resource) {
        InputStream in = ColorMaps.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("Missing resource " + resource);
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return r.lines().filter(l -> !l.isBlank()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
