/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.enhanced;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.tracecompass.internal.provisional.datastore.core.condition.RangeCondition;
import org.eclipse.tracecompass.internal.provisional.datastore.core.historytree.overlapping.AbstractOverlappingHistoryTree;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.StateSystemInterval;

import com.google.common.annotations.VisibleForTesting;

/**
 * A Enhanced History Tree (eSHT) is a state-system specific history tree which
 * makes use of quarks as an additional criterion to place and retrieve
 * intervals from children nodes.
 *
 * @author Loic Prieur-Drevon
 */
public class EnhancedHistoryTree
        extends AbstractOverlappingHistoryTree<StateSystemInterval, EnhancedNode> {

    /**
     * The magic number for this file format.
     */
    public static final int FILE_MAGIC_NUMBER = 0x05FFB100;

    /** File format version. Increment when breaking compatibility. */
    private static final int FILE_VERSION = 8;

    // FIXME If the test plugin is changed to a fragment, and the tests are
    // moved to the same package as this, default-visibility will be sufficient.
    /** Factory for nodes of this tree */
    @VisibleForTesting
    public static final IHTNodeFactory<StateSystemInterval, EnhancedNode> NODE_FACTORY =
            (t, b, m, seq, p, start) -> new EnhancedNode(t, b, m, seq, p, start);

    // ------------------------------------------------------------------------
    // Constructors/"Destructors"
    // ------------------------------------------------------------------------

    /**
     * Create a new Enhanced History Tree from scratch, specifying all
     * configuration parameters.
     *
     * @param stateHistoryFile
     *            The name of the history file
     * @param blockSize
     *            The size of each "block" on disk in bytes. One node will
     *            always fit in one block. It should be at least 4096.
     * @param maxChildren
     *            The maximum number of children allowed per core (non-leaf)
     *            node.
     * @param providerVersion
     *            The version of the state provider. If a file already exists,
     *            and their versions match, the history file will not be rebuilt
     *            uselessly.
     * @param treeStart
     *            The start time of the history
     * @throws IOException
     *             If an error happens trying to open/write to the file
     *             specified in the config
     */
    public EnhancedHistoryTree(File stateHistoryFile,
            int blockSize,
            int maxChildren,
            int providerVersion,
            long treeStart) throws IOException {

        super(stateHistoryFile,
                blockSize,
                maxChildren,
                providerVersion,
                treeStart,
                StateSystemInterval.DESERIALISER);
    }

    /**
     * "Reader" constructor : instantiate a SHTree from an existing tree file on
     * disk
     *
     * @param existingStateFile
     *            Path/filename of the history-file we are to open
     * @param expectedProviderVersion
     *            The expected version of the state provider
     * @throws IOException
     *             If an error happens reading the file
     */
    public EnhancedHistoryTree(File existingStateFile, int expectedProviderVersion)
            throws IOException {
        super(existingStateFile,
                expectedProviderVersion,
                StateSystemInterval.DESERIALISER);
    }

    @Override
    protected int getMagicNumber() {
        return FILE_MAGIC_NUMBER;
    }

    @Override
    protected int getFileVersion() {
        return FILE_VERSION;
    }

    @Override
    protected IHTNodeFactory<StateSystemInterval, EnhancedNode> getNodeFactory() {
        return NODE_FACTORY;
    }

    // ------------------------------------------------------------------------
    // Test-specific methods
    // ------------------------------------------------------------------------

    @Override
    @VisibleForTesting
    protected boolean verifyChildrenSpecific(EnhancedNode parent, int index, EnhancedNode child) {
        return (parent.getChildStart(index) == child.getNodeStart()
                && parent.getChildEnd(index) == child.getNodeEnd()
                && parent.getMinQuark(index) <= child.getMinQuark()
                && parent.getMaxQuark(index) >= child.getMaxQuark());
    }

    @Override
    @VisibleForTesting
    protected boolean verifyIntersectingChildren(EnhancedNode parent, EnhancedNode child) {
        int childSequence = child.getSequenceNumber();
        for (long t = parent.getNodeStart(); t < parent.getNodeEnd(); t++) {
            RangeCondition<Long> rc = RangeCondition.singleton(t);
            boolean shouldBeInCollection = (rc.intersects(child.getNodeStart(), child.getNodeEnd()));
            Collection<Integer> nextChildren = parent.selectNextChildren(rc);
            // There should be only one intersecting child
            if (shouldBeInCollection != nextChildren.contains(childSequence)) {
                return false;
            }
        }
        return true;
    }
}
