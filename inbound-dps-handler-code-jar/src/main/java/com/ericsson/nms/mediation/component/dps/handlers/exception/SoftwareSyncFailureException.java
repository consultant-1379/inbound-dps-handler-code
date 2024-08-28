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

package com.ericsson.nms.mediation.component.dps.handlers.exception;

/**
 * SoftwareSyncFailureException with error message.
 */
public class SoftwareSyncFailureException extends Exception {
    private static final long serialVersionUID = -2731965129217693078L;

    public SoftwareSyncFailureException(final String errorMessage) {
        super(errorMessage);
    }

}
