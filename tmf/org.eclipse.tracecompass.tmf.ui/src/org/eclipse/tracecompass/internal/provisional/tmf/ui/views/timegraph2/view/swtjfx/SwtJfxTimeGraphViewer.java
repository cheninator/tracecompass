/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.ui.views.timegraph2.view.swtjfx;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.control.TimeGraphModelControl;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.provider.ITimeGraphModelRenderProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.states.TimeGraphStateInterval;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.states.TimeGraphStateRender;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.tree.TimeGraphTreeRender;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.view.TimeGraphModelView;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.embed.swt.FXCanvas;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

/**
 * Viewer for the {@link SwtJfxTimeGraphView}, encapsulating all the view's
 * controls.
 *
 * Its contents consist of:
 *
 * TODO update this to its final form
 * <pre>
 * SashForm fBaseControl (parent is passed from the view)
 *  + FXCanvas
 *  |   + ScrollPane
 *  |       + TreeView (?), contains the list of threads
 *  + FXCanvas
 *      + ScrollPane, will contain the time graph area
 *          + Pane, gets resized to very large horizontal size to represent the whole trace range
 *             + Canvas, canvas children are tiled on the Pane to show the content of one Render each
 *             + Canvas
 *             +  ...
 * </pre>
 *
 * Both ScrolledPanes's vertical scrollbars are bound together, so that they
 * scroll together.
 *
 * @author Alexandre Montplaisir
 */
public class SwtJfxTimeGraphViewer extends TimeGraphModelView {

    private static final double MAX_CANVAS_WIDTH = 2000.0;
    private static final double MAX_CANVAS_HEIGHT = 2000.0;

    // ------------------------------------------------------------------------
    // Style definitions
    // (Could eventually be moved to separate .css file?)
    // ------------------------------------------------------------------------

    private static final Color BACKGROUD_LINES_COLOR = checkNotNull(Color.LIGHTBLUE);
    private static final String BACKGROUND_STYLE = "-fx-background-color: rgba(255, 255, 255, 255);"; //$NON-NLS-1$

