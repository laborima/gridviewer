package io.github.laborima.gridviewer.ui;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

import io.github.laborima.gridviewer.data.GridDataset;
import io.github.laborima.gridviewer.data.GridVariable;
import io.github.laborima.gridviewer.data.LatestOnly;
import io.github.laborima.gridviewer.data.Slice;
import io.github.laborima.gridviewer.data.SliceRequest;
import io.github.laborima.gridviewer.data.Stride;
import io.github.laborima.gridviewer.data.Window;
import io.github.laborima.gridviewer.render.GlobeTexture;
import io.github.laborima.gridviewer.render.Rasterizer;

/**
 * The current level wrapped on a rotating globe, as the 2006 Java3D view did. The texture is
 * read once at a stride that fits it, so even huge grids cost a few megabytes.
 */
public final class GlobeView extends Region {

    private record Read(GridVariable variable, SliceRequest request) {
    }

    private record Field(GridVariable variable, Slice slice, float[][] lonLat) {
    }

    private static final int TEXTURE_WIDTH = 4096;
    private static final int TEXTURE_HEIGHT = 2048;

    private final Session session;
    private final Sphere sphere = new Sphere(1, 256);
    private final PhongMaterial material = new PhongMaterial(Color.web("#1a1e24"));
    private final SubScene scene3d;
    private final Rotate tilt = new Rotate(-18, Rotate.X_AXIS);
    private final Rotate spin = new Rotate(0, Rotate.Y_AXIS);
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Canvas overlay = new Canvas();
    private final Label hint = new Label();
    private final BooleanProperty autoRotate = new SimpleBooleanProperty(this, "autoRotate", true);
    private final AnimationTimer timer;

    private LatestOnly<Read, Field> loader;
    private Field field;
    private boolean active;
    private boolean dirty = true;
    private double pressX;
    private double pressY;
    private double pressSpin;
    private double pressTilt;

