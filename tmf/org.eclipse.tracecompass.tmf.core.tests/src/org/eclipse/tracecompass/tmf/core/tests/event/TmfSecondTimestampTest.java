/*******************************************************************************
 * Copyright (c) 2012, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Alexandre Montplaisir - Port to JUnit4
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.tests.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.tracecompass.internal.tmf.core.timestamp.TmfSecondTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.junit.Test;

/**
 * Test suite for the {@link TmfSecondTimestamp} class.
 */
@SuppressWarnings("javadoc")
public class TmfSecondTimestampTest {

    // ------------------------------------------------------------------------
    // Variables
    // ------------------------------------------------------------------------

    private final ITmfTimestamp ts0 = new TmfSecondTimestamp(0);
    private final ITmfTimestamp ts1 = new TmfSecondTimestamp(12345);
    private final ITmfTimestamp ts2 = new TmfSecondTimestamp(-1234);

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    @Test
    public void testDefaultConstructor() {
        assertEquals("getValue", 0, ts0.getValue());
        assertEquals("getscale", 0, ts0.getScale());
    }

    @Test
    public void testFullConstructor() {
        assertEquals("getValue", 12345, ts1.getValue());
        assertEquals("getscale", 0, ts1.getScale());
    }

    // ------------------------------------------------------------------------
    // equals
    // ------------------------------------------------------------------------

    @Test
    public void testEqualsReflexivity() {
        assertEquals("equals", ts0, ts0);
        assertEquals("equals", ts1, ts1);
        assertEquals("equals", ts2, ts2);

        assertFalse("different", ts0.equals(ts1));
        assertFalse("different", ts0.equals(ts2));

        assertFalse("different", ts1.equals(ts0));
        assertFalse("different", ts1.equals(ts2));

        assertFalse("different", ts2.equals(ts0));
        assertFalse("different", ts2.equals(ts1));
    }

    @Test
    public void testEqualsNull() {
        assertTrue("different", !ts0.equals(null));
        assertTrue("different", !ts1.equals(null));
        assertTrue("different", !ts2.equals(null));
    }

    @Test
    public void testEqualsNonTimestamp() {
        assertFalse("equals", ts0.equals(ts0.toString()));
    }

    // ------------------------------------------------------------------------
    // toString
    // ------------------------------------------------------------------------

    @Test
    public void testToString() {
        DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
        Date d0 = new Date(ts0.getValue() * 1000);
        Date d1 = new Date(ts1.getValue() * 1000);
        Date d2 = new Date(ts2.getValue() * 1000);
        assertEquals("toString", df.format(d0) + " 000 000", ts0.toString());
        assertEquals("toString", df.format(d1) + " 000 000", ts1.toString());
        assertEquals("toString", df.format(d2) + " 000 000", ts2.toString());
    }

    // ------------------------------------------------------------------------
    // hashCode
    // ------------------------------------------------------------------------

    @Test
    public void testHashCode() {
        final ITmfTimestamp ts0copy = TmfTimestamp.create(ts0.getValue(), ts0.getScale());
        final ITmfTimestamp ts1copy = TmfTimestamp.create(ts1.getValue(), ts1.getScale());
        final ITmfTimestamp ts2copy = TmfTimestamp.create(ts2.getValue(), ts2.getScale());

        assertEquals("hashCode", ts0.hashCode(), ts0copy.hashCode());
        assertEquals("hashCode", ts1.hashCode(), ts1copy.hashCode());
        assertEquals("hashCode", ts2.hashCode(), ts2copy.hashCode());
    }

    // ------------------------------------------------------------------------
    // normalize
    // ------------------------------------------------------------------------

    @Test
    public void testNormalizeScale0() {
        ITmfTimestamp ts = ts0.normalize(0, 0);
        assertEquals("getValue", 0, ts.getValue());
        assertEquals("getscale", 0, ts.getScale());

        ts = ts0.normalize(12345, 0);
        assertEquals("getValue", 12345, ts.getValue());
        assertEquals("getscale", 0, ts.getScale());

        ts = ts0.normalize(10, 0);
        assertEquals("getValue", 10, ts.getValue());
        assertEquals("getscale", 0, ts.getScale());

        ts = ts0.normalize(-10, 0);
        assertEquals("getValue", -10, ts.getValue());
        assertEquals("getscale", 0, ts.getScale());
    }

