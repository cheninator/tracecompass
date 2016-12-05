/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.enhanced;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.datastore.core.condition.RangeCondition;
import org.eclipse.tracecompass.internal.provisional.datastore.core.historytree.IHTNode;
import org.eclipse.tracecompass.internal.provisional.datastore.core.historytree.overlapping.OverlappingNode;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.StateSystemInterval;

import com.google.common.annotations.VisibleForTesting;

/**
 * Nodes for Enhanced History Trees, which use the quarks as an additional
 * criterion to determine in which children to place and retrieve intervals.
 *
 * @author Loic Prieur-Drevon
 * @author Geneviève Bastien
 */
public class EnhancedNode extends OverlappingNode<StateSystemInterval> {

    /**
     * Custom interval comparator, which adds the notion of attributes.
     */
    private static final Comparator<StateSystemInterval> ENH_NODE_INTERVAL_COMPARATOR = Comparator
            .comparingLong(StateSystemInterval::getEndTime)
            .thenComparingLong(StateSystemInterval::getStartTime)
            .thenComparingInt(StateSystemInterval::getAttribute);

    /* Quark bounds for the current node */
    private int fMinQuark = Integer.MAX_VALUE;
    private int fMaxQuark = 0;

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
    public EnhancedNode(NodeType type, int blockSize, int maxChildren,
            int seqNumber, int parentSeqNumber, long start) {
        super(type, blockSize, maxChildren, seqNumber, parentSeqNumber, start);
    }

    /**
     * Adds the data concerning the quarks covered in the nodes
     */
    protected static class EnhancedExtraData extends OverlappingExtraData {

        /** Minimum quarks in each child's subtree */
        private final int[] fMinQuarks;
        /** Maximum quarks in each child's subtree */
        private final int[] fMaxQuarks;

        /**
         * Enhanced history tree node data constructor
         *
         * @param node
         *            The node containing this extra data.
         */
        public EnhancedExtraData(EnhancedNode node) {
            super(node);
            int size = getNode().getMaxChildren();
            /*
             * We instantiate the following arrays at full size right away,
             * since we want to reserve that space in the node's header.
             * "this.nbChildren" will tell us how many relevant entries there
             * are in those tables.
             */
            fMinQuarks = new int[size];
            fMaxQuarks = new int[size];
            for (int i = 0; i < size; i++) {
                fMinQuarks[i] = 0;
                fMaxQuarks[i] = Integer.MAX_VALUE;
            }
        }

        @Override
        protected EnhancedNode getNode() {
            /* Type enforced by constructor */
            return (EnhancedNode) super.getNode();
        }

        @Override
        public void readSpecificHeader(ByteBuffer buffer) {
            super.readSpecificHeader(buffer);
            int size = getNode().getMaxChildren();

            for (int i = 0; i < size; i++) {
                fMinQuarks[i] = buffer.getInt();
                fMaxQuarks[i] = buffer.getInt();
            }
        }

        @Override
        protected void writeSpecificHeader(ByteBuffer buffer) {
            getNode().takeReadLock();
            try {
                super.writeSpecificHeader(buffer);

                int size = getNode().getMaxChildren();

                /*
                 * Write the children array
                 */
                for (int i = 0; i < size; i++) {
                    buffer.putInt(fMinQuarks[i]);
                    buffer.putInt(fMaxQuarks[i]);
                }
            } finally {
                getNode().releaseReadLock();
            }
        }

        @Override
        protected int getSpecificHeaderSize() {
            int maxChildren = getNode().getMaxChildren();
            int specificSize = super.getSpecificHeaderSize();
            /*
             * MAX_NB * quark bounds (min/max quark tables)
             */
            specificSize += 2 * Integer.BYTES * maxChildren;

            return specificSize;
        }

        @Override
        public void linkNewChild(IHTNode<?> childNode) {
            // The child node should be a EShtNode
            if (!(childNode instanceof EnhancedNode)) {
                throw new IllegalArgumentException("Adding a node that is not an esht node to an esht tree!"); //$NON-NLS-1$
            }
            getNode().takeWriteLock();
            try {

                super.linkNewChild(childNode);
                final int childIndex = getNbChildren() - 1;

                EnhancedNode segmentNode = (EnhancedNode) childNode;
                // If the node is on disk, then update the child with its data
                if (segmentNode.isOnDisk()) {
                    updateChild(segmentNode, childIndex);
                }

                // Add a listener on the child node to update its data in the
                // children's arrays
                segmentNode.addListener((node, endtime) -> updateChild((EnhancedNode) node, childIndex));

            } finally {
                getNode().releaseWriteLock();
            }
        }

        private void updateChild(EnhancedNode node, int childIndex) {
                fMinQuarks[childIndex] = node.getMinQuark();
                fMaxQuarks[childIndex] = node.getMaxQuark();
                // Update min/max quarks with its children data
                for (int i = 0; i < node.getNbChildren(); i++) {
                    fMinQuarks[childIndex] = Math.min(fMinQuarks[childIndex], node.getMinQuark(i));
                    fMaxQuarks[childIndex] = Math.max(fMaxQuarks[childIndex], node.getMaxQuark(i));
                }
        }

