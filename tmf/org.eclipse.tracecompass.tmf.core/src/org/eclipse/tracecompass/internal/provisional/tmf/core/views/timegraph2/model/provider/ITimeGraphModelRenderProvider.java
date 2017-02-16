/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.provider;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.arrows.TimeGraphArrowRender;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.drawnevents.TimeGraphDrawnEventRender;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.states.TimeGraphStateInterval;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.states.TimeGraphStateRender;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.tooltip.TimeGraphTooltip;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.tree.TimeGraphTreeElement;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.tree.TimeGraphTreeRender;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public interface ITimeGraphModelRenderProvider {

    // ------------------------------------------------------------------------
    // Configuration option classes
    // ------------------------------------------------------------------------

    class SortingMode {

        private final String fName;

        public SortingMode(String name) {
            fName = name;
        }

        public String getName() {
            return fName;
        }
    }

    class FilterMode {

        private final String fName;

        public FilterMode(String name) {
            fName = name;
        }

        public String getName() {
            return fName;
        }
    }

    void setTrace(@Nullable ITmfTrace trace);

    // ------------------------------------------------------------------------
    // Render generation methods
    // ------------------------------------------------------------------------

    TimeGraphTreeRender getTreeRender(long startTime, long endTime);

    TimeGraphStateRender getStateRender(TimeGraphTreeElement treeElement,
            long rangeStart, long rangeEnd, long resolution);

    default List<TimeGraphStateRender> getStateRenders(TimeGraphTreeRender treeRender, long resolution) {
        long start = treeRender.getStartTime();
        long end = treeRender.getEndTime();
        return treeRender.getAllTreeElements().stream()
                .map(treeElem -> getStateRender(treeElem, start, end, resolution))
                .collect(Collectors.toList());
    }

    TimeGraphDrawnEventRender getDrawnEventRender(TimeGraphTreeElement treeElement, long rangeStart, long rangeEnd);

    TimeGraphArrowRender getArrowRender(TimeGraphTreeRender treeRender);

    TimeGraphTooltip getTooltip(TimeGraphStateInterval interval);


    // ------------------------------------------------------------------------
    // Sorting modes
    // ------------------------------------------------------------------------

    List<SortingMode> getSortingModes();

    SortingMode getCurrentSortingMode();

    void setCurrentSortingMode(int index);

    // ------------------------------------------------------------------------
    // Filter modes
    // ------------------------------------------------------------------------

    List<FilterMode> getFilterModes();

    void enableFilterMode(int index);

    void disableFilterMode(int index);

    Set<FilterMode> getActiveFilterModes();

}
