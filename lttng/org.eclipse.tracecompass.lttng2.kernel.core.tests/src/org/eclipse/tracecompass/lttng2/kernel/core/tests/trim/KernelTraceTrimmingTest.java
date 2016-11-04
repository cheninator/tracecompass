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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.tracecompass.lttng2.lttng.kernel.core.tests.shared.LttngKernelTestTraceUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.Statedump;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.tests.trim.CtfTmfTraceTrimmingTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test of the trace trimming feature on an LTTng kernel trace. It extends
 * {@link CtfTmfTraceTrimmingTest} to add statedump checking of a kernel trace.
 *
 * @author Alexandre Montplaisir
 */
//@Ignore("Trim command requires Babeltrace 2.0, which is not installed on most CIs. Test can be run manually.")
@RunWith(Parameterized.class)
public class KernelTraceTrimmingTest extends CtfTmfTraceTrimmingTest {

    private static final @NonNull CtfTestTrace KERNEL_TRACE = CtfTestTrace.KERNEL;

    /* Trace cutting parameters */
    private static final long REQUESTED_START_TIME = 1332170682692698596L; // 11:24:42.692 698 596
    private static final long REQUESTED_END_TIME = REQUESTED_START_TIME + 1000000000L; // 11:24:43.692 698 596

    /**
     * Test parameter generator
     *
     * @return The list of constructor parameters, one for each test instance.
     */
    @Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> getTestTraces() {
        /* Only run this on the trace "kernel" */
        return Collections.singletonList(new Object[] { KERNEL_TRACE });
    }

    /**
     * Constructor. Receives parameters defined in {@link #getTestTraces()}.
     *
     * @param testTrace
     *            The test trace to use for this test instance.
     */
    public KernelTraceTrimmingTest(@NonNull CtfTestTrace testTrace) {
        super(testTrace);
    }

    /**
     * Test setup.
     *
     * We're overriding the super.setup() because we need to save the statedump
     * before opening the new trace.
     */
    @Override
    @Before
    public void setup() {
        fOriginalTrace = LttngKernelTestTraceUtils.getTrace(KERNEL_TRACE);
        openTrace(fOriginalTrace);

        fRequestedTraceCutStart = REQUESTED_START_TIME;
        fRequestedTraceCutEnd = REQUESTED_END_TIME;

        TmfTimeRange range = new TmfTimeRange(TmfTimestamp.fromNanos(REQUESTED_START_TIME), TmfTimestamp.fromNanos(REQUESTED_END_TIME));
        try {
            /* Perform the trim to create the new trace */
            fNewTracePath = checkNotNull(Files.createTempDirectory("trimmed-trace-test"));
            fOriginalTrace.trim(range, fNewTracePath, new NullProgressMonitor());

            /* Write the statedump */
            KernelAnalysisModule module = getKernelModule((LttngKernelTrace) fOriginalTrace);
            int providerVersion = checkNotNull(module.getProviderVersion());
            ITmfStateSystem ss1 = module.getStateSystem();
            assertNotNull(ss1);

            Statedump statedump = new Statedump(ss1, REQUESTED_START_TIME, providerVersion);
            statedump.dumpState(checkNotNull(fNewTracePath), ss1.getSSID());

            /* Initialize and open the new trace */
            fNewTrace = new LttngKernelTrace();
            fNewTrace.initTrace(null, fNewTracePath.toString(), CtfTmfEvent.class);
            openTrace(fNewTrace);

        } catch (IOException | CoreException | TmfTraceException e) {
            fail(e.getMessage());
        }
    }

    private static KernelAnalysisModule getKernelModule(@NonNull LttngKernelTrace trace) {
        KernelAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace,
                KernelAnalysisModule.class, KernelAnalysisModule.ID);
        assertNotNull(module);
        module.waitForCompletion();
        return module;
    }

    /**
     * Test that the statedump is saved and restored correctly.
     *
     * @throws Exception
     *             If something fails
     */
    @Test
    public void testTrimStatedump() throws Exception {
        LttngKernelTrace initialTrace = (LttngKernelTrace) fOriginalTrace;
        LttngKernelTrace trimmedTrace = (LttngKernelTrace) fNewTrace;
        Path newTracePath = fNewTracePath;
        assertNotNull(initialTrace);
        assertNotNull(trimmedTrace);
        assertNotNull(newTracePath);

        final long newTraceStartTime = trimmedTrace.getStartTime().toNanos();
        final long newTraceEndTime = trimmedTrace.getEndTime().toNanos();

        ITmfStateSystem ss1 = getKernelModule(initialTrace).getStateSystem();
        assertNotNull(ss1);
        List<ITmfStateInterval> state1 = ss1.queryFullState(REQUESTED_START_TIME + 1);

        ITmfStateSystem ss2 = getKernelModule(trimmedTrace).getStateSystem();
        assertNotNull(ss2);
        List<ITmfStateInterval> state2 = ss2.queryFullState(REQUESTED_START_TIME + 1);

        assertEquals(state1.size(), state2.size());
        for (int ss1quark = 0; ss1quark < state1.size(); ss1quark++) {
            /*
             * The quarks are not necessarily the same between the old and new
             * state systems! We have to resolve new quarks from the full paths.
             */
            String[] attributePath = ss1.getFullAttributePathArray(ss1quark);
            int ss2quark = ss2.getQuarkAbsolute(attributePath);

            ITmfStateInterval interval1 = state1.get(ss1quark);
            ITmfStateInterval interval2 = state2.get(ss2quark);

            /*
             * State start and end times may have been clamped to the beginning
             * or end of the new trace, respectively.
             */
            long expectedStart = Math.max(newTraceStartTime, interval1.getStartTime());
            long expectedEnd = Math.min(newTraceEndTime, interval1.getEndTime());

            assertEquals("Mismatching start times for attribute " + Arrays.toString(attributePath),
                    expectedStart, interval2.getStartTime());

            assertEquals("Mismatching end times for attribute " + Arrays.toString(attributePath),
                    expectedEnd, interval2.getEndTime());

            assertEquals("Mismatching state values for attribute " + Arrays.toString(attributePath),
                    interval1.getStateValue(), interval2.getStateValue());
        }

    }
}