        /**
         * Get the lower quark bound of a child node
         *
         * @param index
         *            The child index
         * @return The maximum start value
         */
        public int getMinQuark(int index) {
            getNode().takeReadLock();
            try {
                if (index >= getNbChildren()) {
                    throw new IndexOutOfBoundsException("The child at index " + index + " does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                return fMinQuarks[index];
            } finally {
                getNode().releaseReadLock();
            }
        }

        /**
         * Get the minimum end value of a child
         *
         * @param index
         *            The child index
         * @return The minimum end value
         */
        public int getMaxQuark(int index) {
            getNode().takeReadLock();
            try {
                if (index >= getNbChildren()) {
                    throw new IndexOutOfBoundsException("The child at index " + index + " does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                return fMaxQuarks[index];
            } finally {
                getNode().releaseReadLock();
            }
        }

        /**
         * Get the index of the children who intersect the given timestamps
         *
         * @param start
         *            The start of the period to intersect
         * @param end
         *            The end of the period to intersect
         * @return The collection of the indexes of nodes who intersect the
         *         requested period
         */
        public Set<Integer> getIntersectingChildren(long start, long end) {
            Set<Integer> set = new HashSet<>();
            for (int i = 0; i < getNbChildren(); i++) {
                if (getChildStart(i) <= end && start <= getChildEnd(i)) {
                    set.add(i);
                }
            }
            return set;
        }

        @Override
        public Collection<Integer> selectNextChildren(RangeCondition<Long> rc) {
            return super.selectNextChildren(rc);
            // TODO Implement custom behaviour?
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), fMinQuarks, fMaxQuarks);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            /* super.equals already checks for null / same class */
            EnhancedExtraData other = (EnhancedExtraData) checkNotNull(obj);
            return (Arrays.equals(fMinQuarks, other.fMinQuarks)
                    && Arrays.equals(fMaxQuarks, other.fMaxQuarks));
        }

    }

    @Override
    protected @Nullable EnhancedExtraData createNodeExtraData(final NodeType type) {
        if (type == NodeType.CORE) {
            return new EnhancedExtraData(this);
        }
        return null;
    }

    /**
     * Get the number of intervals in this node
     *
     * @return The number of intervals
     */
    public int getNumIntervals() {
        return getIntervals().size();
    }

    @Override
    public void add(StateSystemInterval newInterval) {
        super.add(newInterval);
        updateBoundaries(newInterval);
    }

    private void updateBoundaries(StateSystemInterval interval) {
        fMinQuark = Math.min(fMinQuark, interval.getAttribute());
        fMaxQuark = Math.max(fMaxQuark, interval.getAttribute());
    }

    /**
     * Get the maximum start time of an interval in this node
     *
     * @return the latest start time of this node's intervals
     */
    public int getMinQuark() {
        return fMinQuark;
    }

    /**
     * Get the earliest end time of an interval in this node
     *
     * @return the earliest end time of this node's intervals
     */
    public int getMaxQuark() {
        return fMaxQuark;
    }

    /**
     * Get the collection of children who intersect the given start and end
     * times
     *
     * @param start
     *            The start of the period to intersect
     * @param end
     *            The end of the period to intersect
     * @return The collection of the sequence numbers of nodes who intersect the
     *         requested period
     */
    public Set<Integer> getIntersectingChildren(long start, long end) {
        EnhancedExtraData extraData = getCoreNodeData();
        if (extraData != null) {
            return extraData.getIntersectingChildren(start, end).stream()
                    .map(i -> extraData.getChild(i))
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @Override
    protected @Nullable EnhancedExtraData getCoreNodeData() {
        return (EnhancedExtraData) super.getCoreNodeData();
    }

    /**
     * Get the maximum start value of a child of this node
     *
     * @param index
     *            The index of the node to get the child's max start
     * @return The child's maximal start value
     */
    protected int getMinQuark(int index) {
        EnhancedExtraData extraData = getCoreNodeData();
        if (extraData != null) {
            return extraData.getMinQuark(index);
        }
        throw new UnsupportedOperationException("A leaf node does not have children"); //$NON-NLS-1$
    }

    /**
     * Get the minimum end value of a child of this node
     *
     * @param index
     *            The index of the node
     * @return The child's minimum end value
     */
    protected int getMaxQuark(int index) {
        EnhancedExtraData extraData = getCoreNodeData();
        if (extraData != null) {
            return extraData.getMaxQuark(index);
        }
        throw new UnsupportedOperationException("A leaf node does not have children"); //$NON-NLS-1$
    }

    @Override
    protected Comparator<StateSystemInterval> getIntervalComparator() {
        return ENH_NODE_INTERVAL_COMPARATOR;
    }

    @Override
    public Iterable<StateSystemInterval> getMatchingIntervals(
            RangeCondition<Long> timeCondition,
            Predicate<StateSystemInterval> extraPredicate) {
        return super.getMatchingIntervals(timeCondition, extraPredicate);
        // TODO Implement custom behaviour?
    }

    /**
     * {@inheritDoc}
     * Re-exported to give access to package.
     */
    @Override
    @VisibleForTesting
    protected long getChildStart(int index) {
        return super.getChildStart(index);
    }

    /**
     * {@inheritDoc}
     * Re-exported to give access to package.
     */
    @Override
    @VisibleForTesting
    protected long getChildEnd(int index) {
        return super.getChildEnd(index);
    }
}
