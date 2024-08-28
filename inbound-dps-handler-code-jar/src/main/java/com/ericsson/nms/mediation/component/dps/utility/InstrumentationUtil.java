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

import java.util.Collections;
import java.util.List;

/**
 * Provides helper methods for <code>DpsHandlerInstrumentation</code>.
 */
public final class InstrumentationUtil {

    private InstrumentationUtil() {}

    /**
     * Calculates how much time has elapsed since a given starting time.
     *
     * @param startTime
     *            start point in milliseconds
     * @return time elapsed since <code>startTime</code> in milliseconds
     */
    public static long calculateTimeTaken(final long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Calculates an average of a list of objects of type <code>Long</code>.
     * <p>
     * Note: Passed <code>list</code> is cleared in the process.
     *
     * @param list
     *            of type <code>Long</code>
     * @return average of the passed <code>list</code>
     */
    public static long calculateAverageAndReset(final List<Long> list) {
        long sum = 0L;
        long average = 0L;

        if (!list.isEmpty()) {
            for (final long number : list) {
                sum += number;
            }

            average = sum / list.size();
            list.clear();
        }

        return average;
    }

    /**
     * Calculates max of a list of objects of type <code>Long</code>.
     *
     * @param list
     *            of type <code>Long</code>
     * @return maximum value in the passed <code>list</code>
     */
    public static long calculateMaxAndReset(final List<Long> list) {
        long max = 0L;

        if (!list.isEmpty()) {
            max = Collections.max(list);
            list.clear();
        }

        return max;
    }

}
