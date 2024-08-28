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

import static org.mockito.Mockito.when;

import javax.interceptor.InvocationContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DeltaDpsHandlerInstrumentation;

@RunWith(MockitoJUnitRunner.class)
public class DeltaNodeInfoRetrievalDpsHandlerInterceptorTest {
    private static final String EXCEPTION_MESSAGE = "DeltaNodeInfoRetrievalDpsHandlerInterceptorTest Exception Message";

    @Mock
    private InvocationContext mockInvocationContext;
    @Mock
    private DeltaDpsHandlerInstrumentation mockInstrumentation;

    @InjectMocks
    private DeltaNodeInfoRetrievalDpsHandlerInterceptor deltaNodeInfoRetrievalDpsHandlerInterceptor;

    @Before
    public void setUp() throws Exception {
        when(mockInvocationContext.proceed()).thenReturn(new Object());
    }

    @Test
    public void testAddToAverageOverallNodeInfoDeltaSyncTimeTaken_Success() {
        deltaNodeInfoRetrievalDpsHandlerInterceptor.intercept(mockInvocationContext);
        Mockito.verify(mockInstrumentation).addToAverageOverallNodeInfoDeltaSyncTimeTaken(Matchers.anyLong());
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testThrowsInboundDpsHandlerException_Success() throws Exception {
        when(mockInvocationContext.proceed()).thenThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE));
        deltaNodeInfoRetrievalDpsHandlerInterceptor.intercept(mockInvocationContext);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testThrowsRuntimeException_Success() throws Exception {
        when(mockInvocationContext.proceed()).thenThrow(new RuntimeException(EXCEPTION_MESSAGE));
        deltaNodeInfoRetrievalDpsHandlerInterceptor.intercept(mockInvocationContext);
    }

}
