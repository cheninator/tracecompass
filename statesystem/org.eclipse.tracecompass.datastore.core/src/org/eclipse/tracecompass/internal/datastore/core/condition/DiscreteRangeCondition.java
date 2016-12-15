/*******************************************************************************
 * Copyright (c) 2016 Polytechnique
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.eclipse.tracecompass.internal.datastore.core.condition;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.datastore.core.condition.RangeCondition;

/**
 * Generic Set Condition.
 *
 * @author Loïc Prieur-Drevon
 * @param <E>
 *            Comparable type, typically Long for time stamps, Integer for
 *            quarks.
 */
public class DiscreteRangeCondition<E extends Comparable<E>> implements RangeCondition<E> {

    private final NavigableSet<E> fSet;

    /**
     * Constructor for a NumCondition from a Collection of Integers
     *
     * @param c
     *            Integers to include in Set Condition.
     */
    public DiscreteRangeCondition(Collection<E> c) {
        if (c.isEmpty()) {
            throw new IllegalArgumentException("DiscreteRangeCondition requires a non-empty collection"); //$NON-NLS-1$
        }
        fSet = new TreeSet<>(c);
    }

    @Override
    public E min() {
        return fSet.first();
    }

    @Override
    public E max() {
        return fSet.last();
    }

    @Override
    public boolean contains(E element) {
        return fSet.contains(element);
    }

    /**
     * Return true if the range [low, high] intersects at least one element from
     * the Discrete Range.
     *
     * @param low
     *            lower bound of the timerange
     * @param high
     *            upper bound of the timerange
     */
    @Override
    public boolean intersects(E low, E high) {
        @Nullable E floor = fSet.floor(high);
        if (floor == null) {
            /* There is no element smaller than high */
            return false;
        }
        @Nullable E ceil = fSet.ceiling(low);
        if (ceil == null) {
            /* There is no element larger than low */
            return false;
        }
        /* At least one element is between low and high */
        return ceil.compareTo(floor) <= 0;
    }

    @Override
    public DiscreteRangeCondition<E> subCondition(E from, E to) {
        if (from.compareTo(to) > 0) {
            throw new IllegalArgumentException(from.toString() + " is greater than " + to.toString());  //$NON-NLS-1$
        }
        return new DiscreteRangeCondition<>(fSet.subSet(from, true, to, true));
    }

    @Override
    public String toString() {
        return "Discrete condition: " + fSet.toString(); //$NON-NLS-1$
    }

}