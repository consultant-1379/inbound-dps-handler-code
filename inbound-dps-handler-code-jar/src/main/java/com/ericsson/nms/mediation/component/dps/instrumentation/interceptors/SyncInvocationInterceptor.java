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

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SYNC_TYPE_HEADER_NAME;

import java.util.Map;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ericsson.nms.mediation.component.dps.common.SyncType;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DeltaDpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.instrumentation.bindings.SyncInvocationBinding;
import com.ericsson.oss.itpf.common.event.ComponentEvent;

/**
 * The SyncInvocationInterceptor class.
 * <p>  Has an interceptor method that interposes on business methods.
 * And get SYNC_TYPE_HEADER_NAME method</p>
 */
@Interceptor
@SyncInvocationBinding
public class SyncInvocationInterceptor {

    @Inject
    private DeltaDpsHandlerInstrumentation deltaDpsHandlerInstrumentation;

    @AroundInvoke
    public Object intercept(final InvocationContext invocationContext) {
        final String syncType = retrieveSyncTypeHeader(invocationContext.getParameters());
        if (syncType.equals(SyncType.DELTA.getType())) {
            deltaDpsHandlerInstrumentation.increaseDpsDeltaInvocationAttributeSync();
        }

        Object contextResult;

        try {
            contextResult = invocationContext.proceed();
        } catch (final InboundDpsHandlerException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new InboundDpsHandlerException(exception);
        }

        return contextResult;
    }

    private String retrieveSyncTypeHeader(final Object[] invocationContextParameters) {
        final ComponentEvent event = (ComponentEvent) invocationContextParameters[0];
        final Map<String, Object> eventHeaders = event.getHeaders();
        return (String) eventHeaders.get(SYNC_TYPE_HEADER_NAME);
    }

}