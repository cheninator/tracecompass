/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc. and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.statesystem.core.tests.backend.historytree.enhanced;

import org.eclipse.tracecompass.internal.provisional.datastore.core.historytree.AbstractHistoryTree.IHTNodeFactory;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.StateSystemInterval;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.enhanced.EnhancedNode;

/**
 * Test stub for {@link EnhancedNode}, exposing some testing methods to tests.
 *
 * @author Alexandre Montplaisir
 * @author Geneviève Bastien
 */
public class EnhancedNodeStub extends EnhancedNode {

    /** Factory to create nodes of this type */
    public static final IHTNodeFactory<StateSystemInterval, EnhancedNodeStub> ENH_NODE_STUB_FACTORY =
            (t, b, m, seq, p, start) -> new EnhancedNodeStub(t, b, m, seq, p, start);

    /**
     * Constructor
     *
     * @param type
     *            The type of this node
     * @param blockSize
     *            The size (in bytes) of a serialized node on disk
     * @param maxChildren
     *            The maximum allowed number of children per node
     * @param seqNumber
     *            The (unique) sequence number assigned to this particular node
     * @param parentSeqNumber
     *            The sequence number of this node's parent node
     * @param start
     *            The start of the current node
     */
    public EnhancedNodeStub(NodeType type, int blockSize, int maxChildren,
            int seqNumber, int parentSeqNumber, long start) {
        super(type, blockSize, maxChildren, seqNumber, parentSeqNumber, start);
    }

    /**
     * Close the node and set it as written to disk, even though no write was
     * actually done.
     *
     * @param endtime
     *            The end time of the node
     */
    public void closeAndWriteThisNode(long endtime) {
        closeThisNode(endtime);
        setOnDisk();
    }

    @Override
    protected int getMinQuark(int index) {
        return super.getMinQuark(index);
    }

    @Override
    protected int getMaxQuark(int index) {
        return super.getMaxQuark(index);
    }
}
