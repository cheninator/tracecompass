/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.arrows;

import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.TimeGraphEvent;

public class TimeGraphArrow {

    private final TimeGraphEvent fStartEvent;
    private final TimeGraphEvent fEndEvent;

    public TimeGraphArrow(TimeGraphEvent startEvent, TimeGraphEvent endEvent) {
        fStartEvent = startEvent;
        fEndEvent = endEvent;
    }

    public TimeGraphEvent getStartEvent() {
        return fStartEvent;
    }

    public TimeGraphEvent getEndEvent() {
        return fEndEvent;
    }

}
