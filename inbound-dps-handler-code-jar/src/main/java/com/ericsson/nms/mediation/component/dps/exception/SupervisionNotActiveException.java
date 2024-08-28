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

package com.ericsson.nms.mediation.component.dps.exception;

/**
 * Custom runtime exception signaling a problem specific changing sync state.
 */
public class SupervisionNotActiveException extends RuntimeException {

    private static final long serialVersionUID = -267364975807258913L;

    public SupervisionNotActiveException(final String message) {
        super(message);
    }

    public SupervisionNotActiveException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SupervisionNotActiveException(final Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
