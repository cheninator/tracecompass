/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.statesystem.core.tests.backend.historytree.enhanced;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.tracecompass.internal.provisional.datastore.core.historytree.AbstractHistoryTree.IHTNodeFactory;
import org.eclipse.tracecompass.internal.provisional.datastore.core.historytree.HTCoreNodeTest;
import org.eclipse.tracecompass.internal.provisional.datastore.core.historytree.HTNode;
import org.eclipse.tracecompass.internal.provisional.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.StateSystemInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the specific behavior of the EShtNode
 *
 * @author Geneviève Bastien
 */
@RunWith(Parameterized.class)
public class EnhancedNodeTest extends HTCoreNodeTest<StateSystemInterval, EnhancedNodeStub> {

    /**
     * A factory to create base objects for test
     */
    private static final ObjectFactory<StateSystemInterval> BASE_INTERVAL_FACTORY =
            (s, e) -> new StateSystemInterval(s, e, 1, TmfStateValue.nullValue());

    /**
     * Constructor
     *
     * @param name
     *            The name of the test
     * @param headerSize
     *            The size of the header for this node type
     * @param factory
     *            The node factory to use
     * @param readFactory
     *            The factory to read element data from disk
     * @param objFactory
     *            The factory to create objects for this tree
     * @throws IOException
     *             Any exception occurring with the file
     */
    public EnhancedNodeTest(String name,
            int headerSize,
            IHTNodeFactory<StateSystemInterval, EnhancedNodeStub> factory,
            IHTIntervalReader<StateSystemInterval> readFactory,
            ObjectFactory<StateSystemInterval> objFactory) throws IOException {
        super(name, headerSize, factory, readFactory, objFactory);
    }

