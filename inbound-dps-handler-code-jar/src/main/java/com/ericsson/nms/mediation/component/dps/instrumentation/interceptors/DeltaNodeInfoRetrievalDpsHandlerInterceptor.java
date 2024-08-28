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

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DeltaDpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.instrumentation.bindings.DeltaNodeInfoRetrievalDpsHandlerBinding;
import com.ericsson.nms.mediation.component.dps.utility.InstrumentationUtil;

/**
 * The DeltaNodeInfoRetrievalDpsHandlerInterceptor class.
 * <p> Has an interceptor method that interposes on business methods.
 * And persist the average time for node information to be delta sync</p>
 * */
@Interceptor
@DeltaNodeInfoRetrievalDpsHandlerBinding
public class DeltaNodeInfoRetrievalDpsHandlerInterceptor {

    @Inject
    protected DeltaDpsHandlerInstrumentation deltaDpsHandlerInstrumentation;

    @AroundInvoke
    public Object intercept(final InvocationContext invocationContext) {
        final long startTime = System.currentTimeMillis();
        Object contextResult;

        try {
            contextResult = invocationContext.proceed();
        } catch (final InboundDpsHandlerException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new InboundDpsHandlerException(exception);
        }

        addToAverageOverallNodeInfoDeltaSyncTimeTaken(startTime);

        return contextResult;
    }

    private void addToAverageOverallNodeInfoDeltaSyncTimeTaken(final long currentTime) {
        final Long overallSyncTimeTaken = InstrumentationUtil.calculateTimeTaken(currentTime);
        deltaDpsHandlerInstrumentation.addToAverageOverallNodeInfoDeltaSyncTimeTaken(overallSyncTimeTaken);
    }

}