    private static final double SELECTION_STROKE_WIDTH = 1;
    private static final Color SELECTION_STROKE_COLOR = checkNotNull(Color.BLUE);
    private static final Color SELECTION_FILL_COLOR = checkNotNull(Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.4));

    private static final int LABEL_SIDE_MARGIN = 10;

    // ------------------------------------------------------------------------
    // Class fields
    // ------------------------------------------------------------------------

    private final SelectionContext fSelectionCtx = new SelectionContext();
    private final ScrollingContext fScrollingCtx = new ScrollingContext();

    private final LatestTaskExecutor fTaskExecutor = new LatestTaskExecutor();

    private final SashForm fBaseControl;

    private final FXCanvas fTreeFXCanvas;
    private final FXCanvas fTimeGraphFXCanvas;

    private final Pane fTreePane;
    private final ScrollPane fTreeScrollPane;
    private final Pane fTimeGraphPane;
    private final ScrollPane fTimeGraphScrollPane;

    /*
     * Children of the time graph pane are split into groups, so we can easily
     * redraw or add only some of them.
     */
    private final Group fTimeGraphStatesLayer;
    private final Group fTimeGraphSelectionLayer;
    // TODO Layers for markers, arrows

    private final Rectangle fSelectionRect;
    private final Rectangle fOngoingSelectionRect;

    /**
     * Height of individual entries (text + states), including padding.
     *
     * TODO Make this configurable (vertical zoom feature)
     */
    private static final double ENTRY_HEIGHT = 20;


    /** Current zoom level */
    private double fNanosPerPixel = 1.0;


    /**
     * Constructor
     *
     * @param parent
     *            Parent SWT composite
     */
    public SwtJfxTimeGraphViewer(Composite parent, TimeGraphModelControl control) {
        super(control);

        // TODO Convert this sash to JavaFX too?
        fBaseControl = new SashForm(parent, SWT.NONE);

        fTreeFXCanvas = new FXCanvas(fBaseControl, SWT.NONE);
        fTimeGraphFXCanvas = new FXCanvas(fBaseControl, SWT.NONE);

        // TODO Base on time-alignment
        fBaseControl.setWeights(new int[] { 15, 85 });

        // --------------------------------------------------------------------
        // Prepare the tree part's scene graph
        // --------------------------------------------------------------------

        fTreePane = new Pane();

        fTreeScrollPane = new ScrollPane(fTreePane);
        /* We only show the time graph's vertical scrollbar */
        fTreeScrollPane.setVbarPolicy(ScrollBarPolicy.NEVER);
        fTreeScrollPane.setHbarPolicy(ScrollBarPolicy.ALWAYS);

        // --------------------------------------------------------------------
        // Prepare the time graph's part scene graph
        // --------------------------------------------------------------------

        fSelectionRect = new Rectangle();
        fOngoingSelectionRect = new Rectangle();

        Stream.of(fSelectionRect, fOngoingSelectionRect).forEach(rect -> {
            rect.setStroke(SELECTION_STROKE_COLOR);
            rect.setStrokeWidth(SELECTION_STROKE_WIDTH);
            rect.setStrokeLineCap(StrokeLineCap.ROUND);
            rect.setFill(SELECTION_FILL_COLOR);
        });

        fTimeGraphStatesLayer = new Group();
        fTimeGraphSelectionLayer = new Group(fSelectionRect, fOngoingSelectionRect);

        fTimeGraphPane = new Pane(fTimeGraphStatesLayer, fTimeGraphSelectionLayer);
        fTimeGraphPane.setStyle(BACKGROUND_STYLE);
        fTimeGraphPane.addEventHandler(MouseEvent.MOUSE_PRESSED, fSelectionCtx.fMousePressedEventHandler);
        fTimeGraphPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, fSelectionCtx.fMouseDraggedEventHandler);
        fTimeGraphPane.addEventHandler(MouseEvent.MOUSE_RELEASED, fSelectionCtx.fMouseReleasedEventHandler);

        /*
         * We control the width of the time graph pane programatically, so
         * ensure that calls to setPrefWidth set the actual width right away.
         */
        fTimeGraphPane.minWidthProperty().bind(fTimeGraphPane.prefWidthProperty());
        fTimeGraphPane.maxWidthProperty().bind(fTimeGraphPane.prefWidthProperty());

        /*
         * Ensure the time graph pane is always exactly the same vertical size
         * as the tree pane, so they remain aligned.
         */
        fTimeGraphPane.minHeightProperty().bind(fTreePane.heightProperty());
        fTimeGraphPane.prefHeightProperty().bind(fTreePane.heightProperty());
        fTimeGraphPane.maxHeightProperty().bind(fTreePane.heightProperty());

        fTimeGraphScrollPane = new ScrollPane(fTimeGraphPane);
        fTimeGraphScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
        fTimeGraphScrollPane.setHbarPolicy(ScrollBarPolicy.ALWAYS);

