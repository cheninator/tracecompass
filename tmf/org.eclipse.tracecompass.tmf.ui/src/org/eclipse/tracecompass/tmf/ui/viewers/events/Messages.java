/**********************************************************************
 * Copyright (c) 2015 EfficiOS Inc. and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 **********************************************************************/

package org.eclipse.tracecompass.tmf.ui.viewers.events;

import org.eclipse.osgi.util.NLS;

/**
 * Messages for the event table package
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.tmf.ui.viewers.events.messages"; //$NON-NLS-1$

    /** The title of the dialog window */
    public static String StateSystemAttributeSelectionDialog_WindowTitle;

    /** The label for the state system selection widget */
    public static String StateSystemAttributeSelectionDialog_LabelStateSystem;

    /** The label for the attribute path selection widget */
    public static String StateSystemAttributeSelectionDialog_LabelAttributePath;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
