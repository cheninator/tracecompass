package org.eclipse.tracecompass.tmf.ui.tests.views.timegraph2.swtjfx;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.control.TimeGraphModelControl;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.views.timegraph2.view.swtjfx.SwtJfxTimeGraphViewer;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;

public class SwtJfxTimeGraphViewerStub extends SwtJfxTimeGraphViewer {

    public SwtJfxTimeGraphViewerStub(Composite parent, TimeGraphModelControl control) {
        super(parent, control);
    }

    // ------------------------------------------------------------------------
    // Visibility-increasing overrides
    // ------------------------------------------------------------------------

    @Override
    protected Pane getTimeGraphPane() {
        return super.getTimeGraphPane();
    }

    @Override
    protected ScrollPane getTimeGraphScrollPane() {
        return super.getTimeGraphScrollPane();
    }
}
