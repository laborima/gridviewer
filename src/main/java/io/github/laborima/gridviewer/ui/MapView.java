package io.github.laborima.gridviewer.ui;

import java.util.Locale;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import io.github.laborima.gridviewer.data.Axis;
import io.github.laborima.gridviewer.data.GridDataset;
import io.github.laborima.gridviewer.data.GridVariable;
import io.github.laborima.gridviewer.data.LatestOnly;
import io.github.laborima.gridviewer.data.Overlays;
import io.github.laborima.gridviewer.data.SectionRequest;
import io.github.laborima.gridviewer.data.Slice;
import io.github.laborima.gridviewer.data.SliceRequest;
import io.github.laborima.gridviewer.data.Stride;
import io.github.laborima.gridviewer.data.Window;
import io.github.laborima.gridviewer.render.Rasterizer;

/**
 * The map: a horizontal slice drawn in grid index space, north up.
 *
 * <p>Two layers are kept. The <em>base</em> covers the whole grid at a stride that fits the
 * window; it is read once per level and time step. The <em>detail</em> covers what is visible
 * (plus a margin for panning) at the stride the current zoom needs, and is re-read a few tens
 * of milliseconds after the view stops moving. While it loads, the previous pixels are simply
 * stretched, so zooming always feels immediate, whatever the file size.
 */
public final class MapView extends Region {

    private record Read(GridVariable variable, SliceRequest request) {
    }

    private record Layer(GridVariable variable, Slice slice, WritableImage image) {
        boolean sameField(Layer other) {
            SliceRequest a = slice.request();
            SliceRequest b = other.slice.request();
            return other.variable == variable && a.time() == b.time() && a.level() == b.level();
        }
    }

    private static final double MAX_PIXELS_PER_CELL = 96;

    private final Session session;
    private final Canvas canvas = new Canvas();
    private final PauseTransition detailDelay = new PauseTransition(Duration.millis(60));
    private final ContextMenu menu = new ContextMenu();

    private LatestOnly<Read, Slice> baseLoader;
    private LatestOnly<Read, Slice> detailLoader;
    private LatestOnly<int[], double[]> lonLatLoader;
    private boolean baseLoading;
    private boolean detailLoading;

    private Layer base;
    private Layer detail;

    private double originI;
    private double originJ;
    private double scale = 1;
    private boolean fitted;

    private double pressX;
    private double pressY;
    private double pressOriginI;
    private double pressOriginJ;
    private boolean boxZoom;
    private boolean dragged;
    private double mouseX = Double.NaN;
    private double mouseY = Double.NaN;

    private Consumer<Session.SectionLine> onSection = s -> { };

