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

package com.ericsson.nms.mediation.component.dps.instrumentation.interceptors;

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.DELTA_SYNC_START_TIME_HEADER_NAME;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DeltaDpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.instrumentation.bindings.DeltaSyncOverAllTimeTakenBinding;
import com.ericsson.nms.mediation.component.dps.utility.InstrumentationUtil;
import com.ericsson.oss.itpf.common.event.ComponentEvent;

/**
 * The DeltaSyncOverallTimeTakenInterceptor class.
 * <p>  Has an interceptor method that interposes on business methods.
 * And persist the time that delta sync performed</p>
 */
@Interceptor
@DeltaSyncOverAllTimeTakenBinding
public class DeltaSyncOverallTimeTakenInterceptor {
    @Inject
    private DeltaDpsHandlerInstrumentation deltaDpsHandlerInstrumentation;

    @AroundInvoke
    public Object intercept(final InvocationContext invocationContext) {
        final long startTime = retrieveDeltaSyncStartTimeHeader(invocationContext.getParameters());
        Object contextResult;

        try {
            contextResult = invocationContext.proceed();
        } catch (final InboundDpsHandlerException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new InboundDpsHandlerException(exception);
        }

        addToAverageOverallDeltaSyncTimesTaken(startTime);

        return contextResult;
    }

    private long retrieveDeltaSyncStartTimeHeader(final Object[] invocationContextParameters) {
        final ComponentEvent event = (ComponentEvent) invocationContextParameters[0];
        return (Long) event.getHeaders().get(DELTA_SYNC_START_TIME_HEADER_NAME);
    }

    private void addToAverageOverallDeltaSyncTimesTaken(final long deltaSyncStartTime) {
        final Long overallSyncTimeTaken = InstrumentationUtil.calculateTimeTaken(deltaSyncStartTime);
        deltaDpsHandlerInstrumentation.addToAverageOverallDeltaSyncTimesTaken(overallSyncTimeTaken);
    }

}
