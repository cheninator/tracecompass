/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ctf.core.tests.trim;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.tests.shared.CtfTmfTestTraceUtils;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableSet;

/**
 * Tests related to the trimming feature of CTF traces
 * ({@link CtfTmfTrace#trim}).
 *
 * @author Alexandre Montplaisir
 */
//@Ignore("Trim command requires Babeltrace 2.0, which is not installed on most CIs. Test can be run manually.")
@RunWith(Parameterized.class)
public class CtfTmfTraceTrimmingTest {

    /** Test timeout */
    @Rule public TestRule globalTimeout= new Timeout(5, TimeUnit.MINUTES);

    private final @NonNull CtfTestTrace fTestTrace;

    private CtfTmfTrace fOriginalTrace;
    private long fRequestedTraceCutStart;
    private long fRequestedTraceCutEnd;

    private CtfTmfTrace fNewTrace;
    private Path fNewTracePath;

    // ------------------------------------------------------------------------
    // Test suite definition
    // ------------------------------------------------------------------------

    private static final Set<CtfTestTrace> BLACKLISTED_TRACES = ImmutableSet.of(
            /* Trimming doesn't work on experiments at the moment */
            CtfTestTrace.TRACE_EXPERIMENT
            );

    /**
     * Test parameter generator
     *
     * @return The list of constructor parameters, one for each test instance.
     */
    @Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> getTestTraces() {
        CtfTestTrace[] testTraces = CtfTestTrace.values();
        return Arrays.stream(testTraces)
                .filter(testTrace -> !BLACKLISTED_TRACES.contains(testTrace))
                .map(testTrace -> new Object[] { testTrace })
                .collect(Collectors.toList());
    }

    /**
     * Constructor. Receives parameters defined in {@link #getTestTraces()}.
     *
     * @param testTrace
     *            The test trace to use for this test instance.
     */
    public CtfTmfTraceTrimmingTest(@NonNull CtfTestTrace testTrace) {
        fTestTrace = testTrace;
    }

    // ------------------------------------------------------------------------
    // Test instance maintenance
    // ------------------------------------------------------------------------

    /**
     * Test setup
     */
    @Before
    public void setup() {
        fOriginalTrace = CtfTmfTestTraceUtils.getTrace(fTestTrace);
        openTrace(fOriginalTrace);

        fRequestedTraceCutStart = getTraceCutStart(fOriginalTrace);
        fRequestedTraceCutEnd = getTraceCutEnd(fOriginalTrace);

        assertTrue(fRequestedTraceCutStart >= fOriginalTrace.getStartTime().toNanos());
        assertTrue(fRequestedTraceCutEnd <= fOriginalTrace.getEndTime().toNanos());
        assertTrue(fRequestedTraceCutStart < fRequestedTraceCutEnd);

        TmfTimeRange range = new TmfTimeRange(
                TmfTimestamp.fromNanos(fRequestedTraceCutStart),
                TmfTimestamp.fromNanos(fRequestedTraceCutEnd));
        try {
            /* Perform the trim to create the new trace */
            fNewTracePath = checkNotNull(Files.createTempDirectory("trimmed-trace-test"));
            fOriginalTrace.trim(range, fNewTracePath, new NullProgressMonitor());

            /* Initialize the new trace */
            fNewTrace = new CtfTmfTrace();
            fNewTrace.initTrace(null, fNewTracePath.toString(), CtfTmfEvent.class);
            openTrace(fNewTrace);

        } catch (IOException | TmfTraceException e) {
            fail(e.getMessage());
        } catch (CoreException e) {
            /*
             * CoreException are more or less useless, all the interesting stuff
             * is in their "status" objects.
             */
            String msg;
            IStatus status = e.getStatus();
            IStatus[] children = status.getChildren();
            if (children == null) {
                msg = status.getMessage();
            } else {
                msg = Arrays.stream(children)
                        .map(IStatus::getMessage)
                        .collect(Collectors.joining("\n"));
            }
            fail(msg);
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
        CtfTmfTestTraceUtils.dispose(fTestTrace);

        if (fNewTrace != null) {
            fNewTrace.dispose();
        }

        if (fNewTracePath != null) {
            FileUtils.deleteQuietly(fNewTracePath.toFile());
        }
    }

    /** Simulate a trace being opened */
    private static void openTrace(CtfTmfTrace trace) {
        trace.indexTrace(true);
        TmfSignalManager.dispatchSignal(new TmfTraceOpenedSignal(CtfTmfTraceTrimmingTest.class, trace, null));
    }

    /**
     * Get the timestamp at which we should start cutting the trace. It should
     * be roughly 1/4 into the trace.
     */
    private static long getTraceCutStart(CtfTmfTrace trace) {
        long start = trace.getStartTime().toNanos();
        long end = trace.getEndTime().toNanos();

        return ((end - start) / 4) + start;
    }

    /**
     * Get the timestamp at which we should end the trace cutting. It should be
     * roughly at half the trace.
     */
    private static long getTraceCutEnd(CtfTmfTrace trace) {
        long start = trace.getStartTime().toNanos();
        long end = trace.getEndTime().toNanos();

        return ((end - start) / 2) + start;
    }

    // ------------------------------------------------------------------------
    // Test methods and helpers
    // ------------------------------------------------------------------------

    /**
     * Test that all expected events are present in the new trace.
     */
    @Test
    public void testTrimEvents() {
        CtfTmfTrace initialTrace = fOriginalTrace;
        CtfTmfTrace trimmedTrace = fNewTrace;
        Path newTracePath = fNewTracePath;
        assertNotNull(initialTrace);
        assertNotNull(trimmedTrace);
        assertNotNull(newTracePath);

        /*
         * Verify the bounds of the new trace are fine. The actual trace can be
         * smaller than what was requested if there are no events exactly at the
         * bounds, but should not contain events outside of the requested range.
         */
        final long newTraceStartTime = trimmedTrace.getStartTime().toNanos();
        final long newTraceEndTime = trimmedTrace.getEndTime().toNanos();

        assertTrue("Cut trace start time " + newTraceStartTime
                + " is earlier than the requested " + fRequestedTraceCutStart,
                newTraceStartTime >= fRequestedTraceCutStart);

        assertTrue("Cut trace end time " + newTraceEndTime
                + " is later than the requested " + fRequestedTraceCutEnd,
                newTraceEndTime <= fRequestedTraceCutEnd);

        /*
         * Verify that each trace event from the original trace in the given
         * time range is present in the new one.
         */
        ITmfContext context1 = initialTrace.seekEvent(TmfTimestamp.fromNanos(newTraceStartTime));
        CtfTmfEvent event1 = initialTrace.getNext(context1);
        ITmfContext context2 = trimmedTrace.seekEvent(0L);
        CtfTmfEvent event2 = trimmedTrace.getNext(context2);

        int count = 0;
        while (event1.getTimestamp().toNanos() <= fRequestedTraceCutEnd) {
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

        assertEquals(trimmedTrace.getNbEvents(), count);
    }

    /**
     * {@link TmfEvent#equals} checks the container trace, among other things.
     * Here we want to compare events from different traces, so we have to
     * implement our own equals().
     */
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
