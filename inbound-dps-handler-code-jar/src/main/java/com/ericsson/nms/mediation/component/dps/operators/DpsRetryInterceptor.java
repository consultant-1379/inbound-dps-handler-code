/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.operators;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
/**
 * Interceptor class responsible for performing retry operations.
 */
public class DpsRetryInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DpsRetryInterceptor.class);

    @AroundInvoke
    @SuppressWarnings("PMD")
    public Object methodInterceptor(final InvocationContext context) throws Exception {
        int retryCount = 0;
        while (retryCount < DpsRetryExceptionHandler.MAX_NUM_RETRIES) {
            try {
                if (retryCount > 0) {
                    Thread.sleep(getRandomOffset());
                }
                LOGGER.trace("Method {} is being executed", context.getMethod().getName());
                return context.proceed();
            } catch (final RuntimeException e) {
                DpsRetryExceptionHandler.handleRetry(e, retryCount);
                retryCount++;
            }
        }
        throw new InboundDpsHandlerException("The DPS operation failed after " + (retryCount + 1) + " retries.");
    }

    private int getRandomOffset() {
        return (int) (Math.random() * 9) + 2;
    }
}
