/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.statesystem.core.tests.backend.historytree.enhanced;

import java.io.File;
import java.io.IOException;

import org.eclipse.tracecompass.internal.provisional.datastore.core.historytree.AbstractHistoryTree;
import org.eclipse.tracecompass.internal.provisional.datastore.core.historytree.overlapping.AbstractOverlappingHistoryTreeTestBase;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.StateSystemInterval;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.enhanced.EnhancedNode;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;

/**
 * Test the specific methods of the enhanced history tree
 *
 * @author Geneviève Bastien
 */
public class EnhancedHistoryTreeTest
        extends AbstractOverlappingHistoryTreeTestBase<StateSystemInterval, EnhancedNode> {

    private static final StateSystemInterval NULL_INTERVAL = new StateSystemInterval(10, 20, 1, TmfStateValue.nullValue());

    @Override
    protected EnhancedHistoryTreeStub createHistoryTree(
            File stateHistoryFile,
            int blockSize,
            int maxChildren,
            int providerVersion,
            long treeStart) throws IOException {

        return new EnhancedHistoryTreeStub(stateHistoryFile,
                blockSize,
                maxChildren,
                providerVersion,
                treeStart);
    }

    @Override
    protected EnhancedHistoryTreeStub createHistoryTree(
            File existingStateFile, int expProviderVersion) throws IOException {
        return new EnhancedHistoryTreeStub(existingStateFile, expProviderVersion);
    }

    @Override
    protected StateSystemInterval createInterval(long start, long end) {
        return new StateSystemInterval(start, end, 1, TmfStateValue.newValueInt(3));
    }

    @Override
    protected long fillValues(
            AbstractHistoryTree<StateSystemInterval, EnhancedNode> ht,
            int fillSize, long start) {
        int nbValues = fillSize / NULL_INTERVAL.getSizeOnDisk();
        for (int i = 0; i < nbValues; i++) {
            ht.insert(new StateSystemInterval(start + i, start + i + 1, 1, TmfStateValue.nullValue()));
        }
        return start + nbValues;
    }

}
