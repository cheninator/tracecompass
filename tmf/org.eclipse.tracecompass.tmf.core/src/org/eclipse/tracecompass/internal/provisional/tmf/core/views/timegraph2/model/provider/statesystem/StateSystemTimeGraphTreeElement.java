/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.provider.statesystem;

import java.util.List;

import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.tree.TimeGraphTreeElement;

public class StateSystemTimeGraphTreeElement extends TimeGraphTreeElement {

    private final int fSourceQuark;

    public StateSystemTimeGraphTreeElement(String name,
            List<TimeGraphTreeElement> children,
            int sourceQuark) {
        super(name, children);
        fSourceQuark = sourceQuark;
    }

    public int getSourceQuark() {
        return fSourceQuark;
    }

}
