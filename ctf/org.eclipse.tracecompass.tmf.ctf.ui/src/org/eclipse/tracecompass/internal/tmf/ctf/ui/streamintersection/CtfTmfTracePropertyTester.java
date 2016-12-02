/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ctf.ui.streamintersection;

import java.util.Set;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;

import com.google.common.collect.ImmutableSet;

/**
 * Property tester to check if a right-clicked trace element represents a CTF
 * trace.
 *
 * @author Alexandre Montplaisir
 */
public class CtfTmfTracePropertyTester extends PropertyTester {

    private static final String ELEMENT_IS_CTF_PROPERTY = "elementIsCtfTrace"; //$NON-NLS-1$

    /**
     * Known trace IDs of CTF traces.
     *
     * Not great to have to define this statically, but there doesn't seem to be
     * a way to get the trace type class without the trace being necessarily
     * opened.
     */
    private static final Set<String> MATCHING_TRACE_TYPES = ImmutableSet.of(
            "org.eclipse.linuxtools.tmf.ui.type.ctf", //$NON-NLS-1$
            "org.eclipse.linuxtools.lttng2.kernel.tracetype", //$NON-NLS-1$
            "org.eclipse.linuxtools.lttng2.ust.tracetype"); //$NON-NLS-1$

    @Override
    public boolean test(@Nullable Object receiver, @Nullable String property,
            Object @Nullable [] args, @Nullable Object expectedValue) {

        if (ELEMENT_IS_CTF_PROPERTY.equals(property)
                && receiver instanceof TmfTraceElement) {
            TmfTraceElement traceElem = (TmfTraceElement) receiver;

            /*
             * If the trace is opened, check the trace class, it should catch
             * all CTF traces.
             */
            if (traceElem.getTrace() instanceof CtfTmfTrace) {
                return true;
            }

            /* If not, defer to checking the trace type among known ones. */
            if (MATCHING_TRACE_TYPES.contains(traceElem.getTraceType())) {
                return true;
            }
        }

        return false;
    }

}
