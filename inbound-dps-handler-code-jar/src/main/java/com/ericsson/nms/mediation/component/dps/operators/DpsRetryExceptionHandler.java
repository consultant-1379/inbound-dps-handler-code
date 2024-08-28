/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.operators;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception handler class used to handle the DPS operations retry.
 */
public abstract class DpsRetryExceptionHandler {

    public static final int MAX_NUM_RETRIES = 3;
    private static final Logger LOGGER = LoggerFactory.getLogger(DpsRetryExceptionHandler.class);
    private static final String[] RETRIABLE_EXCEPTIONS = {
            "javax.persistence.PersistenceException",
            "javax.persistence.OptimisticLockException",
            "javax.ejb.EJBTransactionRolledbackException"
    };

    public static void handleRetry(final RuntimeException exception, final int retriesCount) {
        final String stackTrace = ExceptionUtils.getStackTrace(exception);
        if (!isRetriableException(stackTrace)) {
            LOGGER.warn("Has encountered a non-retriable exception of type {}. Will not try again.", exception.getClass());
            throw exception;
        }
        LOGGER.warn("Has encountered a retriable exception of type {}. Will try again, attempt number {}.",
                exception.getClass(), retriesCount);
    }

    private static boolean isRetriableException(final String stackTrace) {
        for (final String throwable : RETRIABLE_EXCEPTIONS) {
            if (stackTrace.contains(throwable)) {
                return true;
            }
        }
        return false;
    }
}