    public MapView(Session session) {
        this.session = session;
        getStyleClass().add("map-view");
        getChildren().add(canvas);
        setFocusTraversable(true);
        detailDelay.setOnFinished(e -> requestDetail());

        session.datasetProperty().addListener((o, a, ds) -> attach(ds));
        session.variableProperty().addListener((o, a, v) -> {
            base = null;
            detail = null;
            fitted = false;
            requestBase();
        });
        session.timeProperty().addListener((o, a, b) -> requestBase());
        session.levelProperty().addListener((o, a, b) -> requestBase());
        session.colorMapProperty().addListener((o, a, b) -> recolor());
        session.rangeProperty().addListener((o, a, b) -> recolor());
        session.maskZerosProperty().addListener((o, a, b) -> recolor());
        session.sectionProperty().addListener((o, a, b) -> paint());
        session.points().addListener((javafx.collections.ListChangeListener<Overlays.Point>) c -> paint());
        session.domains().addListener((javafx.collections.ListChangeListener<Overlays.Domain>) c -> paint());

        addEventHandler(ScrollEvent.SCROLL, this::onScroll);
        addEventHandler(ZoomEvent.ZOOM, e -> zoomAt(e.getX(), e.getY(), e.getZoomFactor()));
        addEventHandler(MouseEvent.MOUSE_PRESSED, this::onPressed);
        addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onDragged);
        addEventHandler(MouseEvent.MOUSE_RELEASED, this::onReleased);
        addEventHandler(MouseEvent.MOUSE_MOVED, this::onMoved);
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            mouseX = Double.NaN;
            session.cursorInfoProperty().set("");
            paint();
        });
        addEventHandler(KeyEvent.KEY_PRESSED, this::onKey);
        setOnContextMenuRequested(e -> {
            buildMenu(e.getX(), e.getY());
            menu.show(this, e.getScreenX(), e.getScreenY());
        });
    }

    /** Called when the user asks for a vertical section from the context menu. */
    public void setOnSection(Consumer<Session.SectionLine> handler) {
        this.onSection = handler;
    }

    /** Grid window currently visible, or {@code null} when nothing is shown. */
    public Window visibleWindow() {
        GridVariable v = session.variable();
        double w = getWidth();
        double h = getHeight();
        if (v == null || w <= 0 || h <= 0) {
            return null;
        }
        double ia = gridI(0);
        double ib = gridI(w);
        double ja = gridJ(0);
        double jb = gridJ(h);
        int i0 = (int) Math.floor(Math.min(ia, ib));
        int i1 = (int) Math.floor(Math.max(ia, ib));
        int j0 = (int) Math.floor(Math.min(ja, jb));
        int j1 = (int) Math.floor(Math.max(ja, jb));
        if (i1 < 0 || j1 < 0 || i0 >= v.nx() || j0 >= v.ny()) {
            return null;
        }
        return new Window(i0, j0, i1, j1).clamp(v.nx(), v.ny());
    }

    public WritableImage snapshot() {
        return canvas.snapshot(null, null);
    }

    public void fit() {
        GridVariable v = session.variable();
        double w = getWidth();
        double h = getHeight();
        if (v == null || w <= 0 || h <= 0) {
            return;
        }
        scale = Math.min(w / v.nx(), h / v.ny()) * 0.98;
        originI = (v.nx() - w / scale) / 2;
        originJ = (v.ny() - h / scale) / 2;
        fitted = true;
        viewChanged();
    }

    /** Zooms so that a grid window fills the view. */
    public void showWindow(Window target) {
        GridVariable v = session.variable();
        double w = getWidth();
        double h = getHeight();
        if (v == null || w <= 0 || h <= 0) {
            return;
        }
        scale = Math.min(MAX_PIXELS_PER_CELL, Math.min(w / target.width(), h / target.height()));
        originI = target.i0() + target.width() / 2.0 - w / 2 / scale;
        originJ = target.j0() + target.height() / 2.0 - h / 2 / scale;
        fitted = true;
        viewChanged();
    }

    public void zoomAtCenter(double factor) {
        zoomAt(getWidth() / 2, getHeight() / 2, factor);
    }

    // ---------------------------------------------------------------- data

    private void attach(GridDataset ds) {
        if (baseLoader != null) {
            baseLoader.cancel();
            detailLoader.cancel();
            lonLatLoader.cancel();
        }
        base = null;
        detail = null;
        baseLoading = false;
        detailLoading = false;
        updatePending();
        if (ds == null) {
            baseLoader = null;
            detailLoader = null;
            lonLatLoader = null;
            paint();
            return;
        }
        baseLoader = new LatestOnly<>(ds, r -> ds.readSlice(r.variable(), r.request()), Platform::runLater);
        detailLoader = new LatestOnly<>(ds, r -> ds.readSlice(r.variable(), r.request()), Platform::runLater);
        lonLatLoader = new LatestOnly<>(ds, ij -> {
            GridVariable v = session.variable();
            return v == null ? null : v.lonLatAt(ij[0], ij[1]);
        }, Platform::runLater);
    }

    private void requestBase() {
        GridVariable v = session.variable();
        if (v == null || baseLoader == null) {
            paint();
            return;
        }
        double os = outputScale();
        double w = Math.max(getWidth(), 960) * os;
        double h = Math.max(getHeight(), 540) * os;
        Window full = Window.full(v.nx(), v.ny());
        int stride = Stride.forWindow(full, w, h);
        SliceRequest r = new SliceRequest(v.name(), session.timeProperty().get(), session.levelProperty().get(),
                full, stride);
        baseLoading = true;
        updatePending();
        baseLoader.request(new Read(v, r), slice -> {
            baseLoading = false;
            updatePending();
            if (session.autoRangePending) {
                session.autoRangePending = false;
                session.autoRange(slice.values());
            }
            base = layer(v, slice);
            if (detail != null && !detail.sameField(base)) {
                detail = null;
            }
            report(v, slice);
            if (!fitted) {
                fit();
            } else {
                paint();
                requestDetail();
            }
        }, this::failed);
    }

    private void requestDetail() {
        GridVariable v = session.variable();
        Window visible = visibleWindow();
        if (v == null || base == null || visible == null || detailLoader == null) {
            return;
        }
        double os = outputScale();
        int stride = Stride.forWindow(visible, getWidth() * os, getHeight() * os);
        SliceRequest baseRequest = base.slice().request();
        if (stride >= baseRequest.stride()) {
            if (detail != null) {
                detail = null;
                paint();
            }
            detailLoader.cancel();
            detailLoading = false;
            updatePending();
            return;
        }
        if (detail != null && detail.sameField(base) && detail.slice().request().stride() == stride
                && covers(detail.slice().request().window(), visible)) {
            return;
        }
        // Read a margin of a quarter screen around the view so that small pans cost nothing.
        int mi = visible.width() / 4;
        int mj = visible.height() / 4;
        Window window = new Window(visible.i0() - mi, visible.j0() - mj, visible.i1() + mi, visible.j1() + mj)
                .clamp(v.nx(), v.ny()).alignTo(stride);
        SliceRequest r = new SliceRequest(v.name(), baseRequest.time(), baseRequest.level(), window, stride);
        detailLoading = true;
        updatePending();
        detailLoader.request(new Read(v, r), slice -> {
            detailLoading = false;
            updatePending();
            Layer l = layer(v, slice);
            if (base != null && l.sameField(base)) {
                detail = l;
                report(v, slice);
                paint();
            }
        }, this::failed);
    }

    private static boolean covers(Window outer, Window inner) {
        return outer.i0() <= inner.i0() && outer.j0() <= inner.j0() && outer.i1() >= inner.i1() && outer.j1() >= inner.j1();
    }

    private Layer layer(GridVariable v, Slice slice) {
        int[] argb = Rasterizer.colorize(slice.values(), slice.columns(), session.colorMap(), session.range(),
                session.flipRows(), session.maskZerosProperty().get());
        return new Layer(v, slice, Painting.image(argb, slice.columns()));
    }

    private void recolor() {
        if (base != null) {
            base = layer(base.variable(), base.slice());
        }
        if (detail != null) {
            detail = layer(detail.variable(), detail.slice());
        }
        paint();
    }

    private void report(GridVariable v, Slice slice) {
        SliceRequest r = slice.request();
        long level = (long) v.nx() * v.ny() * Float.BYTES;
        session.readInfoProperty().set(String.format(Locale.ROOT,
                "stride %d  ·  %,d points  ·  %s read in %d ms  ·  full level %s",
                r.stride(), r.samples(), Painting.bytes(slice.bytes()), slice.readNanos() / 1_000_000,
                Painting.bytes(level)));
    }

    private void failed(Throwable e) {
        baseLoading = false;
        detailLoading = false;
        updatePending();
        session.readInfoProperty().set("Read failed: " + e.getMessage());
    }

    private void updatePending() {
        session.loading("map-base", baseLoading);
        session.loading("map-detail", detailLoading);
    }

    private double outputScale() {
        return getScene() != null && getScene().getWindow() != null ? getScene().getWindow().getOutputScaleX() : 1;
    }

    // ---------------------------------------------------------------- coordinates

    private double screenX(double i) {
        return (i - originI) * scale;
    }

    private double screenY(double j) {
        return session.flipRows() ? getHeight() - (j - originJ) * scale : (j - originJ) * scale;
    }

    private double gridI(double x) {
        return originI + x / scale;
    }

    private double gridJ(double y) {
        return session.flipRows() ? originJ + (getHeight() - y) / scale : originJ + y / scale;
    }

    // ---------------------------------------------------------------- interaction

    private void zoomAt(double x, double y, double factor) {
        GridVariable v = session.variable();
        if (v == null || factor <= 0) {
            return;
        }
        double gi = gridI(x);
        double gj = gridJ(y);
        double fitScale = Math.min(getWidth() / v.nx(), getHeight() / v.ny());
        scale = Math.clamp(scale * factor, fitScale * 0.5, MAX_PIXELS_PER_CELL);
        originI = gi - x / scale;
        originJ = session.flipRows() ? gj - (getHeight() - y) / scale : gj - y / scale;
        viewChanged();
    }

    private void onScroll(ScrollEvent e) {
        if (e.getDeltaY() == 0 || e.getTouchCount() > 1) {
            return;
        }
        zoomAt(e.getX(), e.getY(), Math.pow(1.0018, e.getDeltaY() * (e.isInertia() ? 0.5 : 1)));
        e.consume();
    }

    private void onPressed(MouseEvent e) {
        requestFocus();
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        pressX = e.getX();
        pressY = e.getY();
        pressOriginI = originI;
        pressOriginJ = originJ;
        boxZoom = e.isShiftDown();
        dragged = false;
        if (e.getClickCount() == 2) {
            zoomAt(e.getX(), e.getY(), e.isAltDown() ? 0.5 : 2);
        }
    }

    private void onDragged(MouseEvent e) {
        if (!e.isPrimaryButtonDown()) {
            return;
        }
        dragged = true;
        mouseX = e.getX();
        mouseY = e.getY();
        if (boxZoom) {
            paint();
            return;
        }
        originI = pressOriginI - (e.getX() - pressX) / scale;
        double dj = (e.getY() - pressY) / scale;
        originJ = session.flipRows() ? pressOriginJ + dj : pressOriginJ - dj;
        setCursor(javafx.scene.Cursor.CLOSED_HAND);
        viewChanged();
    }

    private void onReleased(MouseEvent e) {
        setCursor(javafx.scene.Cursor.DEFAULT);
        if (boxZoom && dragged) {
            double w = Math.abs(e.getX() - pressX);
            double h = Math.abs(e.getY() - pressY);
            if (w > 6 && h > 6) {
                double ci = gridI((e.getX() + pressX) / 2);
                double cj = gridJ((e.getY() + pressY) / 2);
                double target = Math.min(MAX_PIXELS_PER_CELL, scale * Math.min(getWidth() / w, getHeight() / h));
                scale = target;
                originI = ci - getWidth() / 2 / scale;
                originJ = cj - getHeight() / 2 / scale;
                viewChanged();
            }
        }
        boxZoom = false;
        paint();
    }

    private void onMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        updateCursorInfo();
        if (session.points().size() + session.domains().size() > 0 || boxZoom) {
            paint();
        }
    }

    private void onKey(KeyEvent e) {
        double step = 0.15;
        switch (e.getCode()) {
            case PLUS, EQUALS, ADD -> zoomAtCenter(1.5);
            case MINUS, SUBTRACT -> zoomAtCenter(1 / 1.5);
            case DIGIT0, NUMPAD0, HOME -> fit();
            case LEFT -> pan(-step * getWidth(), 0);
            case RIGHT -> pan(step * getWidth(), 0);
            case UP -> pan(0, -step * getHeight());
            case DOWN -> pan(0, step * getHeight());
            default -> {
                return;
            }
        }
        e.consume();
    }

    private void pan(double dx, double dy) {
        originI += dx / scale;
        originJ += (session.flipRows() ? -dy : dy) / scale;
        viewChanged();
    }

    private void viewChanged() {
        paint();
        updateCursorInfo();
        detailDelay.playFromStart();
    }

    private void buildMenu(double x, double y) {
        menu.getItems().clear();
        GridVariable v = session.variable();
        if (v == null) {
            return;
        }
        int i = (int) Math.floor(gridI(x));
        int j = (int) Math.floor(gridJ(y));
        boolean inside = i >= 0 && j >= 0 && i < v.nx() && j < v.ny();
        boolean has3d = v.levels() > 1;
        MenuItem row = new MenuItem("Vertical section along this row  (j = " + j + ")");
        row.setDisable(!inside || !has3d);
        row.setOnAction(a -> onSection.accept(new Session.SectionLine(SectionRequest.Orientation.ALONG_ROW, j)));
        MenuItem col = new MenuItem("Vertical section along this column  (i = " + i + ")");
        col.setDisable(!inside || !has3d);
        col.setOnAction(a -> onSection.accept(new Session.SectionLine(SectionRequest.Orientation.ALONG_COLUMN, i)));
        MenuItem in = new MenuItem("Zoom in here");
        in.setOnAction(a -> zoomAt(x, y, 2));
        MenuItem reset = new MenuItem("Zoom to fit");
        reset.setOnAction(a -> fit());
        menu.getItems().addAll(row, col, new SeparatorMenuItem(), in, reset);
    }

    private void updateCursorInfo() {
        GridVariable v = session.variable();
        if (v == null || Double.isNaN(mouseX)) {
            return;
        }
        int i = (int) Math.floor(gridI(mouseX));
        int j = (int) Math.floor(gridJ(mouseY));
        if (i < 0 || j < 0 || i >= v.nx() || j >= v.ny()) {
            session.cursorInfoProperty().set("");
            return;
        }
        double value = Double.NaN;
        if (detail != null && detail.slice().request().window().contains(i, j)) {
            value = detail.slice().valueAt(i, j);
        } else if (base != null) {
            value = base.slice().valueAt(i, j);
        }
        boolean masked = Double.isNaN(value) || session.maskZerosProperty().get() && value == 0;
        String units = v.units().isEmpty() ? "" : " " + v.units();
        String head = String.format(Locale.ROOT, "i %d  j %d", i, j);
        String tail = masked ? "land / missing" : Painting.value(value) + units;
        if (v.isRegularLonLat()) {
            session.cursorInfoProperty().set(head + "  ·  " + Painting.lon(v.xAxis().value(i)) + "  "
                    + Painting.lat(v.yAxis().value(j)) + "  ·  " + tail);
        } else if (v.hasCurvilinearCoordinates() && lonLatLoader != null) {
            session.cursorInfoProperty().set(head + "  ·  " + tail);
            lonLatLoader.request(new int[] {i, j}, ll -> {
                if (ll != null) {
                    session.cursorInfoProperty().set(head + "  ·  " + Painting.lon(ll[0]) + "  "
                            + Painting.lat(ll[1]) + "  ·  " + tail);
                }
            }, err -> { });
        } else {
            session.cursorInfoProperty().set(head + "  ·  " + tail);
        }
    }

    // ---------------------------------------------------------------- painting

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (canvas.getWidth() != w || canvas.getHeight() != h) {
            boolean wasEmpty = canvas.getWidth() == 0;
            canvas.setWidth(w);
            canvas.setHeight(h);
            if (!fitted && base != null) {
                fit();
            } else {
                paint();
                if (wasEmpty) {
                    requestBase();
                } else {
                    detailDelay.playFromStart();
                }
            }
        }
    }

    void paint() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.setFill(Painting.BACKGROUND);
        gc.fillRect(0, 0, w, h);
        GridVariable v = session.variable();
        if (v == null || base == null) {
            return;
        }
        double gx0 = screenX(0);
        double gx1 = screenX(v.nx());
        double gy0 = Math.min(screenY(0), screenY(v.ny()));
        double gy1 = Math.max(screenY(0), screenY(v.ny()));
        gc.save();
        gc.beginPath();
        gc.rect(gx0, gy0, gx1 - gx0, gy1 - gy0);
        gc.clip();
        gc.setImageSmoothing(false);
        drawLayer(gc, base);
        if (detail != null && detail.sameField(base)) {
            drawLayer(gc, detail);
        }
        gc.restore();
        gc.setStroke(Color.web("#ffffff", 0.18));
        gc.setLineWidth(1);
        gc.strokeRect(Math.round(gx0) + 0.5, Math.round(gy0) + 0.5, Math.round(gx1 - gx0) - 1, Math.round(gy1 - gy0) - 1);

        if (v.isRegularLonLat()) {
            drawGraticule(gc, v, Math.max(0, gx0), Math.max(0, gy0), Math.min(w, gx1), Math.min(h, gy1));
        }
        drawDomains(gc);
        drawPoints(gc);
        drawSectionLine(gc, v);
        drawBox(gc);
        String units = v.units().isEmpty() ? "" : "  [" + v.units() + "]";
        Painting.colorBar(gc, w - 14, h - 14, session.colorMap(), session.range(), v.name() + units);
    }

    private void drawLayer(GraphicsContext gc, Layer layer) {
        SliceRequest r = layer.slice().request();
        Window win = r.window();
        int s = r.stride();
        double x0 = screenX(win.i0());
        double x1 = screenX(win.i0() + (double) r.columns() * s);
        double ya = screenY(win.j0());
        double yb = screenY(win.j0() + (double) r.rows() * s);
        gc.drawImage(layer.image(), x0, Math.min(ya, yb), x1 - x0, Math.abs(yb - ya));
    }

    /** Longitude and latitude lines, clipped to the grid area (left, top, right, bottom). */
    private void drawGraticule(GraphicsContext gc, GridVariable v, double left, double top, double right,
                               double bottom) {
        Window vis = visibleWindow();
        if (vis == null) {
            return;
        }
        gc.setLineWidth(1);
        gc.setFont(Painting.SMALL);
        gc.setTextBaseline(VPos.TOP);
        Axis x = v.xAxis();
        double lonSpan = Math.abs(x.value(vis.i1()) - x.value(vis.i0()));
        double lonStep = niceStep(lonSpan);
        double lonStart = Math.ceil(Math.min(x.value(vis.i0()), x.value(vis.i1())) / lonStep) * lonStep;
        for (double lon = lonStart; lon <= Math.max(x.value(vis.i0()), x.value(vis.i1())); lon += lonStep) {
            double fi = Painting.fractionalIndex(x.values(), lon);
            if (Double.isNaN(fi)) {
                continue;
            }
            double sx = Math.round(screenX(fi + 0.5)) + 0.5;
            gc.setStroke(Color.web("#ffffff", 0.10));
            gc.strokeLine(sx, top, sx, bottom);
            gc.setTextAlign(TextAlignment.CENTER);
            Painting.haloText(gc, Painting.lon(lon), sx, top + 6);
        }
        Axis y = v.yAxis();
        double latSpan = Math.abs(y.value(vis.j1()) - y.value(vis.j0()));
        double latStep = niceStep(latSpan);
        double latStart = Math.ceil(Math.min(y.value(vis.j0()), y.value(vis.j1())) / latStep) * latStep;
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        for (double lat = latStart; lat <= Math.max(y.value(vis.j0()), y.value(vis.j1())); lat += latStep) {
            double fj = Painting.fractionalIndex(y.values(), lat);
            if (Double.isNaN(fj)) {
                continue;
            }
            double sy = Math.round(screenY(fj + 0.5)) + 0.5;
            gc.setStroke(Color.web("#ffffff", 0.10));
            gc.strokeLine(left, sy, right, sy);
            Painting.haloText(gc, Painting.lat(lat), left + 6, sy);
        }
        gc.setTextBaseline(VPos.BASELINE);
    }

    private static double niceStep(double span) {
        double[] steps = {90, 60, 30, 20, 10, 5, 2, 1, 0.5, 0.25, 0.1, 0.05, 0.02, 0.01};
        for (double s : steps) {
            if (span / s >= 3) {
                return s;
            }
        }
        return 0.01;
    }

    private void drawDomains(GraphicsContext gc) {
        if (session.domains().isEmpty()) {
            return;
        }
        gc.setLineWidth(1);
        gc.setFont(Painting.SMALL);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        for (Overlays.Domain d : session.domains()) {
            Window r = d.window();
            double x0 = screenX(r.i0());
            double x1 = screenX(r.i1() + 1);
            double y0 = Math.min(screenY(r.j0()), screenY(r.j1() + 1));
            double y1 = Math.max(screenY(r.j0()), screenY(r.j1() + 1));
            gc.setStroke(Color.web("#7CFFB2", 0.8));
            gc.strokeRect(x0 + 0.5, y0 + 0.5, x1 - x0, y1 - y0);
            if (x1 - x0 > 26 && y1 - y0 > 16) {
                gc.setFill(Color.web("#7CFFB2"));
                gc.fillText(Integer.toString(d.rank()), x0 + 3, y0 + 2);
            }
        }
    }

    private void drawPoints(GraphicsContext gc) {
        gc.setFont(Painting.SMALL);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        for (Overlays.Point p : session.points()) {
            double x = screenX(p.i() + 0.5);
            double y = screenY(p.j() + 0.5);
            gc.setFill(Painting.ACCENT);
            gc.fillOval(x - 4, y - 4, 8, 8);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(x - 4, y - 4, 8, 8);
            String label = p.label().isEmpty() ? p.i() + "," + p.j() : p.label();
            Painting.haloText(gc, label, x + 8, y);
        }
        gc.setTextBaseline(VPos.BASELINE);
    }

    private void drawSectionLine(GraphicsContext gc, GridVariable v) {
        Session.SectionLine s = session.sectionProperty().get();
        if (s == null) {
            return;
        }
        gc.setStroke(Painting.HIGHLIGHT);
        gc.setLineWidth(2);
        gc.setLineDashes(8, 5);
        if (s.orientation() == SectionRequest.Orientation.ALONG_ROW) {
            double y = screenY(s.index() + 0.5);
            gc.strokeLine(screenX(0), y, screenX(v.nx()), y);
        } else {
            double x = screenX(s.index() + 0.5);
            gc.strokeLine(x, screenY(0), x, screenY(v.ny()));
        }
        gc.setLineDashes();
    }

    private void drawBox(GraphicsContext gc) {
        if (!boxZoom || !dragged || Double.isNaN(mouseX)) {
            return;
        }
        double x = Math.min(pressX, mouseX);
        double y = Math.min(pressY, mouseY);
        double w = Math.abs(mouseX - pressX);
        double h = Math.abs(mouseY - pressY);
        gc.setFill(Color.web("#4cc9f0", 0.15));
        gc.fillRect(x, y, w, h);
        gc.setStroke(Painting.ACCENT);
        gc.setLineWidth(1);
        gc.strokeRect(x + 0.5, y + 0.5, w, h);
    }
}