    /**
     * @return The arrays of parameters
     */
    @Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
                { "Enhanced tree node",
                        HTNode.COMMON_HEADER_SIZE + Integer.BYTES + 3 * Integer.BYTES * NB_CHILDREN + 2 * Long.BYTES * NB_CHILDREN,
                        EnhancedNodeStub.ENH_NODE_STUB_FACTORY,
                        StateSystemInterval.DESERIALISER,
                        BASE_INTERVAL_FACTORY
                },
        });
    }

    /**
     * Test the quark bounds of nodes
     */
    @Test
    public void testQuarkBounds() {
        long start = 10L;
        int avgQuarkNo = 20;

        // Create a new node and add intervals for quarks around average
        EnhancedNodeStub rootNode = newNode(0, -1, start);
        rootNode.add(new StateSystemInterval(start, start + 10, avgQuarkNo, TmfStateValue.newValueInt(3)));
        rootNode.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo, TmfStateValue.newValueInt(4)));
        rootNode.add(new StateSystemInterval(start, start + 10, avgQuarkNo + 1, TmfStateValue.newValueLong(3)));
        rootNode.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo + 1, TmfStateValue.newValueLong(4)));

        // Verify quark bounds
        assertEquals(avgQuarkNo, rootNode.getMinQuark());
        assertEquals(avgQuarkNo + 1, rootNode.getMaxQuark());

        // Create a child node that will start a first branch from the root
        // node. This first branch will first populate the nodes with intervals,
        // then link them to the parent.
        EnhancedNodeStub childNode0 = newNode(1, 0, start);
        childNode0.add(new StateSystemInterval(start, start + 10, avgQuarkNo - 5, TmfStateValue.newValueInt(3)));
        childNode0.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo - 5, TmfStateValue.newValueInt(4)));
        childNode0.add(new StateSystemInterval(start, start + 10, avgQuarkNo - 4, TmfStateValue.newValueLong(3)));
        childNode0.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo - 4, TmfStateValue.newValueLong(4)));

        // Verify quark bounds
        assertEquals(avgQuarkNo - 5, childNode0.getMinQuark());
        assertEquals(avgQuarkNo - 4, childNode0.getMaxQuark());

        // Add a child to the child with lower quark bounds
        EnhancedNodeStub childNode1 = newNode(2, 1, start);
        childNode1.add(new StateSystemInterval(start, start + 10, avgQuarkNo - 7, TmfStateValue.newValueInt(3)));
        childNode1.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo - 7, TmfStateValue.newValueInt(4)));
        childNode1.add(new StateSystemInterval(start, start + 10, avgQuarkNo - 6, TmfStateValue.newValueLong(3)));
        childNode1.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo - 6, TmfStateValue.newValueLong(4)));

        // Verify quark bounds
        assertEquals(avgQuarkNo - 7, childNode1.getMinQuark());
        assertEquals(avgQuarkNo - 6, childNode1.getMaxQuark());

        // Close child 1 and add it as a child to child0 to a node and verify
        // quark's boundaries are
        // propagated to the parent
        childNode1.closeAndWriteThisNode(start + 20);
        childNode0.linkNewChild(childNode1);
        assertEquals(avgQuarkNo - 7, childNode0.getMinQuark(0));
        assertEquals(avgQuarkNo - 6, childNode0.getMaxQuark(0));

        // Create a new child with higher quark bounds
        EnhancedNodeStub childNode2 = newNode(3, 1, start);
        childNode2.add(new StateSystemInterval(start, start + 10, avgQuarkNo - 8, TmfStateValue.newValueInt(3)));
        childNode2.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo - 8, TmfStateValue.newValueInt(4)));
        childNode2.add(new StateSystemInterval(start, start + 10, avgQuarkNo - 5, TmfStateValue.newValueLong(3)));
        childNode2.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo - 5, TmfStateValue.newValueLong(4)));

        // Verify quark bounds
        assertEquals(avgQuarkNo - 8, childNode2.getMinQuark());
        assertEquals(avgQuarkNo - 5, childNode2.getMaxQuark());

        // Close child2, and add it to child0 and verify quark's boundaries are
        // propagated to the parent
        childNode2.closeAndWriteThisNode(start + 20);
        childNode0.linkNewChild(childNode2);
        assertEquals(avgQuarkNo - 8, childNode0.getMinQuark(1));
        assertEquals(avgQuarkNo - 5, childNode0.getMaxQuark(1));

        // Add a child with elements to root node and verify child's boundaries
        childNode0.closeAndWriteThisNode(start + 20);
        rootNode.linkNewChild(childNode0);
        assertEquals(avgQuarkNo - 8, rootNode.getMinQuark(0));
        assertEquals(avgQuarkNo - 4, rootNode.getMaxQuark(0));

        // Create a new branch to root node. The
        // new branch will link the children first, then add intervals. The
        // parent should not be updated until the child is closed
        childNode0 = newNode(4, 0, start);
        rootNode.linkNewChild(childNode0);
        childNode0.add(new StateSystemInterval(start, start + 10, avgQuarkNo + 5, TmfStateValue.newValueInt(3)));
        childNode0.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo + 5, TmfStateValue.newValueInt(4)));
        childNode0.add(new StateSystemInterval(start, start + 10, avgQuarkNo + 4, TmfStateValue.newValueLong(3)));
        childNode0.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo + 4, TmfStateValue.newValueLong(4)));

        // Verify quark bounds
        assertEquals(avgQuarkNo + 4, childNode0.getMinQuark());
        assertEquals(avgQuarkNo + 5, childNode0.getMaxQuark());

        // The root node should not be updated until the node is closed
        assertEquals(0, rootNode.getMinQuark(1));
        assertEquals(Integer.MAX_VALUE, rootNode.getMaxQuark(1));

        // Add a child to the child with lower quark bounds than parent
        childNode1 = newNode(5, 1, start);
        childNode0.linkNewChild(childNode1);
        childNode1.add(new StateSystemInterval(start, start + 10, avgQuarkNo - 7, TmfStateValue.newValueInt(3)));
        childNode1.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo - 7, TmfStateValue.newValueInt(4)));
        childNode1.add(new StateSystemInterval(start, start + 10, avgQuarkNo - 6, TmfStateValue.newValueLong(3)));
        childNode1.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo - 6, TmfStateValue.newValueLong(4)));

        // Verify quark bounds
        assertEquals(avgQuarkNo - 7, childNode1.getMinQuark());
        assertEquals(avgQuarkNo - 6, childNode1.getMaxQuark());

        // The parent nodes should not be udpated
        assertEquals(0, childNode0.getMinQuark(0));
        assertEquals(Integer.MAX_VALUE, childNode0.getMaxQuark(0));
        assertEquals(0, rootNode.getMinQuark(1));
        assertEquals(Integer.MAX_VALUE, rootNode.getMaxQuark(1));

        // Close childNode1, the bounds for the parent should be updated, but
        // not the parent's parent
        childNode1.closeAndWriteThisNode(start + 20);
        assertEquals(avgQuarkNo - 7, childNode0.getMinQuark(0));
        assertEquals(avgQuarkNo - 6, childNode0.getMaxQuark(0));
        assertEquals(0, rootNode.getMinQuark(1));
        assertEquals(Integer.MAX_VALUE, rootNode.getMaxQuark(1));

        // Add a child to the child with higher quark bounds
        childNode2 = newNode(6, 1, start);
        childNode0.linkNewChild(childNode2);
        childNode2.add(new StateSystemInterval(start, start + 10, avgQuarkNo + 1, TmfStateValue.newValueInt(3)));
        childNode2.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo + 1, TmfStateValue.newValueInt(4)));
        childNode2.add(new StateSystemInterval(start, start + 10, avgQuarkNo + 6, TmfStateValue.newValueLong(3)));
        childNode2.add(new StateSystemInterval(start + 11, start + 20, avgQuarkNo + 6, TmfStateValue.newValueLong(4)));

        // Verify quark bounds
        assertEquals(avgQuarkNo + 1, childNode2.getMinQuark());
        assertEquals(avgQuarkNo + 6, childNode2.getMaxQuark());

        // The parent's data should not be udpated
        assertEquals(0, childNode0.getMinQuark(1));
        assertEquals(Integer.MAX_VALUE, childNode0.getMaxQuark(1));
        assertEquals(0, rootNode.getMinQuark(1));
        assertEquals(Integer.MAX_VALUE, rootNode.getMaxQuark(1));

        // Close node2, the parent's data should be updated but not the parent's
        // parent
        childNode2.closeAndWriteThisNode(start + 20);
        assertEquals(avgQuarkNo + 1, childNode0.getMinQuark(1));
        assertEquals(avgQuarkNo + 6, childNode0.getMaxQuark(1));
        assertEquals(0, rootNode.getMinQuark(1));
        assertEquals(Integer.MAX_VALUE, rootNode.getMaxQuark(1));

        // Close node 0, the root node should now be udpated
        childNode0.closeAndWriteThisNode(start + 20);
        assertEquals(avgQuarkNo - 7, rootNode.getMinQuark(1));
        assertEquals(avgQuarkNo + 6, rootNode.getMaxQuark(1));
        rootNode.closeAndWriteThisNode(start + 20);
    }

}
