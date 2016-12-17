/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.lttng2.kernel.core.tests.trim;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.tracecompass.lttng2.lttng.kernel.core.tests.shared.LttngKernelTestTraceUtils;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.tests.trim.CtfTmfTraceTrimmingTest;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the CTF stream-intersection operation on a LTTng kernel trace.
 *
 * @author Alexandre Montplaisir
 */
//@Ignore("Trim command requires Babeltrace 2.0, which is not installed on most CIs. Test can be run manually.")
public class KernelTraceStreamIntersectionTest {

    // TODO Add a trace that has non-equal streams
    private static final @NonNull CtfTestTrace TEST_TRACE = CtfTestTrace.MANY_THREADS;

    /* Expected trace bounds after stream-intersection */
    // TODO put real values
    private static final long EXPECTED_NEW_START = 1332170682692698596L; // 11:24:42.692 698 596
    private static final long EXPECTED_NEW_END   = 1332170683692698596L; // 11:24:43.692 698 596
    private static final int EXPECTED_NB_EVENTS = 10;

    private CtfTmfTrace fOriginalTrace;
    private CtfTmfTrace fNewTrace;
    private Path fNewTracePath;

    /**
     * Test setup.
     */
    @Before
    public void setup() {
        fOriginalTrace = LttngKernelTestTraceUtils.getTrace(TEST_TRACE);
        openTrace(fOriginalTrace);

        try {
            /* Perform the stream intersection to create the new trace */
            fNewTracePath = checkNotNull(Files.createTempDirectory("trimmed-trace-test"));
            fOriginalTrace.streamIntersection(fNewTracePath, new NullProgressMonitor());

            /* Initialize and open the new trace */
            fNewTrace = new LttngKernelTrace();
            fNewTrace.initTrace(null, fNewTracePath.toString(), CtfTmfEvent.class);
            openTrace(fNewTrace);

        } catch (IOException | CoreException | TmfTraceException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test teardown
     */
    @After
    public void tearDown() {
        if (fOriginalTrace != null) {
            fOriginalTrace.dispose();
        }
        LttngKernelTestTraceUtils.dispose(TEST_TRACE);

        if (fNewTrace != null) {
            fNewTrace.dispose();
        }

        if (fNewTracePath != null) {
            FileUtils.deleteQuietly(fNewTracePath.toFile());
        }
    }

    /**
     * Simulate a trace being opened
     */
    private static void openTrace(CtfTmfTrace trace) {
        trace.indexTrace(true);
        TmfSignalManager.dispatchSignal(new TmfTraceOpenedSignal(CtfTmfTraceTrimmingTest.class, trace, null));
    }

    /**
     * Test that all expected events are present in the new trace.
     */
    @Test
    public void testEvents() {
        CtfTmfTrace initialTrace = fOriginalTrace;
        CtfTmfTrace trimmedTrace = fNewTrace;
        Path newTracePath = fNewTracePath;
        assertNotNull(initialTrace);
        assertNotNull(trimmedTrace);
        assertNotNull(newTracePath);

        /* Verify the new trace bounds */
        final long newTraceStartTime = trimmedTrace.getStartTime().toNanos();
        final long newTraceEndTime = trimmedTrace.getEndTime().toNanos();
        assertEquals(EXPECTED_NEW_START, newTraceStartTime);
        assertEquals(EXPECTED_NEW_END, newTraceEndTime);

        /*
         * Verify that each event from the original trace in the intersection is
         * present in the new one.
         */
        ITmfContext context1 = initialTrace.seekEvent(TmfTimestamp.fromNanos(EXPECTED_NEW_START));
        CtfTmfEvent event1 = initialTrace.getNext(context1);
        ITmfContext context2 = trimmedTrace.seekEvent(0L);
        CtfTmfEvent event2 = trimmedTrace.getNext(context2);

        int count = 0;
        while (event1.getTimestamp().toNanos() <= EXPECTED_NEW_END) {
            assertNotNull(event1);
            assertNotNull("Expected event not present in trimmed trace: " + eventToString(event1), event2);

            assertTrue("The following events are not the same: \n "
                    + eventToString(event1) + '\n'
                    + eventToString(event2),
                    eventsEquals(event1, event2));

            event1 = initialTrace.getNext(context1);
            event2 = trimmedTrace.getNext(context2);
            count++;
        }

        assertEquals(EXPECTED_NB_EVENTS, count);
    }

    private static boolean eventsEquals(CtfTmfEvent event1, CtfTmfEvent event2) {
        return Objects.equals(event1.getTimestamp(), event2.getTimestamp())
                && Objects.equals(event1.getType(), event2.getType())
                && Objects.equals(event1.getContent(), event2.getContent())
                && Objects.equals(event1.getCPU(), event2.getCPU());

                // FIXME This currently gets renamed, but eventually won't.
//                && Objects.equals(event1.getChannel(), event2.getChannel());
    }

    private static String eventToString(CtfTmfEvent event) {
        return com.google.common.base.Objects.toStringHelper(event)
                .add("Timestamp", event.getTimestamp())
                .add("Type", event.getType())
                .add("Content", event.getContent())
                .add("CPU", event.getCPU())
                .add("Channel", event.getChannel())
                .toString();
    }
}
