package io.github.laborima.gridviewer.data;

import java.util.Locale;
import java.util.Set;

/** Recognises longitude and latitude variables by their CF units or their usual names. */
final class Coordinates {

    private static final Set<String> EAST = Set.of(
            "degrees_east", "degree_east", "degree_e", "degrees_e", "degreee", "degreese");
    private static final Set<String> NORTH = Set.of(
            "degrees_north", "degree_north", "degree_n", "degrees_n", "degreen", "degreesn");

    private Coordinates() {
    }

    static boolean isLongitude(String name, String units) {
        String n = name.toLowerCase(Locale.ROOT);
        return EAST.contains(units.toLowerCase(Locale.ROOT))
                || n.startsWith("lon") || n.equals("nav_lon") || n.startsWith("glam");
    }

    static boolean isLatitude(String name, String units) {
        String n = name.toLowerCase(Locale.ROOT);
        return NORTH.contains(units.toLowerCase(Locale.ROOT))
                || n.startsWith("lat") || n.equals("nav_lat") || n.startsWith("gphi");
    }

    static boolean isTime(String name, String units) {
        String n = name.toLowerCase(Locale.ROOT);
        return units.toLowerCase(Locale.ROOT).contains(" since ") || n.contains("time") || n.equals("t");
    }
}
