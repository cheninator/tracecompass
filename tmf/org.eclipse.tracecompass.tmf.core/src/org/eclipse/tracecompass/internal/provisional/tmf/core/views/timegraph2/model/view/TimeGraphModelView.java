/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.view;

import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.control.TimeGraphModelControl;

public abstract class TimeGraphModelView {

    private final TimeGraphModelControl fControl;

    protected TimeGraphModelView(TimeGraphModelControl control) {
        fControl = control;
    }

    public TimeGraphModelControl getControl() {
        return fControl;
    }

    public final void dispose() {
        disposeImpl();
    }

    // ------------------------------------------------------------------------
    // Template methods
    // ------------------------------------------------------------------------

    protected abstract void disposeImpl();

    public abstract void clear();

    /**
     * This should be called whenever the visible window moves, including zoom
     * level changes.
     */
    public abstract void seekVisibleRange(long visibleWindowStartTime, long visibleWindowEndTime);


    /**
     * Draw a new selection rectangle. The previous one, if any, will be
     * removed.
     */
    public abstract void drawSelection(long selectionStartTime, long selectionEndTime);

}
