/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.tests.views.timegraph2.swtjfx;

import static org.junit.Assert.assertEquals;

import org.eclipse.tracecompass.internal.provisional.tmf.ui.views.timegraph2.view.swtjfx.SwtJfxTimeGraphViewer;
import org.junit.Test;

public class SwtJfxTimeGraphViewerStaticTest {

    private static final double DELTA = 0.1;

    /**
     * Test area consisting of 100 pixels representing a timerange from 1000 to
     * 2000.
     */
    private static class TestArea1 {
        private static final long START_TIMESTAMP = 1000;
        private static final long END_TIMESTAMP = 2000;
        private static final double NANOS_PER_PIXEL = 10.0;
    }

    @Test
    public void testTimeToPosition() {
        double yPos = SwtJfxTimeGraphViewer.timestampToPaneXPos(1500,
                TestArea1.START_TIMESTAMP,
                TestArea1.END_TIMESTAMP,
                TestArea1.NANOS_PER_PIXEL);
        assertEquals(50.0, yPos, DELTA);

        long start = 1332170682440133097L;
        long end   = 1332170692664579801L;
        long ts1   = 1332170683481793497L;
        long ts2   = 1332170683485732407L;
        double yPos1 = SwtJfxTimeGraphViewer.timestampToPaneXPos(ts1, start, end, 10.0);
        double yPos2 = SwtJfxTimeGraphViewer.timestampToPaneXPos(ts2, start, end, 10.0);
        assertEquals(104166039.959, yPos1, DELTA);
        assertEquals(104559930.959, yPos2, DELTA);

    }

    @Test
    public void testPositionToTimestamp() {
        long ts = SwtJfxTimeGraphViewer.paneXPosToTimestamp(50.0,
                TestArea1.START_TIMESTAMP * TestArea1.NANOS_PER_PIXEL,
                TestArea1.START_TIMESTAMP,
                TestArea1.NANOS_PER_PIXEL);
        assertEquals(1500, ts);
    }
}
