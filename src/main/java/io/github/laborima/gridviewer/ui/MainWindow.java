package io.github.laborima.gridviewer.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import io.github.laborima.gridviewer.Main;
import io.github.laborima.gridviewer.data.GridDataset;
import io.github.laborima.gridviewer.data.GridVariable;
import io.github.laborima.gridviewer.data.Overlays;
import io.github.laborima.gridviewer.data.SliceRequest;
import io.github.laborima.gridviewer.data.Stride;
import io.github.laborima.gridviewer.render.ColorMap;
import io.github.laborima.gridviewer.render.ColorMaps;
import io.github.laborima.gridviewer.render.ValueRange;

/**
 * The main window: variables on the left, map / section / globe in the middle, level and time
 * navigation below, colour controls on top.
 */
public final class MainWindow extends BorderPane {

    private static final int MAX_RECENT = 10;
    /** A public OPeNDAP dataset far larger than any disk, to show what the viewer does. */
    static final String SAMPLE_URL = "https://coastwatch.pfeg.noaa.gov/erddap/griddap/jplMURSST41";

    private final Stage stage;
    private final Session session = new Session();
    private final Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);

    private final MapView map = new MapView(session);
    private final SectionView section = new SectionView(session, map::visibleWindow);
    private final GlobeView globe = new GlobeView(session);
    private final TabPane tabs = new TabPane();
    private final Tab mapTab = new Tab("Map", map);
    private final Tab sectionTab = new Tab("Section", section);
    private final Tab globeTab = new Tab("Globe", globe);

    private final ListView<GridVariable> variables = new ListView<>();
    private final TextArea attributes = new TextArea();
    private final StackPane center = new StackPane();
    private final VBox welcome = new VBox();
    private final Menu recentMenu = new Menu("Open Recent");

    private final Slider levelSlider = new Slider();
    private final Label levelLabel = new Label();
    private final Slider timeSlider = new Slider();
    private final Label timeLabel = new Label();
    private final ToggleButton play = new ToggleButton("▶");
    private final Timeline player = new Timeline();

    private final ComboBox<ColorMap> palette = new ComboBox<>();
    private final TextField minField = new TextField();
    private final TextField maxField = new TextField();
    private final Label fileInfo = new Label();

    /** Called once the first slice of a newly opened variable is on screen. */
    private Consumer<MainWindow> onFirstImage;
    private int initialTime;
    private int initialLevel;

    public MainWindow(Stage stage) {
        this.stage = stage;
        getStyleClass().add("main-window");
        setTop(new VBox(menuBar(), toolBar()));
        setCenter(centerPane());
        setLeft(sidebar());
        setBottom(statusBar());
        installDragAndDrop();
        session.variableProperty().addListener((o, a, v) -> variableChanged(v));
        session.levelProperty().addListener((o, a, z) -> syncSlider(levelSlider, z.intValue()));
        session.timeProperty().addListener((o, a, t) -> syncSlider(timeSlider, t.intValue()));
        session.datasetProperty().addListener((o, a, ds) -> datasetChanged(ds));
        map.setOnSection(line -> {
            session.sectionProperty().set(line);
            tabs.getSelectionModel().select(sectionTab);
        });
        tabs.getSelectionModel().selectedItemProperty().addListener((o, a, t) -> {
            globe.setActive(t == globeTab);
            if (t == sectionTab) {
                section.refresh();
            }
        });
        updateTitle();
    }

    public Session session() {
        return session;
    }

    public MapView map() {
        return map;
    }

    public SectionView section() {
        return section;
    }

    public GlobeView globe() {
        return globe;
    }

    public TabPane tabs() {
        return tabs;
    }

    public Tab mapTab() {
        return mapTab;
    }

    public Tab sectionTab() {
        return sectionTab;
    }

    public Tab globeTab() {
        return globeTab;
    }

    public void setOnFirstImage(Consumer<MainWindow> callback) {
        this.onFirstImage = callback;
    }

    // ---------------------------------------------------------------- opening

    /**
     * Opens a file or URL in the background, then shows {@code variable} (or the first one).
     */
    public void open(String location, String variable) {
        open(location, variable, 0, 0);
    }

    /** Opens a file and shows {@code variable} at a time step and level (negative: from the end). */
    public void open(String location, String variable, int time, int level) {
        initialTime = time;
        initialLevel = level;
        session.readInfoProperty().set("Opening " + location + "…");
        session.loading("open", true);
        Thread.ofVirtual().name("open").start(() -> {
            try {
                GridDataset ds = GridDataset.open(location);
                Platform.runLater(() -> {
                    session.loading("open", false);
                    GridDataset old = session.dataset();
                    session.datasetProperty().set(ds);
                    if (old != null) {
                        old.close();
                    }
                    rememberRecent(location);
                    if (ds.variables().isEmpty()) {
                        error("No gridded variable", ds.displayName() + " contains no variable with two horizontal dimensions.");
                        return;
                    }
                    GridVariable v = variable == null ? null : ds.variable(variable);
                    variables.getSelectionModel().select(v != null ? v : ds.variables().getFirst());
                });
            } catch (IOException | RuntimeException e) {
                Platform.runLater(() -> {
                    session.loading("open", false);
                    session.readInfoProperty().set("");
                    error("Cannot open " + location, e.getMessage() == null ? e.toString() : e.getMessage());
                });
            }
        });
    }

    private void openDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open a NetCDF file");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("NetCDF / HDF5", "*.nc", "*.nc4", "*.cdf", "*.netcdf", "*.h5", "*.hdf5", "*.he5"),
                new FileChooser.ExtensionFilter("All files", "*"));
        String last = prefs.get("lastDirectory", null);
        if (last != null && new File(last).isDirectory()) {
            chooser.setInitialDirectory(new File(last));
        }
        File f = chooser.showOpenDialog(stage);
        if (f != null) {
            prefs.put("lastDirectory", f.getParent());
            open(f.getAbsolutePath(), null);
        }
    }

    private void openUrlDialog() {
        TextInputDialog d = new TextInputDialog(prefs.get("lastUrl", "https://"));
        d.initOwner(stage);
        d.setTitle("Open URL");
        d.setHeaderText("OPeNDAP or HTTP URL of a NetCDF dataset.\nOnly the points on screen are transferred.");
        d.setContentText("URL:");
        d.getDialogPane().setPrefWidth(620);
        d.showAndWait().map(String::strip).filter(s -> !s.isEmpty()).ifPresent(url -> {
            prefs.put("lastUrl", url);
            open(url, null);
        });
    }

    private void rememberRecent(String location) {
        List<String> recent = new ArrayList<>(recentFiles());
        recent.remove(location);
        recent.addFirst(location);
        while (recent.size() > MAX_RECENT) {
            recent.removeLast();
        }
        prefs.put("recent", String.join("\n", recent));
        refreshRecent();
    }

    private List<String> recentFiles() {
        String s = prefs.get("recent", "");
        return s.isBlank() ? List.of() : List.of(s.split("\n"));
    }

    private void refreshRecent() {
        recentMenu.getItems().clear();
        for (String r : recentFiles()) {
            MenuItem item = new MenuItem(r);
            item.setMnemonicParsing(false);
            item.setOnAction(e -> open(r, null));
            recentMenu.getItems().add(item);
        }
        recentMenu.setDisable(recentMenu.getItems().isEmpty());
        buildWelcome();
    }

    // ---------------------------------------------------------------- state changes

    private void datasetChanged(GridDataset ds) {
        variables.getItems().setAll(ds == null ? List.of() : ds.variables());
        welcome.setVisible(ds == null);
        tabs.setVisible(ds != null);
        if (ds != null) {
            long size = ds.fileSize();
            fileInfo.setText(ds.fileType() + (size >= 0 ? "  ·  " + Painting.bytes(size) : "  ·  remote"));
        } else {
            fileInfo.setText("");
        }
        updateTitle();
    }

    private void variableChanged(GridVariable v) {
        player.stop();
        play.setSelected(false);
        if (v == null) {
            attributes.clear();
            return;
        }
        attributes.setText(v.attributesString());
        attributes.positionCaret(0);
        configureSlider(levelSlider, v.levels(), session.levelProperty().get());
        configureSlider(timeSlider, v.times(), session.timeProperty().get());
        updateNavigationLabels();
        if (v.levels() < 2 && tabs.getSelectionModel().getSelectedItem() == sectionTab) {
            tabs.getSelectionModel().select(mapTab);
        }
        if (onFirstImage != null) {
            Consumer<MainWindow> callback = onFirstImage;
            onFirstImage = null;
            waitForImage(callback);
        }
        updateTitle();
    }

    /** Runs {@code callback} once no read has been pending for a little while. */
    public void whenIdle(Runnable callback) {
        Timeline poll = new Timeline();
        int[] quiet = {0};
        poll.getKeyFrames().add(new KeyFrame(Duration.millis(100), e -> {
            quiet[0] = session.pendingReadsProperty().get() == 0 ? quiet[0] + 1 : 0;
            if (quiet[0] >= 8) {
                poll.stop();
                callback.run();
            }
        }));
        poll.setCycleCount(Timeline.INDEFINITE);
        poll.play();
    }

    private void waitForImage(Consumer<MainWindow> callback) {
        Timeline poll = new Timeline();
        poll.getKeyFrames().add(new KeyFrame(Duration.millis(150), e -> {
            if (session.pendingReadsProperty().get() == 0) {
                poll.stop();
                callback.accept(this);
            }
        }));
        poll.setCycleCount(Timeline.INDEFINITE);
        poll.setDelay(Duration.millis(300));
        poll.play();
    }

    /** Moves a slider to a value set elsewhere, widening it first so the value is not clamped. */
    private static void syncSlider(Slider s, int value) {
        if (value > s.getMax()) {
            s.setMax(value);
        }
        s.setValue(value);
    }

    private static void configureSlider(Slider s, int n, int value) {
        s.setMin(0);
        s.setMax(Math.max(0, n - 1));
        s.setValue(value);
        s.setMajorTickUnit(Math.max(1, n - 1));
        s.setBlockIncrement(1);
        s.setDisable(n < 2);
    }

    private void updateNavigationLabels() {
        GridVariable v = session.variable();
        if (v == null) {
            return;
        }
        int z = session.levelProperty().get();
        int t = session.timeProperty().get();
        levelLabel.setText(v.levelAxis() == null ? "—" : v.levelAxis().label(z) + "   (" + (z + 1) + "/" + v.levels() + ")");
        timeLabel.setText(v.timeAxis() == null ? "—" : v.timeAxis().label(t) + "   (" + (t + 1) + "/" + v.times() + ")");
    }

    private void updateTitle() {
        GridDataset ds = session.dataset();
        GridVariable v = session.variable();
        stage.setTitle(ds == null ? "Grid Viewer"
                : "Grid Viewer — " + ds.displayName() + (v == null ? "" : "  ·  " + v.name()));
    }

    private void stepLevel(int delta) {
        GridVariable v = session.variable();
        if (v != null) {
            levelSlider.setValue(Math.clamp(session.levelProperty().get() + delta, 0, v.levels() - 1));
        }
    }

    private void stepTime(int delta) {
        GridVariable v = session.variable();
        if (v != null && v.times() > 1) {
            timeSlider.setValue(Math.floorMod(session.timeProperty().get() + delta, v.times()));
        }
    }

    // ---------------------------------------------------------------- layout

    private MenuBar menuBar() {
        MenuItem open = item("Open…", new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), this::openDialog);
        MenuItem openUrl = item("Open URL…", new KeyCodeCombination(KeyCode.U, KeyCombination.SHORTCUT_DOWN), this::openUrlDialog);
        MenuItem export = item("Export Image…", new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN), this::exportDialog);
        MenuItem close = item("Close File", new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN), this::closeFile);
        MenuItem quit = item("Quit", new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN), Platform::exit);
        refreshRecent();
        Menu file = new Menu("File", null, open, openUrl, recentMenu, new SeparatorMenuItem(), export,
                new SeparatorMenuItem(), close, quit);

        MenuItem showMap = item("Map", new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN),
                () -> tabs.getSelectionModel().select(mapTab));
        MenuItem showSection = item("Section", new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN),
                () -> tabs.getSelectionModel().select(sectionTab));
        MenuItem showGlobe = item("Globe", new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.SHORTCUT_DOWN),
                () -> tabs.getSelectionModel().select(globeTab));
        MenuItem fit = item("Zoom to Fit", new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN), map::fit);
        MenuItem zoomIn = item("Zoom In", new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN),
                () -> map.zoomAtCenter(1.5));
        MenuItem zoomOut = item("Zoom Out", new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN),
                () -> map.zoomAtCenter(1 / 1.5));
        CheckMenuItem trueDepth = new CheckMenuItem("Sections at True Depth");
        trueDepth.selectedProperty().bindBidirectional(section.trueDepthProperty());
        CheckMenuItem spin = new CheckMenuItem("Rotate Globe");
        spin.selectedProperty().bindBidirectional(globe.autoRotateProperty());
        MenuItem fullScreen = item("Full Screen", new KeyCodeCombination(KeyCode.F11), () -> stage.setFullScreen(!stage.isFullScreen()));
        Menu view = new Menu("View", null, showMap, showSection, showGlobe, new SeparatorMenuItem(), fit, zoomIn,
                zoomOut, new SeparatorMenuItem(), trueDepth, spin, new SeparatorMenuItem(), fullScreen);

        MenuItem nextLevel = item("Next Level (deeper)", new KeyCodeCombination(KeyCode.PAGE_DOWN), () -> stepLevel(1));
        MenuItem prevLevel = item("Previous Level", new KeyCodeCombination(KeyCode.PAGE_UP), () -> stepLevel(-1));
        MenuItem nextTime = item("Next Time Step", new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN), () -> stepTime(1));
        MenuItem prevTime = item("Previous Time Step", new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN), () -> stepTime(-1));
        MenuItem autoRange = item("Automatic Colour Range", new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN),
                this::autoRangeFromView);
        CheckMenuItem mask = new CheckMenuItem("Treat Zeros as Land");
        mask.selectedProperty().bindBidirectional(session.maskZerosProperty());
        Menu data = new Menu("Data", null, prevLevel, nextLevel, prevTime, nextTime, new SeparatorMenuItem(), autoRange, mask);

        MenuItem points = item("Load Point List…", null, () -> loadOverlay(true));
        MenuItem layout = item("Load NEMO layout.dat…", null, () -> loadOverlay(false));
        MenuItem clear = item("Clear Overlays", null, () -> {
            session.points().clear();
            session.domains().clear();
        });
        Menu overlays = new Menu("Overlays", null, points, layout, new SeparatorMenuItem(), clear);

        MenuItem keys = item("Keyboard and Mouse", new KeyCodeCombination(KeyCode.F1), this::showHelp);
        MenuItem about = item("About Grid Viewer", null, this::showAbout);
        Menu help = new Menu("Help", null, keys, about);

        MenuBar bar = new MenuBar(file, view, data, overlays, help);
        bar.setUseSystemMenuBar(true);
        return bar;
    }

    private static MenuItem item(String text, KeyCombination accel, Runnable action) {
        MenuItem m = new MenuItem(text);
        if (accel != null) {
            m.setAccelerator(accel);
        }
        m.setOnAction(e -> action.run());
        return m;
    }

    private ToolBar toolBar() {
        Button open = button("Open", "Open a NetCDF file (Ctrl+O)", this::openDialog);
        open.getStyleClass().add("accent");
        Button url = button("URL", "Open an OPeNDAP URL (Ctrl+U)", this::openUrlDialog);

        palette.getItems().setAll(ColorMaps.all());
        palette.setCellFactory(l -> new PaletteCell());
        palette.setButtonCell(new PaletteCell());
        palette.setVisibleRowCount(16);
        palette.valueProperty().bindBidirectional(session.colorMapProperty());
        palette.setTooltip(new Tooltip("Colour map"));

        minField.setPrefColumnCount(6);
        maxField.setPrefColumnCount(6);
        minField.setTooltip(new Tooltip("Lowest value of the colour range"));
        maxField.setTooltip(new Tooltip("Highest value of the colour range"));
        minField.setOnAction(e -> commitRange());
        maxField.setOnAction(e -> commitRange());
        minField.focusedProperty().addListener((o, a, f) -> {
            if (!f) {
                commitRange();
            }
        });
        maxField.focusedProperty().addListener((o, a, f) -> {
            if (!f) {
                commitRange();
            }
        });
        session.rangeProperty().addListener((o, a, r) -> showRange(r));
        showRange(session.range());

        Button auto = button("Auto", "Fit the colour range to what is on screen (Ctrl+R)", this::autoRangeFromView);
        Button sym = button("±", "Centre the colour range on zero", () -> session.rangeProperty().set(session.range().symmetric()));
        Button reverse = button("⇄", "Reverse the colour map", () -> session.colorMapProperty().set(session.colorMap().reversed()));
        ToggleButton mask = new ToggleButton("0 = land");
        mask.setTooltip(new Tooltip("Draw exact zeros as land, for NEMO outputs without fill values"));
        mask.selectedProperty().bindBidirectional(session.maskZerosProperty());

        Button fit = button("Fit", "Zoom to fit (Ctrl+0)", map::fit);
        Button export = button("Export", "Save the current view as PNG (Ctrl+E)", this::exportDialog);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Node[] range = {new Label("Range"), minField, new Label("to"), maxField, auto, sym};
        for (Node n : new Node[] {palette, reverse, minField, maxField, auto, sym, mask, fit, export}) {
            n.disableProperty().bind(session.variableProperty().isNull());
        }
        ToolBar bar = new ToolBar(open, url, new Separator(), palette, reverse);
        bar.getItems().addAll(range);
        bar.getItems().addAll(mask, spacer, fit, export);
        bar.getStyleClass().add("main-toolbar");
        return bar;
    }

    private static Button button(String text, String tip, Runnable action) {
        Button b = new Button(text);
        b.setTooltip(new Tooltip(tip));
        b.setOnAction(e -> action.run());
        return b;
    }

    private Node sidebar() {
        Label title = new Label("VARIABLES");
        title.getStyleClass().add("sidebar-title");
        variables.setCellFactory(l -> new VariableCell());
        variables.getSelectionModel().selectedItemProperty().addListener((o, a, v) -> {
            if (v != null && v != session.variable()) {
                // Negative indices count from the end: -1 is the latest time step.
                int t = initialTime < 0 ? v.times() + initialTime : initialTime;
                int z = initialLevel < 0 ? v.levels() + initialLevel : initialLevel;
                session.show(v, t, z);
                initialTime = 0;
                initialLevel = 0;
            }
        });
        VBox.setVgrow(variables, Priority.ALWAYS);
        Label attrTitle = new Label("ATTRIBUTES");
        attrTitle.getStyleClass().add("sidebar-title");
        attributes.setEditable(false);
        attributes.setWrapText(true);
        attributes.getStyleClass().add("attributes");
        attributes.setPrefRowCount(12);
        VBox box = new VBox(6, title, variables, attrTitle, attributes);
        box.getStyleClass().add("sidebar");
        box.setPadding(new Insets(10));
        box.setPrefWidth(300);
        box.setMinWidth(220);
        return box;
    }

    private Node centerPane() {
        mapTab.setClosable(false);
        sectionTab.setClosable(false);
        globeTab.setClosable(false);
        tabs.getTabs().addAll(mapTab, sectionTab, globeTab);
        tabs.setVisible(false);
        buildWelcome();
        center.getChildren().addAll(welcome, tabs);
        BorderPane pane = new BorderPane(center);
        pane.setBottom(navigationBar());
        return pane;
    }

    private void buildWelcome() {
        welcome.getChildren().clear();
        welcome.getStyleClass().setAll("welcome");
        welcome.setAlignment(Pos.CENTER);
        welcome.setSpacing(14);
        Image icon = GridViewerApp.icon(256);
        if (icon != null) {
            ImageView iv = new ImageView(icon);
            iv.setFitWidth(128);
            iv.setFitHeight(128);
            welcome.getChildren().add(iv);
        }
        Label name = new Label("Grid Viewer");
        name.getStyleClass().add("welcome-title");
        Label sub = new Label("Open a NetCDF file, or drop it here. Only the points on screen are read,\n"
                + "so files larger than memory open instantly.");
        sub.getStyleClass().add("welcome-subtitle");
        sub.setAlignment(Pos.CENTER);
        sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Button open = button("Open file…", "Ctrl+O", this::openDialog);
        open.getStyleClass().addAll("accent", "big");
        Button url = button("Open URL…", "Ctrl+U", this::openUrlDialog);
        url.getStyleClass().add("big");
        HBox buttons = new HBox(10, open, url);
        buttons.setAlignment(Pos.CENTER);
        Hyperlink sample = new Hyperlink("No file at hand? Try 21 TB of daily sea surface temperature at 1 km (NASA MUR, remote)");
        sample.setOnAction(e -> open(SAMPLE_URL, "analysed_sst", -1, 0));
        welcome.getChildren().addAll(name, sub, buttons, sample);
        List<String> recent = recentFiles();
        if (!recent.isEmpty()) {
            Label r = new Label("RECENT");
            r.getStyleClass().add("sidebar-title");
            VBox list = new VBox(2, r);
            list.setAlignment(Pos.CENTER);
            list.setPadding(new Insets(18, 0, 0, 0));
            for (String path : recent.subList(0, Math.min(6, recent.size()))) {
                Hyperlink link = new Hyperlink(path);
                link.setOnAction(e -> open(path, null));
                list.getChildren().add(link);
            }
            welcome.getChildren().add(list);
        }
    }

    private Node navigationBar() {
        levelSlider.valueProperty().addListener((o, a, b) -> {
            int z = (int) Math.round(b.doubleValue());
            if (z != session.levelProperty().get()) {
                session.levelProperty().set(z);
            }
            updateNavigationLabels();
        });
        timeSlider.valueProperty().addListener((o, a, b) -> {
            int t = (int) Math.round(b.doubleValue());
            if (t != session.timeProperty().get()) {
                session.timeProperty().set(t);
            }
            updateNavigationLabels();
        });
        levelSlider.setSnapToTicks(false);
        HBox.setHgrow(levelSlider, Priority.ALWAYS);
        HBox.setHgrow(timeSlider, Priority.ALWAYS);
        levelLabel.getStyleClass().add("nav-value");
        timeLabel.getStyleClass().add("nav-value");
        levelLabel.setMinWidth(150);
        timeLabel.setMinWidth(190);

        player.getKeyFrames().add(new KeyFrame(Duration.millis(350), e -> stepTime(1)));
        player.setCycleCount(Timeline.INDEFINITE);
        play.setTooltip(new Tooltip("Play the time steps"));
        play.selectedProperty().addListener((o, a, on) -> {
            play.setText(on ? "❚❚" : "▶");
            if (on) {
                player.play();
            } else {
                player.stop();
            }
        });
        play.disableProperty().bind(timeSlider.disabledProperty());

        Label lv = new Label("Level");
        lv.getStyleClass().add("nav-title");
        Label tv = new Label("Time");
        tv.getStyleClass().add("nav-title");
        HBox level = new HBox(8, lv, levelSlider, levelLabel);
        level.setAlignment(Pos.CENTER_LEFT);
        HBox time = new HBox(8, tv, play, timeSlider, timeLabel);
        time.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(level, Priority.ALWAYS);
        HBox.setHgrow(time, Priority.ALWAYS);
        HBox bar = new HBox(24, level, time);
        bar.getStyleClass().add("navigation");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.visibleProperty().bind(session.variableProperty().isNotNull());
        bar.managedProperty().bind(bar.visibleProperty());
        return bar;
    }

    private Node statusBar() {
        Label cursor = new Label();
        cursor.textProperty().bind(session.cursorInfoProperty());
        cursor.getStyleClass().add("status-cursor");
        Label read = new Label();
        read.textProperty().bind(session.readInfoProperty());
        read.getStyleClass().add("status-read");
        ProgressIndicator busy = new ProgressIndicator();
        busy.setPrefSize(14, 14);
        busy.visibleProperty().bind(Bindings.greaterThan(session.pendingReadsProperty(), 0));
        fileInfo.getStyleClass().add("status-file");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep.visibleProperty().bind(fileInfo.textProperty().isNotEmpty());
        HBox bar = new HBox(14, cursor, spacer, busy, read, sep, fileInfo);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    // ---------------------------------------------------------------- actions

    private void commitRange() {
        try {
            double lo = Double.parseDouble(minField.getText().strip().replace(',', '.'));
            double hi = Double.parseDouble(maxField.getText().strip().replace(',', '.'));
            if (hi > lo && (lo != session.range().min() || hi != session.range().max())) {
                session.rangeProperty().set(new ValueRange(lo, hi));
            }
        } catch (NumberFormatException e) {
            showRange(session.range());
        }
    }

    private void showRange(ValueRange r) {
        minField.setText(Painting.value(r.min()));
        maxField.setText(Painting.value(r.max()));
    }

    private void autoRangeFromView() {
        GridVariable v = session.variable();
        GridDataset ds = session.dataset();
        if (v == null || ds == null) {
            return;
        }
        var window = map.visibleWindow();
        if (window == null) {
            return;
        }
        int stride = Stride.forWindow(window, 512, 512);
        var request = new SliceRequest(v.name(), session.timeProperty().get(),
                session.levelProperty().get(), window.alignTo(stride), stride);
        ds.submit(() -> ds.readSlice(v, request)).thenAccept(s -> Platform.runLater(() -> session.autoRange(s.values())));
    }

    private void closeFile() {
        GridDataset ds = session.dataset();
        session.show(null);
        session.datasetProperty().set(null);
        if (ds != null) {
            ds.close();
        }
    }

    private void loadOverlay(boolean points) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(points ? "Load a point list (i j [label] per line)" : "Load a NEMO layout.dat");
        File f = chooser.showOpenDialog(stage);
        if (f == null) {
            return;
        }
        try {
            if (points) {
                session.points().setAll(Overlays.readPoints(f.toPath()));
            } else {
                session.domains().setAll(Overlays.readLayout(f.toPath()));
            }
        } catch (IOException e) {
            error("Cannot read " + f.getName(), e.getMessage());
        }
    }

    private void exportDialog() {
        if (session.variable() == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));
        GridVariable v = session.variable();
        String tab = tabs.getSelectionModel().getSelectedItem().getText().toLowerCase(Locale.ROOT);
        chooser.setInitialFileName(v.name() + "-" + tab + ".png");
        File f = chooser.showSaveDialog(stage);
        if (f != null) {
            try {
                exportCurrentView(f.toPath());
            } catch (IOException e) {
                error("Cannot write " + f.getName(), e.getMessage());
            }
        }
    }

    /** Writes the visible tab (map, section or globe) to a PNG file. */
    public void exportCurrentView(Path file) throws IOException {
        Tab t = tabs.getSelectionModel().getSelectedItem();
        WritableImage img = t == sectionTab ? section.snapshot() : t == globeTab ? globe.snapshotImage() : map.snapshot();
        writePng(img, file);
    }

    static void writePng(WritableImage img, Path file) throws IOException {
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        int[] argb = new int[w * h];
        img.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), argb, 0, w);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, argb, 0, w);
        ImageIO.write(out, "png", file.toFile());
    }

    private void installDragAndDrop() {
        setOnDragOver(e -> {
            if (e.getDragboard().hasFiles() || e.getDragboard().hasUrl()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        setOnDragDropped(e -> {
            var db = e.getDragboard();
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                open(db.getFiles().getFirst().getAbsolutePath(), null);
            } else if (db.hasUrl()) {
                open(db.getUrl(), null);
            }
            e.setDropCompleted(true);
            e.consume();
        });
    }

    private void showHelp() {
        info("Keyboard and mouse", """
                Map
                  Mouse wheel / pinch        zoom around the pointer
                  Drag                       pan
                  Shift + drag               zoom to a box
                  Double-click               zoom in (Alt: zoom out)
                  Right-click                vertical section along a row or column
                  + / -   arrows   0         zoom, pan, fit

                Data
                  Page Up / Page Down        previous / next level
                  Alt + Left / Right         previous / next time step
                  Ctrl+R                     colour range from what is on screen

                Globe
                  Drag to rotate, wheel to zoom.
                """);
    }

    private void showAbout() {
        info("About Grid Viewer", "Grid Viewer " + Main.VERSION + """


                Fast viewer for large NetCDF ocean and climate model grids.
                First written in 2006 at Mercator Océan, rewritten in 2026
                for Java 25 and JavaFX.

                Reading: Unidata netCDF-Java. Colour maps: cmocean, matplotlib.
                """);
    }

    private void info(String title, String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.initOwner(stage);
        a.setTitle(title);
        a.setHeaderText(title);
        Label body = new Label(text);
        body.getStyleClass().add("monospace");
        a.getDialogPane().setContent(body);
        a.showAndWait();
    }

    private void error(String title, String text) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initOwner(stage);
        a.setTitle("Grid Viewer");
        a.setHeaderText(title);
        a.setContentText(text);
        a.showAndWait();
    }

    // ---------------------------------------------------------------- cells

    private static final class VariableCell extends ListCell<GridVariable> {
        private final Label name = new Label();
        private final Label desc = new Label();
        private final Label dims = new Label();
        private final VBox box = new VBox(1, name, desc, dims);

        VariableCell() {
            name.getStyleClass().add("var-name");
            desc.getStyleClass().add("var-desc");
            dims.getStyleClass().add("var-dims");
            box.setPadding(new Insets(4, 2, 4, 2));
        }

        @Override
        protected void updateItem(GridVariable v, boolean empty) {
            super.updateItem(v, empty);
            if (empty || v == null) {
                setGraphic(null);
                return;
            }
            name.setText(v.name() + (v.units().isEmpty() ? "" : "   " + v.units()));
            desc.setText(v.longName());
            dims.setText(v.dimensionsString());
            setTooltip(new Tooltip(v.longName()));
            setGraphic(box);
        }
    }

    private static final class PaletteCell extends ListCell<ColorMap> {
        private final ImageView swatch = new ImageView();

        PaletteCell() {
            swatch.setFitWidth(72);
            swatch.setFitHeight(12);
            swatch.setSmooth(true);
        }

        @Override
        protected void updateItem(ColorMap m, boolean empty) {
            super.updateItem(m, empty);
            if (empty || m == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            swatch.setImage(Painting.strip(m));
            setGraphic(swatch);
            setText(m.name());
        }
    }
}
