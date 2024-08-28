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

package com.ericsson.nms.mediation.component.dps.exception;

/**
 * Custom runtime exception signaling a problem specific to one of the In-bound DPS handlers.
 */
public class InboundDpsHandlerException extends RuntimeException {
    private static final long serialVersionUID = -4726708227623824220L;

    public InboundDpsHandlerException(final String message) {
        super(message);
    }

    public InboundDpsHandlerException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InboundDpsHandlerException(final Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