    @Test
    public void testNormalizeScaleNot0() {
        ITmfTimestamp ts = ts0.normalize(0, 1);
        assertEquals("Zero test", TmfTimestamp.ZERO, ts);

        ts = ts0.normalize(12345, 1);
        assertEquals("getValue", 12345, ts.getValue());
        assertEquals("getscale", 1, ts.getScale());

        ts = ts0.normalize(10, 1);
        assertEquals("getValue", 10, ts.getValue());
        assertEquals("getscale", 1, ts.getScale());

        ts = ts0.normalize(-10, 1);
        assertEquals("getValue", -10, ts.getValue());
        assertEquals("getscale", 1, ts.getScale());
    }

    // ------------------------------------------------------------------------
    // compareTo
    // ------------------------------------------------------------------------

    @Test
    public void testBasicCompareTo() {
        final ITmfTimestamp tstamp1 = TmfTimestamp.fromSeconds(900);
        final ITmfTimestamp tstamp2 = TmfTimestamp.fromSeconds(1000);
        final ITmfTimestamp tstamp3 = TmfTimestamp.fromSeconds(1100);

        assertTrue(tstamp1.compareTo(tstamp1) == 0);

        assertTrue("CompareTo", tstamp1.compareTo(tstamp2) < 0);
        assertTrue("CompareTo", tstamp1.compareTo(tstamp3) < 0);

        assertTrue("CompareTo", tstamp2.compareTo(tstamp1) > 0);
        assertTrue("CompareTo", tstamp2.compareTo(tstamp3) < 0);

        assertTrue("CompareTo", tstamp3.compareTo(tstamp1) > 0);
        assertTrue("CompareTo", tstamp3.compareTo(tstamp2) > 0);
    }

    @Test
    public void testCompareTo() {
        final ITmfTimestamp ts0a = TmfTimestamp.create(0, 2);
        final ITmfTimestamp ts1a = TmfTimestamp.create(123450, -1);
        final ITmfTimestamp ts2a = TmfTimestamp.create(-12340, -1);

        assertTrue(ts1.compareTo(ts1) == 0);

        assertTrue("CompareTo", ts0.compareTo(ts0a) == 0);
        assertTrue("CompareTo", ts1.compareTo(ts1a) == 0);
        assertTrue("CompareTo", ts2.compareTo(ts2a) == 0);
    }

    // ------------------------------------------------------------------------
    // getDelta
    // ------------------------------------------------------------------------

    @Test
    public void testDelta() {
        // Delta for same scale and precision (delta > 0)
        ITmfTimestamp tstamp0 = TmfTimestamp.fromSeconds(10);
        ITmfTimestamp tstamp1 = TmfTimestamp.fromSeconds(5);
        ITmfTimestamp expectd = TmfTimestamp.fromSeconds(5);

        ITmfTimestamp delta = tstamp0.getDelta(tstamp1);
        assertEquals("getDelta", 0, delta.compareTo(expectd));

        // Delta for same scale and precision (delta < 0)
        tstamp0 = TmfTimestamp.fromSeconds(5);
        tstamp1 = TmfTimestamp.fromSeconds(10);
        expectd = TmfTimestamp.fromSeconds(-5);

        delta = tstamp0.getDelta(tstamp1);
        assertEquals("getDelta", 0, delta.compareTo(expectd));
    }

    @Test
    public void testDelta2() {
        // Delta for different scale and same precision (delta > 0)
        final ITmfTimestamp tstamp0 = TmfTimestamp.fromSeconds(10);
        final ITmfTimestamp tstamp1 = TmfTimestamp.create(1, 1);
        final ITmfTimestamp expectd = TmfTimestamp.create(0, 0);

        final ITmfTimestamp delta = tstamp0.getDelta(tstamp1);
        assertEquals("getDelta", 0, delta.compareTo(expectd));
    }

}
