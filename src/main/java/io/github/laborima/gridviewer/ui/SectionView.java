package io.github.laborima.gridviewer.ui;

import java.util.Locale;
import java.util.function.Supplier;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import io.github.laborima.gridviewer.data.Axis;
import io.github.laborima.gridviewer.data.GridDataset;
import io.github.laborima.gridviewer.data.GridVariable;
import io.github.laborima.gridviewer.data.LatestOnly;
import io.github.laborima.gridviewer.data.Section;
import io.github.laborima.gridviewer.data.SectionRequest;
import io.github.laborima.gridviewer.data.Stride;
import io.github.laborima.gridviewer.data.Window;
import io.github.laborima.gridviewer.render.Rasterizer;

/**
 * Vertical section through every level, along the row or column picked on the map, over the
 * horizontal extent currently visible on the map. Levels are drawn either evenly (one band per
 * model level, which favours the upper ocean) or at their true depth.
 */
public final class SectionView extends Region {

    private record Read(GridVariable variable, SectionRequest request) {
    }

    private static final double LEFT = 70;
    private static final double TOP = 44;
    private static final double RIGHT = 20;
    private static final double BOTTOM = 92;

    private final Session session;
    private final Supplier<Window> mapWindow;
    private final Canvas canvas = new Canvas();
    private final BooleanProperty trueDepth = new SimpleBooleanProperty(this, "trueDepth");
    private final PauseTransition delay = new PauseTransition(Duration.millis(40));

    private LatestOnly<Read, Section> loader;
    private GridVariable shownVariable;
    private Section section;
    private WritableImage image;
    private double mouseX = Double.NaN;
    private double mouseY = Double.NaN;

