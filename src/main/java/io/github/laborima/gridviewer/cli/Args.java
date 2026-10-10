package io.github.laborima.gridviewer.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimal command line parser: {@code --name value}, {@code -n value}, flags and positionals.
 */
public final class Args {

    private final Map<String, String> options = new HashMap<>();
    private final List<String> positionals = new ArrayList<>();

    /**
     * @param flags options that take no value, without dashes
     * @param aliases short option names mapped to long ones, without dashes
     */
    public Args(String[] argv, Set<String> flags, Map<String, String> aliases) {
        for (int k = 0; k < argv.length; k++) {
            String a = argv[k];
            if (a.startsWith("-") && a.length() > 1 && !isNumber(a)) {
                String name = a.replaceFirst("^--?", "");
                String value = null;
                int eq = name.indexOf('=');
                if (eq >= 0) {
                    value = name.substring(eq + 1);
                    name = name.substring(0, eq);
                }
                name = aliases.getOrDefault(name, name);
                if (flags.contains(name)) {
                    options.put(name, "true");
                } else {
                    if (value == null) {
                        if (k + 1 >= argv.length) {
                            throw new IllegalArgumentException("Missing value for " + a);
                        }
                        value = argv[++k];
                    }
                    options.put(name, value);
                }
            } else {
                positionals.add(a);
            }
        }
    }

    public List<String> positionals() {
        return positionals;
    }

    public boolean has(String name) {
        return options.containsKey(name);
    }

    public String get(String name, String fallback) {
        return options.getOrDefault(name, fallback);
    }

    public int getInt(String name, int fallback) {
        String v = options.get(name);
        try {
            return v == null ? fallback : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--" + name + " expects an integer, got " + v, e);
        }
    }

    private static boolean isNumber(String s) {
        return s.matches("-\\d+(\\.\\d*)?([eE][-+]?\\d+)?(:.*)?");
    }
}
