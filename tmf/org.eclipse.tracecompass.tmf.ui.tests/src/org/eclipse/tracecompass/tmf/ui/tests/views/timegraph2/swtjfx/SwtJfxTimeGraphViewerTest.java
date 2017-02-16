/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.tests.views.timegraph2.swtjfx;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.control.TimeGraphModelControl;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SwtJfxTimeGraphViewerTest {

//    private static final double DELTA = 0.1;

    private static final long FULL_TRACE_START_TIME = 100000L;
    private static final long FULL_TRACE_END_TIME = 200000L;

    private @Nullable TimeGraphModelControl fControl;
    private @Nullable SwtJfxTimeGraphViewerStub fViewer;

    @Before
    public void setup() {
        Shell shell = checkNotNull(Display.getDefault().getActiveShell());
        TimeGraphModelControl control = new TimeGraphModelControl(new ModelRenderProviderStub());
        SwtJfxTimeGraphViewerStub viewer = new SwtJfxTimeGraphViewerStub(shell, control);
        control.attachViewer(viewer);

        control.setTimeGraphAreaRange(FULL_TRACE_START_TIME, FULL_TRACE_END_TIME);
        viewer.getTimeGraphScrollPane().setMinWidth(1000.0);

        fControl = control;
        fViewer = viewer;
    }

    @After
    public void tearDown() {
        if (fViewer != null) {
            fViewer.dispose();
        }
        if (fControl != null) {
            fControl.dispose();
        }

        fControl = null;
        fViewer = null;
    }

    @Test
    public void testSeekVisibleRange() {
        SwtJfxTimeGraphViewerStub viewer = checkNotNull(fViewer);

        TmfTimeRange range = createTimeRange(150000L, 160000L);
        TmfSignal signal = new TmfWindowRangeUpdatedSignal(this, range);
        TmfSignalManager.dispatchSignal(signal);

        double visibleWidth = viewer.getTimeGraphScrollPane().getWidth();
        System.out.println("width=" + visibleWidth);
    }

    @Test
    public void testZoomOut() {

    }

    private static TmfTimeRange createTimeRange(long start, long end) {
        return new TmfTimeRange(TmfTimestamp.fromNanos(start), TmfTimestamp.fromNanos(end));
    }
}
