package org.eclipse.tracecompass.internal.provisional.tmf.core.views.json;

import java.util.List;

import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.control.TimeGraphModelControl;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.provider.ITimeGraphModelRenderProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.states.TimeGraphStateRender;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.render.tree.TimeGraphTreeRender;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.view.TimeGraphModelView;

public class TimeGraphJsonOutput extends TimeGraphModelView {

    public TimeGraphJsonOutput(TimeGraphModelControl control) {
        super(control);
    }

    @Override
    public void disposeImpl() {
    }

    @Override
    public void clear() {
        // TODO
    }

    @Override
    public void seekVisibleRange(long visibleWindowStartTime, long visibleWindowEndTime) {
        /* Generate JSON for the visible area */
        ITimeGraphModelRenderProvider provider = getControl().getModelRenderProvider();

        TimeGraphTreeRender treeRender = provider.getTreeRender(visibleWindowStartTime, visibleWindowEndTime);
        List<TimeGraphStateRender> stateRenders = provider.getStateRenders(treeRender, 1);

        RenderToJson.printRenderTo(stateRenders);
    }

    @Override
    public void drawSelection(long selectionStartTime, long selectionEndTime) {
        // TODO NYI
    }

}