    /**
     * @param mapWindow supplies the grid window visible on the map, to cut the section to it
     */
    public SectionView(Session session, Supplier<Window> mapWindow) {
        this.session = session;
        this.mapWindow = mapWindow;
        getStyleClass().add("section-view");
        getChildren().add(canvas);
        delay.setOnFinished(e -> request());
        session.datasetProperty().addListener((o, a, ds) -> attach(ds));
        session.variableProperty().addListener((o, a, b) -> {
            section = null;
            image = null;
            paint();
        });
        session.sectionProperty().addListener((o, a, b) -> refresh());
        session.timeProperty().addListener((o, a, b) -> refresh());
        session.levelProperty().addListener((o, a, b) -> paint());
        session.colorMapProperty().addListener((o, a, b) -> recolor());
        session.rangeProperty().addListener((o, a, b) -> recolor());
        session.maskZerosProperty().addListener((o, a, b) -> recolor());
        trueDepth.addListener((o, a, b) -> recolor());
        addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            mouseX = e.getX();
            mouseY = e.getY();
            paint();
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            mouseX = Double.NaN;
            paint();
        });
    }

    public BooleanProperty trueDepthProperty() {
        return trueDepth;
    }

    /** Re-reads the section, for instance when the tab is shown after the map moved. */
    public void refresh() {
        delay.playFromStart();
    }

    public WritableImage snapshot() {
        return canvas.snapshot(null, null);
    }

    private void attach(GridDataset ds) {
        if (loader != null) {
            loader.cancel();
        }
        session.loading("section", false);
        section = null;
        image = null;
        loader = ds == null ? null : new LatestOnly<>(ds, r -> r.variable().readSection(r.request()), Platform::runLater);
        paint();
    }

    private void request() {
        GridVariable v = session.variable();
        Session.SectionLine line = session.sectionProperty().get();
        if (v == null || line == null || loader == null || v.levels() < 2) {
            section = null;
            image = null;
            paint();
            return;
        }
        boolean alongRow = line.orientation() == SectionRequest.Orientation.ALONG_ROW;
        int length = alongRow ? v.nx() : v.ny();
        Window w = mapWindow.get();
        int from = w == null ? 0 : alongRow ? w.i0() : w.j0();
        int to = w == null ? length - 1 : alongRow ? w.i1() : w.j1();
        double px = Math.max(200, getWidth() - LEFT - RIGHT) * outputScale();
        int stride = Stride.forAxis(to - from + 1, px);
        from -= Math.floorMod(from, stride);
        SectionRequest r = new SectionRequest(v.name(), session.timeProperty().get(), line.orientation(),
                line.index(), from, to, stride);
        session.loading("section", true);
        loader.request(new Read(v, r), s -> {
            session.loading("section", false);
            shownVariable = v;
            section = s;
            recolor();
        }, e -> {
            session.loading("section", false);
            section = null;
            image = null;
            session.readInfoProperty().set("Section failed: " + e.getMessage());
            paint();
        });
    }

    private double outputScale() {
        return getScene() != null && getScene().getWindow() != null ? getScene().getWindow().getOutputScaleX() : 1;
    }

    private void recolor() {
        if (section == null) {
            paint();
            return;
        }
        int cols = section.columns();
        float[] values = section.values();
        Axis z = shownVariable.levelAxis();
        if (trueDepth.get() && z.hasValues()) {
            // Resample levels onto rows evenly spaced in depth.
            int rows = Math.max(100, (int) Math.round(Math.max(1, getHeight() - TOP - BOTTOM) * outputScale()));
            float[] resampled = new float[rows * cols];
            double maxDepth = depthBottom(z);
            for (int r = 0; r < rows; r++) {
                int k = levelAtDepth(z, (r + 0.5) / rows * maxDepth);
                System.arraycopy(values, k * cols, resampled, r * cols, cols);
            }
            values = resampled;
        }
        int[] argb = Rasterizer.colorize(values, cols, session.colorMap(), session.range(), false,
                session.maskZerosProperty().get());
        image = Painting.image(argb, cols);
        paint();
    }

    private static double depthBottom(Axis z) {
        int n = z.length();
        return Math.abs(z.value(n - 1)) + Math.abs(z.value(n - 1) - z.value(n - 2)) / 2;
    }

    private static int levelAtDepth(Axis z, double depth) {
        int best = 0;
        for (int k = 1; k < z.length(); k++) {
            double boundary = (Math.abs(z.value(k - 1)) + Math.abs(z.value(k))) / 2;
            if (depth >= boundary) {
                best = k;
            }
        }
        return best;
    }

    @Override
    protected void layoutChildren() {
        if (canvas.getWidth() != getWidth() || canvas.getHeight() != getHeight()) {
            canvas.setWidth(getWidth());
            canvas.setHeight(getHeight());
            if (trueDepth.get()) {
                recolor();
            } else {
                paint();
            }
        }
    }

    private void paint() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.setFill(Painting.BACKGROUND);
        gc.fillRect(0, 0, w, h);
        GridVariable v = session.variable();
        Session.SectionLine line = session.sectionProperty().get();
        gc.setTextBaseline(VPos.CENTER);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Painting.LABEL);
        gc.setFill(Painting.MUTED);
        if (v == null) {
            return;
        }
        if (v.levels() < 2) {
            gc.fillText(v.name() + " has a single level: no vertical section.", w / 2, h / 2);
            return;
        }
        if (line == null || section == null || image == null) {
            gc.fillText(line == null
                    ? "Right-click on the map and pick “Vertical section along this row / column”."
                    : "Reading section…", w / 2, h / 2);
            return;
        }
        double pw = w - LEFT - RIGHT;
        double ph = h - TOP - BOTTOM;
        gc.setImageSmoothing(false);
        gc.drawImage(image, LEFT, TOP, pw, ph);
        gc.setStroke(Color.web("#ffffff", 0.25));
        gc.setLineWidth(1);
        gc.strokeRect(LEFT - 0.5, TOP - 0.5, pw + 1, ph + 1);

        SectionRequest r = section.request();
        boolean alongRow = r.orientation() == SectionRequest.Orientation.ALONG_ROW;
        Axis z = v.levelAxis();
        drawDepthTicks(gc, z, ph);
        drawLevelMarker(gc, z, pw, ph);
        drawPositionTicks(gc, v, r, alongRow, pw, ph);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Painting.LABEL);
        gc.setFill(Painting.TEXT);
        gc.fillText(title(v, r, alongRow), LEFT, TOP / 2);
        String units = v.units().isEmpty() ? "" : "  [" + v.units() + "]";
        Painting.colorBar(gc, w - RIGHT, h - 8, session.colorMap(), session.range(), v.name() + units);
        drawCursor(gc, v, z, r, alongRow, pw, ph);
    }

    private String title(GridVariable v, SectionRequest r, boolean alongRow) {
        StringBuilder sb = new StringBuilder(v.longName());
        sb.append("  ·  ").append(alongRow ? "row j = " : "column i = ").append(r.fixed());
        Axis fixedAxis = alongRow ? v.yAxis() : v.xAxis();
        if (fixedAxis.hasValues()) {
            double c = fixedAxis.value(r.fixed());
            sb.append(" (").append(alongRow ? Painting.lat(c) : Painting.lon(c)).append(')');
        }
        if (v.timeAxis() != null && v.times() > 1) {
            sb.append("  ·  ").append(v.timeAxis().label(r.time()));
        }
        sb.append(String.format(Locale.ROOT, "  ·  stride %d, %d ms", r.stride(), section.readNanos() / 1_000_000));
        return sb.toString();
    }

    private double levelTop(Axis z, int k, double ph) {
        if (trueDepth.get() && z.hasValues()) {
            double boundary = k == 0 ? 0 : (Math.abs(z.value(k - 1)) + Math.abs(z.value(k))) / 2;
            return TOP + boundary / depthBottom(z) * ph;
        }
        return TOP + k * ph / z.length();
    }

    private void drawDepthTicks(GraphicsContext gc, Axis z, double ph) {
        gc.setFont(Painting.SMALL);
        gc.setFill(Painting.MUTED);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setTextBaseline(VPos.CENTER);
        double lastY = -100;
        for (int k = 0; k < z.length(); k++) {
            double y = (levelTop(z, k, ph) + levelTop(z, k + 1 < z.length() ? k + 1 : k, ph)) / 2;
            if (k == z.length() - 1) {
                y = levelTop(z, k, ph) + (TOP + ph - levelTop(z, k, ph)) / 2;
            }
            if (y - lastY < 18) {
                continue;
            }
            lastY = y;
            gc.fillText(z.label(k), LEFT - 8, y);
            gc.setStroke(Color.web("#ffffff", 0.3));
            gc.strokeLine(LEFT - 5, Math.round(y) + 0.5, LEFT - 1, Math.round(y) + 0.5);
        }
    }

    private void drawLevelMarker(GraphicsContext gc, Axis z, double pw, double ph) {
        int k = session.levelProperty().get();
        if (k < 0 || k >= z.length()) {
            return;
        }
        double y0 = levelTop(z, k, ph);
        double y1 = k + 1 < z.length() ? levelTop(z, k + 1, ph) : TOP + ph;
        double y = (y0 + y1) / 2;
        gc.setStroke(Painting.HIGHLIGHT);
        gc.setLineWidth(1.5);
        gc.setLineDashes(6, 4);
        gc.strokeLine(LEFT, y, LEFT + pw, y);
        gc.setLineDashes();
    }

    private void drawPositionTicks(GraphicsContext gc, GridVariable v, SectionRequest r, boolean alongRow,
                                   double pw, double ph) {
        Axis along = alongRow ? v.xAxis() : v.yAxis();
        int n = r.to() - r.from() + 1;
        gc.setFont(Painting.SMALL);
        gc.setFill(Painting.MUTED);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.TOP);
        int ticks = Math.max(2, (int) (pw / 110));
        for (int t = 0; t <= ticks; t++) {
            int idx = r.from() + (int) Math.round(t * (n - 1) / (double) ticks);
            double x = LEFT + (idx - r.from() + 0.5) / n * pw;
            String label;
            if (along.hasValues() && (along.isLongitude() || along.isLatitude())) {
                label = along.isLongitude() ? Painting.lon(along.value(idx)) : Painting.lat(along.value(idx));
            } else {
                label = (alongRow ? "i " : "j ") + idx;
            }
            gc.setTextAlign(t == 0 ? TextAlignment.LEFT : t == ticks ? TextAlignment.RIGHT : TextAlignment.CENTER);
            gc.fillText(label, t == 0 ? LEFT : t == ticks ? LEFT + pw : x, TOP + ph + 6);
            gc.setStroke(Color.web("#ffffff", 0.3));
            gc.strokeLine(Math.round(x) + 0.5, TOP + ph + 1, Math.round(x) + 0.5, TOP + ph + 4);
        }
    }

    private void drawCursor(GraphicsContext gc, GridVariable v, Axis z, SectionRequest r, boolean alongRow,
                            double pw, double ph) {
        if (Double.isNaN(mouseX) || mouseX < LEFT || mouseX > LEFT + pw || mouseY < TOP || mouseY > TOP + ph) {
            return;
        }
        int col = Math.clamp((int) ((mouseX - LEFT) / pw * section.columns()), 0, section.columns() - 1);
        int k = 0;
        for (int l = 0; l < z.length(); l++) {
            if (mouseY >= levelTop(z, l, ph)) {
                k = l;
            }
        }
        int idx = r.from() + col * r.stride();
        float value = section.valueAt(k, col);
        Axis along = alongRow ? v.xAxis() : v.yAxis();
        String where = along.hasValues() && (along.isLongitude() || along.isLatitude())
                ? (along.isLongitude() ? Painting.lon(along.value(idx)) : Painting.lat(along.value(idx)))
                : (alongRow ? "i " : "j ") + idx;
        boolean masked = Float.isNaN(value) || session.maskZerosProperty().get() && value == 0;
        session.cursorInfoProperty().set(where + "  ·  " + z.label(k) + "  ·  "
                + (masked ? "land / missing" : Painting.value(value) + (v.units().isEmpty() ? "" : " " + v.units())));
        gc.setStroke(Color.web("#ffffff", 0.5));
        gc.setLineWidth(1);
        gc.strokeLine(Math.round(mouseX) + 0.5, TOP, Math.round(mouseX) + 0.5, TOP + ph);
    }
}
