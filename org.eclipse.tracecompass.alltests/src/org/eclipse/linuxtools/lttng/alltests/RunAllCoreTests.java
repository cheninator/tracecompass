/*******************************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.lttng.alltests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Master test suite for all Linux Tools LTTng unit tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    org.eclipse.linuxtools.btf.core.tests.AllTests.class,
    org.eclipse.linuxtools.ctf.core.tests.AllCtfCoreTests.class,
    org.eclipse.linuxtools.ctf.parser.tests.AllCtfParserTests.class,
    org.eclipse.linuxtools.gdbtrace.core.tests.AllGdbTraceCoreTests.class,
    org.eclipse.linuxtools.lttng2.control.core.tests.AllTests.class,
    org.eclipse.linuxtools.lttng2.kernel.core.tests.AllTests.class,
    org.eclipse.linuxtools.lttng2.ust.core.tests.AllTests.class,
    org.eclipse.tracecompass.pcap.core.tests.AllPcapCoreTests.class,
    org.eclipse.tracecompass.statesystem.core.tests.AllTests.class,
    org.eclipse.tracecompass.tmf.analysis.xml.core.tests.AllAnalysisXmlCoreTests.class,
    org.eclipse.tracecompass.tmf.core.tests.AllTmfCoreTests.class,
    org.eclipse.tracecompass.tmf.ctf.core.tests.AllTests.class,
    org.eclipse.tracecompass.tmf.pcap.core.tests.AllTmfPcapCoreTests.class,
    org.eclipse.tracecompass.tmf.analysis.xml.core.tests.AllAnalysisXmlCoreTests.class,
    org.eclipse.tracecompass.tmf.ctf.core.tests.AllTests.class,
    org.eclipse.tracecompass.tmf.pcap.core.tests.AllTmfPcapCoreTests.class
})
public class RunAllCoreTests {

}
