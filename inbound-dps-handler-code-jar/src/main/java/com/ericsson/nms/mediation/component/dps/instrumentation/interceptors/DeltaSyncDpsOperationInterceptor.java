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
import com.ericsson.nms.mediation.component.dps.instrumentation.bindings.DeltaSyncDpsOperationBinding;

/**
 * THe DeltaSyncDpsOperationInterceptor class.
 * <p> Has an interceptor method that interposes on business methods that returns number of attribute proceed.</p>
 */
@Interceptor
@DeltaSyncDpsOperationBinding
public class DeltaSyncDpsOperationInterceptor {

    @Inject
    private DeltaDpsHandlerInstrumentation deltaDpsHandlerInstrumentation;

    @AroundInvoke
    public Object intercept(final InvocationContext invocationContext) {
        int totalAttrs;

        try {
            totalAttrs = (int) invocationContext.proceed();
        } catch (final InboundDpsHandlerException exception) {
            deltaDpsHandlerInstrumentation.increaseFailedDeltaSync();
            throw exception;
        } catch (final Exception exception) {
            throw new InboundDpsHandlerException(exception);
        }

        deltaDpsHandlerInstrumentation.increaseSuccessfulDeltaSync();
        deltaDpsHandlerInstrumentation.addToNumberOfAttrBeingSyncedSamples(Long.valueOf(totalAttrs));

        return totalAttrs;
    }
}
