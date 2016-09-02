/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.trace;

import java.io.IOException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

/**
 * Stub for a generic CTF kernel trace.
 *
 * It's basically an LTTng (2.0/2.1) trace, but without depending on o.e.t.lttng2.kernel!
 */
public class KernelCtfTraceStub extends CtfTmfTrace implements IKernelTrace {

    /**
     * Constructor
     */
    public KernelCtfTraceStub() {
        super();
    }

    /**
     * Get a trace from a CTF test trace.
     *
     * @param ctfTrace
     *            The CTF test trace
     * @return The initialized trace
     */
    public static synchronized KernelCtfTraceStub getTrace(CtfTestTrace ctfTrace) {
        String tracePath;
        try {
            tracePath = FileLocator.toFileURL(ctfTrace.getTraceURL()).getPath();
        } catch (IOException e) {
            throw new IllegalStateException();
        }

        KernelCtfTraceStub trace = new KernelCtfTraceStub();
        try {
            trace.initTrace(null, tracePath, CtfTmfEvent.class);
        } catch (TmfTraceException e) {
            /* Should not happen if tracesExist() passed */
            throw new RuntimeException(e);
        }
        return trace;
    }

    @Override
    public @NonNull IKernelAnalysisEventLayout getKernelEventLayout() {
        return KernelEventLayoutStub.getInstance();
    }
}