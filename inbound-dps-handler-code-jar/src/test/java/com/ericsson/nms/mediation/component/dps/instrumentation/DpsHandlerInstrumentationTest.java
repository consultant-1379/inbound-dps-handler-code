/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2013
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.instrumentation;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DpsHandlerInstrumentationTest {
    private static final long[] CALCULATION_SAMPLES = { 1870L, 1900L, 2001L, 2245L };
    private static final long EXPECTED_AVG = sum(CALCULATION_SAMPLES) / CALCULATION_SAMPLES.length;
    private static final long EXPECTED_MAX = max(CALCULATION_SAMPLES);

    private static final int EXPECTED_CONTROLLER_INVOCATION_COUNTER = 10;
    private static final int EXPECTED_TOPOLOGY_INVOCATION_COUNTER = 100;
    private static final int EXPECTED_ATTRIBUTE_INVOCATION_COUNTER = 9999;
    private static final int EXPECTED_FAILED_SYNCS_COUNTER = 9999;
    private static final int EXPECTED_SUCCESSFUL_SYNCS_COUNTER = 9999;

    private DpsHandlerInstrumentation dpsHandlerInstrumentation;

    @Before
    public void setUp() throws Exception {
        dpsHandlerInstrumentation = new DpsHandlerInstrumentation();
    }

    @After
    public void tearDown() throws Exception {
        dpsHandlerInstrumentation = null;
    }

    @Test
    public void testDpsControllerInvocation() {
        dpsHandlerInstrumentation.setDpsInvocationController(new AtomicInteger(EXPECTED_CONTROLLER_INVOCATION_COUNTER - 1));
        dpsHandlerInstrumentation.increaseDpsControllerInvocations();

        final int dpsControllerInvocations = dpsHandlerInstrumentation.getDpsInvocationController();
        assertEquals(EXPECTED_CONTROLLER_INVOCATION_COUNTER, dpsControllerInvocations);
    }

    @Test
    public void testDpsTopologyInvocation() {
        dpsHandlerInstrumentation.setDpsInvocationTopologySync(new AtomicInteger(EXPECTED_TOPOLOGY_INVOCATION_COUNTER - 1));
        dpsHandlerInstrumentation.increaseDpsTopologySyncInvocations();

        final int dpsTopologyInvocations = dpsHandlerInstrumentation.getDpsInvocationTopologySync();
        assertEquals(EXPECTED_TOPOLOGY_INVOCATION_COUNTER, dpsTopologyInvocations);
    }

    @Test
    public void testDpsAttributeInvocation() {
        dpsHandlerInstrumentation.setDpsInvocationAttributeSync(new AtomicInteger(EXPECTED_ATTRIBUTE_INVOCATION_COUNTER - 1));
        dpsHandlerInstrumentation.increaseDpsAttributeSyncInvocations();

        final int dpsAttributeInvocations = dpsHandlerInstrumentation.getDpsInvocationAttributeSync();
        assertEquals(EXPECTED_ATTRIBUTE_INVOCATION_COUNTER, dpsAttributeInvocations);
    }

    @Test
    public void testDpsNumberOfSuccessfulSyncs() {
        dpsHandlerInstrumentation.setDpsCounterForSuccessfulSync(new AtomicInteger(EXPECTED_SUCCESSFUL_SYNCS_COUNTER - 1));
        dpsHandlerInstrumentation.increaseDpsCounterForSuccessfulSync();

        final int dpsSuccessfulSyncInvocations = dpsHandlerInstrumentation.getDpsCounterForSuccessfulSync();
        assertEquals(EXPECTED_SUCCESSFUL_SYNCS_COUNTER, dpsSuccessfulSyncInvocations);
    }

    @Test
    public void testDpsNumberOfFailedSyncs() {
        dpsHandlerInstrumentation.setDpsNumberOfFailedSyncs(new AtomicInteger(EXPECTED_FAILED_SYNCS_COUNTER - 1));
        dpsHandlerInstrumentation.increaseDpsNumberOfFailedSyncs();

        final int dpsFailedSyncInvocations = dpsHandlerInstrumentation.getDpsNumberOfFailedSyncs();
        assertEquals(EXPECTED_FAILED_SYNCS_COUNTER, dpsFailedSyncInvocations);
    }

    @Test
    public void testAverageNumberOfMosBeingSynced() {
        for (final long sample : CALCULATION_SAMPLES) {
            dpsHandlerInstrumentation.addToNumberOfMosBeingSyncedSamples(sample);
        }

        final long avgNumberOfMosBeingSynced = dpsHandlerInstrumentation.getAverageNumberOfMosBeingSynced();
        assertEquals(EXPECTED_AVG, avgNumberOfMosBeingSynced);
    }

    @Test
    public void testAverageNumberOfAttributesBeingSynced() {
        for (final long sample : CALCULATION_SAMPLES) {
            dpsHandlerInstrumentation.addToNumberOfAttrBeingSyncedSamples(sample);
        }

        final long avgNumberOfAttributesBeingSynced = dpsHandlerInstrumentation.getAverageNumberOfAttrBeingSynced();
        assertEquals(EXPECTED_AVG, avgNumberOfAttributesBeingSynced);
    }

    @Test
    public void testAverageDpsTopologyDataTimeTaken() {
        for (final long sample : CALCULATION_SAMPLES) {
            dpsHandlerInstrumentation.addToAverageDpsTopologyDataTimesTaken(sample);
        }

        final long avgDpsTopologyDataTimeTaken = dpsHandlerInstrumentation.getAverageDpsTopologyDataTimeTaken();
        assertEquals(EXPECTED_AVG, avgDpsTopologyDataTimeTaken);
    }

    @Test
    public void testAverageDpsAttributeDataTimeTaken() {
        for (final long sample : CALCULATION_SAMPLES) {
            dpsHandlerInstrumentation.addToAverageDpsAttributeDataTimesTaken(sample);
        }

        final long avgDpsAttributeDataTimeTaken = dpsHandlerInstrumentation.getAverageDpsAttributeDataTimeTaken();
        assertEquals(EXPECTED_AVG, avgDpsAttributeDataTimeTaken);
    }

    @Test
    public void testAverageOverallSyncTimesTaken() {
        for (final long sample : CALCULATION_SAMPLES) {
            dpsHandlerInstrumentation.addToAverageOverallSyncTimesTaken(sample);
        }

        final long avgOverallSyncTimeTaken = dpsHandlerInstrumentation.getAverageOverallSyncTimeTaken();
        assertEquals(EXPECTED_AVG, avgOverallSyncTimeTaken);
    }

    @Test
    public void testAverage_CalulationIsEmpty() {
        final long avgDpsAttributeDataTimeTaken = dpsHandlerInstrumentation.getAverageDpsAttributeDataTimeTaken();
        assertEquals(0L, avgDpsAttributeDataTimeTaken);
    }

    @Test
    public void testMaxOverallSyncTimesTaken() {
        for (final long sample : CALCULATION_SAMPLES) {
            dpsHandlerInstrumentation.addToMaxOverallSyncTimesTaken(sample);
        }

        final long maxOverallSyncTimeTaken = dpsHandlerInstrumentation.getMaxOverallSyncTimeTaken();
        assertEquals(EXPECTED_MAX, maxOverallSyncTimeTaken);
    }

    @Test
    public void testMaxOverallSyncTimesTakenIsEmpty() {
        final long maxOverallSyncTimeTaken = dpsHandlerInstrumentation.getMaxOverallSyncTimeTaken();
        assertEquals(0L, maxOverallSyncTimeTaken);
    }

    private static long sum(final long[] elements) {
        long sum = 0L;

        for (final long element : elements) {
            sum += element;
        }
        return sum;
    }

    private static long max(final long[] elements) {
        long max = elements[0];

        for (int i = 1; i < elements.length; i++) {
            if (elements[i] > max) {
                max = elements[i];
            }
        }
        return max;
    }

}
