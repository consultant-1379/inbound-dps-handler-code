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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InboundDpsHandlerExceptionTest {

    private static final String EXCEPTION_MESSAGE = "Test exception";

    @Mock
    private Exception mockException;

    @Test
    public void testConstructors() throws Exception {
        new InboundDpsHandlerException(EXCEPTION_MESSAGE);
        new InboundDpsHandlerException(EXCEPTION_MESSAGE, mockException);
    }

}