//        fTimeGraphScrollPane.viewportBoundsProperty().addListener(fScrollingCtx.fHScrollChangeListener);
        fTimeGraphScrollPane.setOnMouseEntered(fScrollingCtx.fMouseEnteredEventHandler);
        fTimeGraphScrollPane.setOnMouseExited(fScrollingCtx.fMouseExitedEventHandler);
        fTimeGraphScrollPane.hvalueProperty().addListener(fScrollingCtx.fHScrollChangeListener);

        /* Synchronize the two scrollpanes' vertical scroll bars together */
        fTreeScrollPane.vvalueProperty().bindBidirectional(fTimeGraphScrollPane.vvalueProperty());

        // --------------------------------------------------------------------
        // Hook the parts into the SWT window
        // --------------------------------------------------------------------

        fTreeFXCanvas.setScene(new Scene(fTreeScrollPane));
        fTimeGraphFXCanvas.setScene(new Scene(fTimeGraphScrollPane));

        /*
         * Initially populate the viewer with the context of the current trace.
         */
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        getControl().initializeForTrace(trace);
    }

    // ------------------------------------------------------------------------
    // Test accessors
    // ------------------------------------------------------------------------

    @VisibleForTesting
    protected Pane getTimeGraphPane() {
        return fTimeGraphPane;
    }

    @VisibleForTesting
    protected ScrollPane getTimeGraphScrollPane() {
        return fTimeGraphScrollPane;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    public void disposeImpl() {
    }

    @Override
    public void clear() {
        // TODO
    }

    @Override
    public void seekVisibleRange(long visibleWindowStartTime, long visibleWindowEndTime) {
        final long fullTimeGraphStart = getControl().getFullTimeGraphStartTime();
        final long fullTimeGraphEnd = getControl().getFullTimeGraphEndTime();

        /* Update the zoom level */
        long windowTimeRange = visibleWindowEndTime - visibleWindowStartTime;
        double timeGraphWidth = fTimeGraphScrollPane.getWidth();
        fNanosPerPixel = windowTimeRange / timeGraphWidth;

        double timeGraphAreaWidth = timestampToPaneXPos(fullTimeGraphEnd) - timestampToPaneXPos(fullTimeGraphStart);
        if (timeGraphAreaWidth < 1.0) {
            // FIXME
            return;
        }

        double newValue;
        if (visibleWindowStartTime == fullTimeGraphStart) {
            newValue = fTimeGraphScrollPane.getHmin();
        } else if (visibleWindowEndTime == fullTimeGraphEnd) {
            newValue = fTimeGraphScrollPane.getHmax();
        } else {
            // FIXME Not aligned perfectly yet, see how the scrolling
            // listener does it?
            long targetTs = (visibleWindowStartTime + visibleWindowEndTime) / 2;
            double xPos = timestampToPaneXPos(targetTs);
            newValue = xPos / timeGraphAreaWidth;
        }

        fTimeGraphPane.setPrefWidth(timeGraphAreaWidth);
        fTimeGraphScrollPane.setHvalue(newValue);

        paintArea(visibleWindowStartTime, visibleWindowEndTime);
    }

    private void paintArea(long windowStartTime, long windowEndTime) {
        final long fullTimeGraphStart = getControl().getFullTimeGraphStartTime();
        final long fullTimeGraphEnd = getControl().getFullTimeGraphEndTime();

        /*
         * Get the current target width of the viewer, so we know at which
         * resolution we must do state system queries.
         *
         * Yes! We can query the size of visible components outside of the UI
         * thread! Praise the JavaFX!
         */
        long treePaneWidth = Math.round(fTreeScrollPane.getWidth());

        long windowTimeRange = windowEndTime - windowStartTime;

        /*
         * Request the needed renders and prepare the corresponding
         * canvases. We target at most one "window width" before and
         * after the current window, clamped by the trace's start and
         * end.
         */
        final long renderingStartTime = Math.max(fullTimeGraphStart, windowStartTime - windowTimeRange);
        final long renderingEndTime = Math.min(fullTimeGraphEnd, windowEndTime + windowTimeRange);
        final long renderTimeRange = (long) (MAX_CANVAS_WIDTH * fNanosPerPixel);
        final long resolution = Math.max(1, Math.round(fNanosPerPixel));

        if (renderTimeRange < 1) {
            return;
        }

        /* Get visible bounds */
        Bounds paneBounds = fTimeGraphScrollPane.localToScene(fTimeGraphScrollPane.getBoundsInParent());
        System.out.println("Visible X bounds: " + paneBounds.getMinX() + ", " + paneBounds.getMaxX());


        Bounds bounds = fTimeGraphScrollPane.getViewportBounds();
        long minX = -1 * (long) bounds.getMinX();
        long maxX = -1* (long) bounds.getMaxX();
        System.out.println("Visible X bounds: " + minX + "-" + maxX);

        Task<@Nullable Void> task = new Task<@Nullable Void>() {
            @Override
            protected @Nullable Void call() {
                ITimeGraphModelRenderProvider renderProvider = getControl().getModelRenderProvider();
                TimeGraphTreeRender treeRender = renderProvider.getTreeRender(windowStartTime, windowEndTime);

                /*
                 * Get the vertical "slices" of state renders.
                 *
                 * FIXME iterate/peek/allMatch can be replaced by the more
                 * intuitive iterate/takeWhile/map/collect with Java 9.
                 */
                List<List<TimeGraphStateRender>> stateRenders = new ArrayList<>();
                LongStream
                        .iterate(renderingStartTime, i -> i + renderTimeRange)
                        .peek(renderStart -> {
                            long renderEnd = Math.min(renderStart + renderTimeRange, renderingEndTime);

                            System.out.printf("requesting render from %,d to %,d, resolution=%d%n",
                                    renderStart, renderEnd, resolution);

                            List<TimeGraphStateRender> render = renderProvider.getStateRenders(treeRender, resolution);
                            stateRenders.add(render);
                        })
                        .allMatch(i -> i <= renderingEndTime);

                /* Prepare the tree part */
                Node treeContents = prepareTreeContents(treeRender, treePaneWidth);

                /* Prepare the time graph part */
                Node timeGraphContents = prepareTimeGraphContents(stateRenders);

                if (Thread.currentThread().isInterrupted()) {
                    /* Task was cancelled, no need to update the UI */
                    System.out.println("job was cancelled before it could end");
                    return null;
                }

                /* Update the view! */
                // Display.getDefault().syncExec( () -> {
                Platform.runLater(() -> {
                    fTreePane.getChildren().clear();
                    fTreePane.getChildren().add(treeContents);

                    fTimeGraphStatesLayer.getChildren().clear();
                    fTimeGraphStatesLayer.getChildren().add(timeGraphContents);
                });

                return null;
            }
        };

        fTaskExecutor.schedule(task);
    }

    @Override
    public void drawSelection(long selectionStartTime, long selectionEndTime) {
        double xStart = timestampToPaneXPos(selectionStartTime);
        double xEnd = timestampToPaneXPos(selectionEndTime);
        double xWidth = xEnd - xStart;

        fSelectionRect.setX(xStart);
        fSelectionRect.setY(0);
        fSelectionRect.setWidth(xWidth);
        fSelectionRect.setHeight(fTimeGraphPane.getHeight());

        fSelectionRect.setVisible(true);
    }

    // ------------------------------------------------------------------------
    // Methods related to the Tree area
    // ------------------------------------------------------------------------

    private static Node prepareTreeContents(TimeGraphTreeRender treeRender, double paneWidth) {
        /* Prepare the tree element objects */
        List<Label> treeElements = treeRender.getAllTreeElements().stream()
                // TODO Put as a real tree. TreeView ?
                .map(elem -> new Label(elem.getName()))
                .peek(label -> {
                    label.setPrefHeight(ENTRY_HEIGHT);
                    label.setPadding(new Insets(0, LABEL_SIDE_MARGIN, 0, LABEL_SIDE_MARGIN));
                    /*
                     * Re-set the solid background for the labels, so we do not
                     * see the background lines through.
                     */
                    label.setStyle(BACKGROUND_STYLE);
                })
                .collect(Collectors.toList());

        VBox treeElemsBox = new VBox(); // Change to TreeView eventually ?
        treeElemsBox.getChildren().addAll(treeElements);

        /* Prepare the Canvases with the horizontal alignment lines */
        List<Canvas> canvases = new ArrayList<>();
        int maxEntriesPerCanvas = (int) (MAX_CANVAS_HEIGHT / ENTRY_HEIGHT);
        Lists.partition(treeElements, maxEntriesPerCanvas).forEach(subList -> {
            int nbElements = subList.size();
            double height = nbElements * ENTRY_HEIGHT;

            Canvas canvas = new Canvas(paneWidth, height);
            drawBackgroundLines(canvas, ENTRY_HEIGHT);
            canvas.setCache(true);
            canvases.add(canvas);
        });
        VBox canvasBox = new VBox();
        canvasBox.getChildren().addAll(canvases);

        /* Put the background Canvas and the Tree View into their containers */
        StackPane stackPane = new StackPane(canvasBox, treeElemsBox);
        stackPane.setStyle(BACKGROUND_STYLE);
        return stackPane;
    }

    // ------------------------------------------------------------------------
    // Methods related to the Time Graph area
    // ------------------------------------------------------------------------

    private Node prepareTimeGraphContents(List<List<TimeGraphStateRender>> renders) {
        Set<Node> canvases = renders.stream()
                .parallel() // order doesn't matter here
                .flatMap(render -> getCanvasesForRender(render).stream())
                .collect(Collectors.toSet());

        return new Group(canvases);
    }

    /**
     * Get the vertically-tiled Canvas's for a single render. They will
     * be already relocated correctly, so the collection's order does not
     * matter.
     *
     * @param render
     *            The render
     * @return The vertical set of canvases
     */
    private Collection<Canvas> getCanvasesForRender(List<TimeGraphStateRender> render) {
        List<List<TimeGraphStateInterval>> stateIntervals = render.stream()
                .map(stateRender -> stateRender.getStateIntervals())
                .collect(Collectors.toList());

        if (stateIntervals.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        /* The canvas will be put on the Pane at this offset */
        final double xOffset = timestampToPaneXPos(render.get(0).getStartTime());
        final double xEnd = timestampToPaneXPos(render.get(0).getEndTime());
        final double canvasWidth = xEnd - xOffset;
        final int maxEntriesPerCanvas = (int) (MAX_CANVAS_HEIGHT / ENTRY_HEIGHT);

        /*
         * Split the full list of intervals into smaller partitions, and draw
         * one Canvas per partition.
         */
        List<Canvas> canvases = new ArrayList<>();
        double yOffset = 0;
        List<List<List<TimeGraphStateInterval>>> partitionedIntervals =
                Lists.partition(stateIntervals, maxEntriesPerCanvas);
        for (int i = 0; i < partitionedIntervals.size(); i++) {
            /* "states" represent the subset of intervals to draw on this Canvas */
            List<List<TimeGraphStateInterval>> states = partitionedIntervals.get(i);
            final double canvasHeight = ENTRY_HEIGHT * states.size();

            Canvas canvas = new Canvas(canvasWidth, canvasHeight);
            drawBackgroundLines(canvas, ENTRY_HEIGHT);
            drawStates(states, canvas.getGraphicsContext2D(), xOffset);

//            System.out.println("relocating canvas of size + (" + canvasWidth + ", " + canvasHeight + ") to " + xOffset + ", " + yOffset);
            canvas.relocate(xOffset, yOffset);
            canvas.setCache(true); // TODO Test?
            canvases.add(canvas);

            yOffset += canvasHeight;
        }
        return canvases;
    }

    private void drawStates(List<List<TimeGraphStateInterval>> stateIntervalsToDraw, GraphicsContext gc, double xOffset) {
        IntStream.range(0, stateIntervalsToDraw.size()).forEach(index -> {
            /*
             * The base (top) of each full-thickness rectangle object we will
             * draw for this entry
             */
            final double xBase = index * ENTRY_HEIGHT;

            List<TimeGraphStateInterval> intervals = stateIntervalsToDraw.get(index);
            for (TimeGraphStateInterval interval : intervals) {
                try {
                    /*
                     * These coordinates are relative to the canvas itself, so
                     * we need to substract the value of the offset of the
                     * canvas relative to the Pane.
                     */
                    final double xStart = timestampToPaneXPos(interval.getStartEvent().getTimestamp()) - xOffset;
                    final double xEnd = timestampToPaneXPos(interval.getEndEvent().getTimestamp()) - xOffset;
                    final double xWidth = Math.max(1.0, xEnd - xStart) + 1.0;

                    double yStart, yHeight;
                    switch (interval.getLineThickness()) {
                    case NORMAL:
                    default:
                        yStart = xBase + 4;
                        yHeight = ENTRY_HEIGHT - 4;
                        break;
                    case SMALL:
                        yStart = xBase + 8;
                        yHeight = ENTRY_HEIGHT - 8;
                        break;
                    }

                    gc.setFill(JfxColorFactory.getColorFromDef(interval.getColorDefinition()));
                    gc.fillRect(xStart, yStart, xWidth, yHeight);

                } catch (IllegalArgumentException iae) { // TODO Temp
                    System.out.println("out of bounds interval:" + interval.toString());
                    continue;
                }

                // TODO Paint the state's name if applicable
            }
        });

    }

    // ------------------------------------------------------------------------
    // Mouse event listeners
    // ------------------------------------------------------------------------

    /**
     * Class encapsulating the time range selection, related drawing and
     * listeners.
     */
    private class SelectionContext {

        private boolean fOngoingSelection;
        private double fMouseOriginX;

        public final EventHandler<MouseEvent> fMousePressedEventHandler = e -> {
            if (e.isShiftDown() ||
                    e.isControlDown() ||
                    e.isSecondaryButtonDown() ||
                    e.isMiddleButtonDown()) {
                /* Do other things! */
                // TODO!
                return;
            }

            if (fOngoingSelection) {
                return;
            }

            /* Remove the current selection, if there is one */
            fSelectionRect.setVisible(false);

            fMouseOriginX = e.getX();

            fOngoingSelectionRect.setX(fMouseOriginX);
            fOngoingSelectionRect.setY(0);
            fOngoingSelectionRect.setWidth(0);
            fOngoingSelectionRect.setHeight(fTimeGraphPane.getHeight());

            fOngoingSelectionRect.setVisible(true);

            e.consume();

            fOngoingSelection = true;
        };

        public final EventHandler<MouseEvent> fMouseDraggedEventHandler = e -> {
            double newX = e.getX();
            double offsetX = newX - fMouseOriginX;

            if (offsetX > 0) {
                fOngoingSelectionRect.setX(fMouseOriginX);
                fOngoingSelectionRect.setWidth(offsetX);
            } else {
                fOngoingSelectionRect.setX(newX);
                fOngoingSelectionRect.setWidth(-offsetX);
            }

            e.consume();
        };

        public final EventHandler<MouseEvent> fMouseReleasedEventHandler = e -> {
            fOngoingSelectionRect.setVisible(false);

            e.consume();

            /* Send a time range selection signal for the currently selected time range */
            double startX = Math.max(0, fOngoingSelectionRect.getX());
            // FIXME Possible glitch when selecting backwards outside of the window
            double endX = Math.min(fTimeGraphPane.getWidth(), startX + fOngoingSelectionRect.getWidth());
            long tsStart = paneXPosToTimestamp(startX);
            long tsEnd = paneXPosToTimestamp(endX);

            getControl().updateTimeRangeSelection(tsStart, tsEnd);

            fOngoingSelection = false;
        };
    }

    /**
     * Class encapsulating the scrolling operations of the time graph pane.
     *
     * The mouse entered/exited handlers ensure only the scrollpane being
     * interacted by the user is the one sending the synchronization signals.
     */
    private class ScrollingContext {

        private boolean fUserActionOngoing = false;

        private final EventHandler<MouseEvent> fMouseEnteredEventHandler = e -> {
            fUserActionOngoing = true;
        };

        private final EventHandler<MouseEvent> fMouseExitedEventHandler = e -> {
            fUserActionOngoing = false;
        };

        /**
         * Listener for the horizontal scrollbar changes
         */
        private final ChangeListener<Object> fHScrollChangeListener = (observable, oldValue, newValue) -> {
            if (!fUserActionOngoing) {
                System.out.println("Listener triggered but inactive");
                return;
            }

            System.out.println("Change listener triggered, oldval=" + oldValue.toString() + ", newval=" + newValue.toString());

            /*
             * Determine the X position represented by the left edge of the pane
             */
            double hmin = fTimeGraphScrollPane.getHmin();
            double hmax = fTimeGraphScrollPane.getHmax();
            double hvalue = fTimeGraphScrollPane.getHvalue();
            double contentWidth = fTimeGraphPane.getLayoutBounds().getWidth();
            double viewportWidth = fTimeGraphScrollPane.getViewportBounds().getWidth();
            double hoffset = Math.max(0, contentWidth - viewportWidth) * (hvalue - hmin) / (hmax - hmin);

            /*
             * Convert the positions of the left and right edges to timestamps,
             * and send a window range update signal
             */
            long tsStart = paneXPosToTimestamp(hoffset);
            long tsEnd = paneXPosToTimestamp(hoffset + viewportWidth);

            System.out.printf("Offset: %.1f, width: %.1f %n", hoffset, viewportWidth);
            System.out.printf("Sending visible range update: %,d to %,d%n", tsStart, tsEnd);

            getControl().updateVisibleTimeRange(tsStart, tsEnd);

            /*
             * The control will not send its own signal back to us (to avoid
             * jitter while scrolling). We will however refresh the area to
             * paint ourselves
             */
            paintArea(tsStart, tsEnd);
        };
    }

    // ------------------------------------------------------------------------
    // Common utils
    // ------------------------------------------------------------------------

    private static void drawBackgroundLines(Canvas canvas, double entryHeight) {
        double width = canvas.getWidth();
        int nbLines = (int) (canvas.getHeight() / entryHeight);


        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.save();

        gc.setStroke(BACKGROUD_LINES_COLOR);
        gc.setLineWidth(1);
        /* average+2 gives the best-looking output */
        DoubleStream.iterate((ENTRY_HEIGHT / 2) + 2, i -> i + entryHeight).limit(nbLines).forEach(yPos -> {
            gc.strokeLine(0, yPos, width, yPos);
        });

        gc.restore();
    }

    private double timestampToPaneXPos(long timestamp) {
        long fullTimeGraphStartTime = getControl().getFullTimeGraphStartTime();
        long fullTimeGraphEndTime = getControl().getFullTimeGraphEndTime();
        return timestampToPaneXPos(timestamp, fullTimeGraphStartTime, fullTimeGraphEndTime, fNanosPerPixel);
    }

    @VisibleForTesting
    public static double timestampToPaneXPos(long timestamp, long start, long end, double nanosPerPixel) {
        if (timestamp < start) {
            throw new IllegalArgumentException(timestamp + " is smaller than trace start time " + start); //$NON-NLS-1$
        }
        if (timestamp > end) {
            throw new IllegalArgumentException(timestamp + " is greater than trace end time " + end); //$NON-NLS-1$
        }

        double traceTimeRange = end - start;
        double timeStampRatio = (timestamp - start) / traceTimeRange;

        long fullTraceWidthInPixels = (long) (traceTimeRange / nanosPerPixel);
        double xPos = fullTraceWidthInPixels * timeStampRatio;
        return Math.round(xPos);
    }

    private long paneXPosToTimestamp(double x) {
        long fullTimeGraphStartTime = getControl().getFullTimeGraphStartTime();
        return paneXPosToTimestamp(x, fTimeGraphPane.getWidth(), fullTimeGraphStartTime, fNanosPerPixel);
    }

    @VisibleForTesting
    public static long paneXPosToTimestamp(double x, double totalWidth, long startTimestamp, double nanosPerPixel) {
        if (x < 0.0 || totalWidth < 1.0 || x > totalWidth) {
            throw new IllegalArgumentException("Invalid position arguments: pos=" + x + ", width=" + totalWidth);
        }

        long ts = Math.round(x * nanosPerPixel);
        return ts + startTimestamp;
    }

}
