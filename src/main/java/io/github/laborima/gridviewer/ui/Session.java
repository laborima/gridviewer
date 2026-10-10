package io.github.laborima.gridviewer.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import io.github.laborima.gridviewer.data.GridDataset;
import io.github.laborima.gridviewer.data.GridVariable;
import io.github.laborima.gridviewer.data.Overlays;
import io.github.laborima.gridviewer.data.SectionRequest;
import io.github.laborima.gridviewer.render.ColorMap;
import io.github.laborima.gridviewer.render.ColorMaps;
import io.github.laborima.gridviewer.render.ValueRange;

/**
 * What is being looked at, shared by the map, section and globe views. FX thread only.
 */
public final class Session {

    /** A vertical section: along a row (fixed j) or a column (fixed i). */
    public record SectionLine(SectionRequest.Orientation orientation, int index) {
    }

    private final ObjectProperty<GridDataset> dataset = new SimpleObjectProperty<>(this, "dataset");
    private final ObjectProperty<GridVariable> variable = new SimpleObjectProperty<>(this, "variable");
    private final IntegerProperty time = new SimpleIntegerProperty(this, "time");
    private final IntegerProperty level = new SimpleIntegerProperty(this, "level");
    private final ObjectProperty<ColorMap> colorMap = new SimpleObjectProperty<>(this, "colorMap", ColorMaps.get("thermal"));
    private final ObjectProperty<ValueRange> range = new SimpleObjectProperty<>(this, "range", new ValueRange(0, 1));
    private final BooleanProperty maskZeros = new SimpleBooleanProperty(this, "maskZeros");
    private final ObjectProperty<SectionLine> section = new SimpleObjectProperty<>(this, "section");
    private final ObservableList<Overlays.Point> points = FXCollections.observableArrayList();
    private final ObservableList<Overlays.Domain> domains = FXCollections.observableArrayList();
    private final StringProperty cursorInfo = new SimpleStringProperty(this, "cursorInfo", "");
    private final StringProperty readInfo = new SimpleStringProperty(this, "readInfo", "");
    private final IntegerProperty pendingReads = new SimpleIntegerProperty(this, "pendingReads");

    /** Set when a new variable is shown, so the first slice read sets the colour range. */
    boolean autoRangePending;

    public ObjectProperty<GridDataset> datasetProperty() {
        return dataset;
    }

    public GridDataset dataset() {
        return dataset.get();
    }

    public ObjectProperty<GridVariable> variableProperty() {
        return variable;
    }

    public GridVariable variable() {
        return variable.get();
    }

    public IntegerProperty timeProperty() {
        return time;
    }

    public IntegerProperty levelProperty() {
        return level;
    }

    public ObjectProperty<ColorMap> colorMapProperty() {
        return colorMap;
    }

    public ColorMap colorMap() {
        return colorMap.get();
    }

    public ObjectProperty<ValueRange> rangeProperty() {
        return range;
    }

    public ValueRange range() {
        return range.get();
    }

    public BooleanProperty maskZerosProperty() {
        return maskZeros;
    }

    public ObjectProperty<SectionLine> sectionProperty() {
        return section;
    }

    public ObservableList<Overlays.Point> points() {
        return points;
    }

    public ObservableList<Overlays.Domain> domains() {
        return domains;
    }

    public StringProperty cursorInfoProperty() {
        return cursorInfo;
    }

    public StringProperty readInfoProperty() {
        return readInfo;
    }

    public IntegerProperty pendingReadsProperty() {
        return pendingReads;
    }

    /** North up: grids stored south to north are drawn with their last row at the top. */
    public boolean flipRows() {
        GridVariable v = variable();
        return v == null || !v.yAxis().isDescending();
    }

    /** Shows a variable, choosing a colour map and asking for an automatic range. */
    public void show(GridVariable v) {
        section.set(null);
        time.set(0);
        level.set(0);
        if (v != null) {
            colorMap.set(ColorMaps.suggest(v.name(), v.longName(), v.units()));
            maskZeros.set(!v.declaresMissingValues() && v.hasCurvilinearCoordinates());
            double[] d = ColorMaps.defaultRange(v.name());
            autoRangePending = d == null;
            if (d != null) {
                range.set(new ValueRange(d[0], d[1]));
            }
        }
        variable.set(v);
    }

    /** Applies an automatic range computed from {@code values}, centred for diverging maps. */
    public void autoRange(float[] values) {
        ValueRange r = ValueRange.auto(values);
        range.set(ColorMaps.isDiverging(colorMap()) ? r.symmetric() : r);
    }
}
