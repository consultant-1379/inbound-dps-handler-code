/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
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

public class DeltaDpsHandlerInstrumentationTest {
    private static final long[] CALCULATION_SAMPLES = { 1870L, 1900L, 2001L, 2245L };
    private static final long EXPECTED_AVG = sum(CALCULATION_SAMPLES) / CALCULATION_SAMPLES.length;
    private static final int EXPECTED_FAILED_SYNCS_COUNTER = 9999;
    private static final int EXPECTED_SUCCESSFUL_SYNCS_COUNTER = 9999;
    private static final int EXPECTED_SUCCESSFUL_SYNCS_INVOCATION_COUNTER = 100;

    private DeltaDpsHandlerInstrumentation deltaDpsHandlerInstrumentation;

    @Before
    public void setUp() throws Exception {
        deltaDpsHandlerInstrumentation = new DeltaDpsHandlerInstrumentation();
    }

    @After
    public void tearDown() throws Exception {
        deltaDpsHandlerInstrumentation = null;
    }

    @Test
    public void testDpsNumberOfSuccessfulSyncs() {
        deltaDpsHandlerInstrumentation.setDpsSuccessfulDeltaSync(new AtomicInteger(EXPECTED_SUCCESSFUL_SYNCS_COUNTER - 1));
        deltaDpsHandlerInstrumentation.increaseSuccessfulDeltaSync();

        final int dpsSuccessfulSyncInvocations = deltaDpsHandlerInstrumentation.getDpsSuccessfulDeltaSync();
        assertEquals(EXPECTED_SUCCESSFUL_SYNCS_COUNTER, dpsSuccessfulSyncInvocations);
    }

    @Test
    public void testDpsNumberOfFailedSyncs() {
        deltaDpsHandlerInstrumentation.setDpsFailedDeltaSync(new AtomicInteger(EXPECTED_FAILED_SYNCS_COUNTER - 1));
        deltaDpsHandlerInstrumentation.increaseFailedDeltaSync();

        final int dpsFailedSyncInvocations = deltaDpsHandlerInstrumentation.getDpsFailedDeltaSync();
        assertEquals(EXPECTED_FAILED_SYNCS_COUNTER, dpsFailedSyncInvocations);
    }

    @Test
    public void testAverageNumberOfAttributesBeingSynced() {
        for (final long sample : CALCULATION_SAMPLES) {
            deltaDpsHandlerInstrumentation.addToNumberOfAttrBeingSyncedSamples(sample);
        }

        final long avgNumberOfAttributesBeingSynced = deltaDpsHandlerInstrumentation.getAverageNumberOfAttrBeingSynced();
        assertEquals(EXPECTED_AVG, avgNumberOfAttributesBeingSynced);
    }

    @Test
    public void testAverageTimeTakenToPersistAttributes() {
        for (final long sample : CALCULATION_SAMPLES) {
            deltaDpsHandlerInstrumentation.addToDpsPersistAttributesAvgTimeSamples(sample);
        }

        final long dpsPersistAttributesAvgTime = deltaDpsHandlerInstrumentation.getDpsPersistAttributesAvgTime();
        assertEquals(EXPECTED_AVG, dpsPersistAttributesAvgTime);
    }

    @Test
    public void testAverageOverallDeltaSyncTimeTaken() {
        for (final long sample : CALCULATION_SAMPLES) {
            deltaDpsHandlerInstrumentation.addToAverageOverallDeltaSyncTimesTaken(sample);
        }

        final long dpsPersistAttributesAvgTime = deltaDpsHandlerInstrumentation.getAverageOverallDeltaSyncTimeTaken();
        assertEquals(EXPECTED_AVG, dpsPersistAttributesAvgTime);
    }

    @Test
    public void testAverageOverallNodeInfoDeltaSyncTimeTaken() {
        for (final long sample : CALCULATION_SAMPLES) {
            deltaDpsHandlerInstrumentation.addToAverageOverallNodeInfoDeltaSyncTimeTaken(sample);
        }

        final long dpsPersistAttributesAvgTime = deltaDpsHandlerInstrumentation.getAverageOverallNodeInfoDeltaSyncTimeTaken();
        assertEquals(EXPECTED_AVG, dpsPersistAttributesAvgTime);
    }

    @Test
    public void testDpsDeltaInvocationAttributeSync() {
        deltaDpsHandlerInstrumentation.setDpsDeltaInvocationAttributeSync(new AtomicInteger(EXPECTED_SUCCESSFUL_SYNCS_INVOCATION_COUNTER - 1));
        deltaDpsHandlerInstrumentation.increaseDpsDeltaInvocationAttributeSync();

        final int dpsSuccessfulSyncInvocations = deltaDpsHandlerInstrumentation.getDpsDeltaInvocationAttributeSync();
        assertEquals(EXPECTED_SUCCESSFUL_SYNCS_INVOCATION_COUNTER, dpsSuccessfulSyncInvocations);
    }

    private static long sum(final long[] elements) {
        long sum = 0L;

        for (final long element : elements) {
            sum += element;
        }

        return sum;
    }

}
