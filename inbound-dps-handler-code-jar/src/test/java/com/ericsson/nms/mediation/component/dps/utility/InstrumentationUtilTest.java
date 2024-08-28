/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InstrumentationUtilTest {

    private static final int NO_ELEMENTS = 10;

    private List<Long> timestamps;
    private long timestampsAverage;

    @Test
    public void testCalculateTimeTaken() {
        final long startTime = 0L;

        final long timeTaken = InstrumentationUtil.calculateTimeTaken(startTime);
        assertTrue(timeTaken > 0L);
    }

    @Test
    public void testCalculateAverageAndReset() {
        setupSampleTimestamps();

        final long calculatedAverage = InstrumentationUtil.calculateAverageAndReset(timestamps);
        assertEquals(timestampsAverage, calculatedAverage);
        assertTrue(timestamps.size() == 0);
    }

    @Test
    public void testCalculateMaxAndReset() {
        setupSampleTimestamps();

        final long max = Collections.max(timestamps);
        final long calculatedMax = InstrumentationUtil.calculateMaxAndReset(timestamps);
        assertEquals(max, calculatedMax);
        assertTrue(timestamps.size() == 0);
    }

    @Test
    public void testCalculateAverageAndReset_EmptyList() {
        final long calculatedAverage = InstrumentationUtil.calculateAverageAndReset(new ArrayList<Long>());
        assertEquals(0L, calculatedAverage);
    }

    @Test
    public void testCalculateMaxAndReset_EmptyList() {
        final long calculatedMax = InstrumentationUtil.calculateMaxAndReset(new ArrayList<Long>());
        assertEquals(0L, calculatedMax);
    }

    private void setupSampleTimestamps() {
        timestampsAverage = 0L;
        long timestampsSum = 0L;

        timestamps = new ArrayList<Long>();
        for (int i = 0; i < NO_ELEMENTS; i++) {
            final long timestamp = generateTimestamp();
            timestamps.add(timestamp);
            timestampsSum += timestamp;
        }

        timestampsAverage = timestampsSum / NO_ELEMENTS;
    }

    private long generateTimestamp() {
        final Random numberGenerator = new Random();

        long timestamp = -1L;
        while (timestamp < 0L) {
            timestamp = numberGenerator.nextLong() / NO_ELEMENTS;
        }

        return timestamp;
    }

}
