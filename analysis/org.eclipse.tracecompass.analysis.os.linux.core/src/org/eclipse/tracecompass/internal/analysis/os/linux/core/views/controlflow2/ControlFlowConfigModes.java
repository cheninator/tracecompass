/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.core.views.controlflow2;

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.provider.ITimeGraphModelRenderProvider.FilterMode;
import org.eclipse.tracecompass.internal.provisional.tmf.core.views.timegraph2.model.provider.ITimeGraphModelRenderProvider.SortingMode;

public interface ControlFlowConfigModes {

    SortingMode SORTING_BY_TID = new SortingMode(nullToEmptyString(Messages.ControlFlowSortingModes_ByTid));

    SortingMode SORTING_BY_THREAD_NAME = new SortingMode(nullToEmptyString(Messages.ControlFlowSortingModes_ByThreadName));

    FilterMode FILTERING_INACTIVE_ENTRIES = new FilterMode(nullToEmptyString(Messages.ControlFlowFilterModes_InactiveEntries));
}
