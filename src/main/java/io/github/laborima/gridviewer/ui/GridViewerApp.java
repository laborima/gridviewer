package io.github.laborima.gridviewer.ui;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import io.github.laborima.gridviewer.cli.Args;
import io.github.laborima.gridviewer.data.SectionRequest;
import io.github.laborima.gridviewer.data.Window;
import io.github.laborima.gridviewer.render.ColorMaps;
import io.github.laborima.gridviewer.render.ValueRange;

/**
 * The JavaFX application.
 *
 * <p>Besides a file or URL to open, it accepts options that set up the view, which is how the
 * README screenshots are produced: {@code --var}, {@code --level}, {@code --time},
 * {@code --palette}, {@code --range MIN:MAX}, {@code --window I0,J0,I1,J1},
 * {@code --section row:J|col:I}, {@code --tab map|section|globe}, {@code --globe-lon LON},
 * {@code --size WxH} and {@code --screenshot FILE.png} (capture the window, then quit).
 */
public final class GridViewerApp extends Application {

    private static final String ICON = "/io/github/laborima/gridviewer/icons/gridviewer-%d.png";

    public static void launchWith(String[] args) {
        Application.launch(GridViewerApp.class, args);
    }

    /** Application icon of the given size, or {@code null} when not bundled. */
    static Image icon(int size) {
        InputStream in = GridViewerApp.class.getResourceAsStream(ICON.formatted(size));
        return in == null ? null : new Image(in);
    }

    @Override
    public void start(Stage stage) {
        Args args = new Args(getParameters().getRaw().toArray(String[]::new), Set.of(),
                Map.of("v", "var", "z", "level", "t", "time", "p", "palette", "r", "range"));
        MainWindow window = new MainWindow(stage);
        String[] size = args.get("size", "1440x900").split("x");
        Scene scene = new Scene(window, Double.parseDouble(size[0]), Double.parseDouble(size[1]));
        scene.getStylesheets().add(GridViewerApp.class.getResource("/io/github/laborima/gridviewer/gridviewer.css")
                .toExternalForm());
        for (int s : new int[] {16, 32, 48, 64, 128, 256, 512}) {
            Image img = icon(s);
            if (img != null) {
                stage.getIcons().add(img);
            }
        }
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();

        if (!args.positionals().isEmpty()) {
            window.setOnFirstImage(w -> applyOptions(w, args, stage));
            window.open(args.positionals().getFirst(), args.get("var", null));
        }
    }

    @Override
    public void stop() {
        System.exit(0);
    }

    private static void applyOptions(MainWindow w, Args args, Stage stage) {
        Session s = w.session();
        if (args.has("palette")) {
            s.colorMapProperty().set(ColorMaps.get(args.get("palette", null)));
        }
        if (args.has("range")) {
            String[] r = args.get("range", "0:1").split(":");
            s.rangeProperty().set(new ValueRange(Double.parseDouble(r[0]), Double.parseDouble(r[1])));
        }
        s.levelProperty().set(args.getInt("level", 0));
        s.timeProperty().set(args.getInt("time", 0));
        if (args.has("window")) {
            String[] p = args.get("window", "").split(",");
            w.map().showWindow(new Window(Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                    Integer.parseInt(p[2]), Integer.parseInt(p[3])));
        }
        if (args.has("section")) {
            String[] p = args.get("section", "").split(":");
            var orientation = p[0].startsWith("row") ? SectionRequest.Orientation.ALONG_ROW
                    : SectionRequest.Orientation.ALONG_COLUMN;
            s.sectionProperty().set(new Session.SectionLine(orientation, Integer.parseInt(p[1])));
        }
        if (args.has("globe-lon")) {
            w.globe().faceLongitude(Double.parseDouble(args.get("globe-lon", "0")));
            w.globe().autoRotateProperty().set(false);
        }
        switch (args.get("tab", "map")) {
            case "section" -> w.tabs().getSelectionModel().select(w.sectionTab());
            case "globe" -> w.tabs().getSelectionModel().select(w.globeTab());
            default -> w.tabs().getSelectionModel().select(w.mapTab());
        }
        if (args.has("screenshot")) {
            Path out = Path.of(args.get("screenshot", "screenshot.png"));
            PauseTransition settle = new PauseTransition(Duration.millis(Double.parseDouble(args.get("delay", "3000"))));
            settle.setOnFinished(e -> {
                try {
                    MainWindow.writePng(stage.getScene().snapshot(null), out);
                    System.out.println("Screenshot written to " + out);
                } catch (Exception ex) {
                    System.err.println("Screenshot failed: " + ex.getMessage());
                }
                Platform.exit();
            });
            settle.play();
        }
    }
}
