package io.github.laborima.gridviewer;

import java.io.PrintStream;
import java.util.Arrays;

import io.github.laborima.gridviewer.cli.InfoCommand;
import io.github.laborima.gridviewer.cli.RenderCommand;
import io.github.laborima.gridviewer.ui.GridViewerApp;

/**
 * Entry point. Without a sub-command it opens the viewer; {@code info}, {@code render} and
 * {@code palettes} run headless.
 */
public final class Main {

    public static final String VERSION = versionString();

    private static final String HELP = """
            Grid Viewer %s - fast viewer for large NetCDF ocean and climate grids

            Usage:
              gridviewer [FILE|URL] [--var NAME]   open the viewer
              gridviewer info FILE|URL             list the gridded variables
              gridviewer render FILE ... -o PNG    draw a map without a window (render --help)
              gridviewer palettes                  list the colour maps
              gridviewer --version
            """;

    private Main() {
    }

    public static void main(String[] args) {
        PrintStream out = System.out;
        String command = args.length == 0 ? "" : args[0];
        String[] rest = args.length == 0 ? args : Arrays.copyOfRange(args, 1, args.length);
        try {
            int code = switch (command) {
                case "info" -> InfoCommand.info(rest, out);
                case "render" -> {
                    System.setProperty("java.awt.headless", "true");
                    yield RenderCommand.run(rest, out);
                }
                case "palettes" -> InfoCommand.palettes(out);
                case "-h", "--help", "help" -> {
                    out.printf(HELP, VERSION);
                    yield 0;
                }
                case "-V", "--version", "version" -> {
                    out.println("gridviewer " + VERSION);
                    yield 0;
                }
                default -> {
                    GridViewerApp.launchWith(args);
                    yield 0;
                }
            };
            if (code != 0) {
                System.exit(code);
            }
        } catch (Exception e) {
            System.err.println("gridviewer: " + rootMessage(e));
            System.exit(1);
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }

    private static String versionString() {
        String v = Main.class.getPackage().getImplementationVersion();
        return v == null ? "dev" : v;
    }
}