    public GlobeView(Session session) {
        this.session = session;
        getStyleClass().add("globe-view");

        material.setSpecularColor(Color.gray(0.12));
        sphere.setMaterial(material);
        sphere.getTransforms().addAll(tilt, spin);
        AmbientLight ambient = new AmbientLight(Color.gray(0.62));
        PointLight sun = new PointLight(Color.gray(0.55));
        sun.setTranslateX(-6);
        sun.setTranslateY(-4);
        sun.setTranslateZ(-8);
        Group world = new Group(sphere, ambient, sun);

        camera.setNearClip(0.01);
        camera.setFarClip(100);
        camera.setFieldOfView(30);
        camera.setTranslateZ(-4.6);

        scene3d = new SubScene(world, 100, 100, true, SceneAntialiasing.BALANCED);
        scene3d.setFill(Color.TRANSPARENT);
        scene3d.setCamera(camera);
        overlay.setMouseTransparent(true);
        hint.getStyleClass().add("globe-hint");
        hint.setMouseTransparent(true);
        getChildren().addAll(scene3d, overlay, hint);

        timer = new AnimationTimer() {
            private long last;

            @Override
            public void handle(long now) {
                if (last != 0) {
                    // One turn every 30 seconds, like the original.
                    spin.setAngle(spin.getAngle() - (now - last) / 1e9 * 12);
                }
                last = now;
            }

            @Override
            public void stop() {
                last = 0;
                super.stop();
            }
        };
        autoRotate.addListener((o, a, on) -> {
            if (on) {
                timer.start();
            } else {
                timer.stop();
            }
        });
        sceneProperty().addListener((o, a, b) -> updateTimer());

        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            pressX = e.getX();
            pressY = e.getY();
            pressSpin = spin.getAngle();
            pressTilt = tilt.getAngle();
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            autoRotate.set(false);
            spin.setAngle(pressSpin - (e.getX() - pressX) * 0.3);
            tilt.setAngle(Math.clamp(pressTilt + (e.getY() - pressY) * 0.3, -89, 89));
        });
        addEventHandler(ScrollEvent.SCROLL, e -> camera.setTranslateZ(
                Math.clamp(camera.getTranslateZ() * Math.pow(1.0015, -e.getDeltaY()), -14, -1.25)));

        session.datasetProperty().addListener((o, a, ds) -> attach(ds));
        session.variableProperty().addListener((o, a, b) -> invalidate());
        session.timeProperty().addListener((o, a, b) -> invalidate());
        session.levelProperty().addListener((o, a, b) -> invalidate());
        session.colorMapProperty().addListener((o, a, b) -> recolor());
        session.rangeProperty().addListener((o, a, b) -> recolor());
        session.maskZerosProperty().addListener((o, a, b) -> recolor());
    }

    public BooleanProperty autoRotateProperty() {
        return autoRotate;
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        scene3d.setWidth(w);
        scene3d.setHeight(h);
        if (overlay.getWidth() != w || overlay.getHeight() != h) {
            overlay.setWidth(w);
            overlay.setHeight(h);
            paintOverlay();
        }
        hint.resizeRelocate(0, 0, w, hint.prefHeight(w));
    }

    @Override
    protected double computePrefWidth(double height) {
        return 400;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 300;
    }

    /** The globe only reads data while its tab is shown. */
    public void setActive(boolean active) {
        this.active = active;
        updateTimer();
        if (active && dirty) {
            refresh();
        }
    }

    private void invalidate() {
        dirty = true;
        if (active) {
            refresh();
        }
    }

    /** Turns the globe so that a longitude faces the viewer. */
    public void faceLongitude(double lon) {
        spin.setAngle(-lon - 90);
    }

    public WritableImage snapshotImage() {
        return snapshot(null, null);
    }

    private void updateTimer() {
        if (autoRotate.get() && active && getScene() != null) {
            timer.start();
        } else {
            timer.stop();
        }
    }

    private void attach(GridDataset ds) {
        if (loader != null) {
            loader.cancel();
        }
        field = null;
        dirty = true;
        material.setDiffuseMap(null);
        loader = ds == null ? null : new LatestOnly<>(ds, r -> {
            Slice s = ds.readSlice(r.variable(), r.request());
            SliceRequest q = r.request();
            float[][] ll = r.variable().isRegularLonLat() ? null : r.variable().readLonLat(q.window(), q.stride());
            return new Field(r.variable(), s, ll);
        }, Platform::runLater);
    }

    /** Reads the texture for the current variable, level and time step. */
    public void refresh() {
        dirty = false;
        GridVariable v = session.variable();
        if (v == null || loader == null) {
            field = null;
            material.setDiffuseMap(null);
            paintOverlay();
            return;
        }
        Window full = Window.full(v.nx(), v.ny());
        int stride = Stride.forWindow(full, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        SliceRequest r = new SliceRequest(v.name(), session.timeProperty().get(), session.levelProperty().get(),
                full, stride);
        loader.request(new Read(v, r), f -> {
            field = f;
            recolor();
        }, e -> session.readInfoProperty().set("Globe read failed: " + e.getMessage()));
    }

    private void recolor() {
        paintOverlay();
        if (field == null) {
            return;
        }
        GridVariable v = field.variable();
        Slice s = field.slice();
        int[] colors = Rasterizer.colorize(s.values(), s.columns(), session.colorMap(), session.range(), false,
                session.maskZerosProperty().get());
        int[] tex;
        if (v.isRegularLonLat()) {
            tex = GlobeTexture.regular(colors, s.request().window(), s.request().stride(), v.xAxis(), v.yAxis(),
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
        } else if (field.lonLat() != null) {
            tex = GlobeTexture.scattered(colors, field.lonLat()[0], field.lonLat()[1], s.columns(),
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
        } else {
            // No geographic coordinates: stretch the index grid over the whole globe.
            int[] flipped = Rasterizer.colorize(s.values(), s.columns(), session.colorMap(), session.range(),
                    session.flipRows(), session.maskZerosProperty().get());
            material.setDiffuseColor(Color.WHITE);
            material.setDiffuseMap(Painting.image(flipped, s.columns()));
            return;
        }
        material.setDiffuseColor(Color.WHITE);
        material.setDiffuseMap(Painting.image(tex, TEXTURE_WIDTH));
    }

    private void paintOverlay() {
        var gc = overlay.getGraphicsContext2D();
        double w = overlay.getWidth();
        double h = overlay.getHeight();
        gc.clearRect(0, 0, w, h);
        GridVariable v = session.variable();
        if (v == null) {
            hint.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder(v.longName());
        if (v.levelAxis() != null && v.levels() > 1) {
            sb.append("\n").append(v.levelAxis().label(session.levelProperty().get()));
        }
        if (v.timeAxis() != null && v.times() > 1) {
            sb.append("  ·  ").append(v.timeAxis().label(session.timeProperty().get()));
        }
        if (!v.isRegularLonLat() && !v.hasCurvilinearCoordinates()) {
            sb.append("\nNo longitude/latitude in this file: the index grid is stretched over the globe.");
        }
        hint.setText(sb.toString());
        String units = v.units().isEmpty() ? "" : "  [" + v.units() + "]";
        if (w > 0 && h > 0) {
            Painting.colorBar(gc, w - 14, h - 14, session.colorMap(), session.range(), v.name() + units);
        }
    }
}
