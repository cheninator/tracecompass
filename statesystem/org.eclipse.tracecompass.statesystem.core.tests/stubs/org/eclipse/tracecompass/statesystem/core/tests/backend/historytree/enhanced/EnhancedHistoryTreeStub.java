/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.statesystem.core.tests.backend.historytree.enhanced;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.List;

import org.eclipse.tracecompass.internal.provisional.datastore.core.historytree.IHistoryTree;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.enhanced.EnhancedHistoryTree;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.enhanced.EnhancedNode;

/**
 * Stub class to unit test the history tree. You can set the size of the
 * interval section before using the tree, in order to fine-tune the test.
 *
 * Note to developers: This tree is not meant to be used with a backend. It just
 * exposes some info from the history tree.
 *
 * @author Geneviève Bastien
 */
public class EnhancedHistoryTreeStub extends EnhancedHistoryTree {

    private int fLastInsertionIndex;

    /**
     * Minimum size a block of this tree should have
     */
    public static final int MINIMUM_BLOCK_SIZE = IHistoryTree.TREE_HEADER_SIZE;

    /**
     * Constructor
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
    public EnhancedHistoryTreeStub(File stateHistoryFile,
            int blockSize,
            int maxChildren,
            int providerVersion,
            long treeStart) throws IOException {

        super(stateHistoryFile,
                blockSize,
                maxChildren,
                providerVersion,
                treeStart);
    }

    /**
     * "Reader" constructor : instantiate a SHTree from an existing tree file on
     * disk
     *
     * @param existingStateFile
     *            Path/filename of the history-file we are to open
     * @param expProviderVersion
     *            The expected version of the state provider
     * @throws IOException
     *             If an error happens reading the file
     */
    public EnhancedHistoryTreeStub(File existingStateFile, int expProviderVersion) throws IOException {
        super(existingStateFile, expProviderVersion);
    }

    @Override
    protected EnhancedNode getNode(int seqNb) throws ClosedChannelException {
        return super.getNode(seqNb);
    }

    @Override
    protected List<EnhancedNode> getLatestBranch() {
        return super.getLatestBranch();
    }

    @Override
    protected void informInsertingAtDepth(int depth) {
        fLastInsertionIndex = depth;
    }

    /**
     * Get the index in the current branch where the last element was inserted
     *
     * @return The index in the branch of the last insertion
     */
    public int getLastInsertionLocation() {
        return fLastInsertionIndex;
    }

}
