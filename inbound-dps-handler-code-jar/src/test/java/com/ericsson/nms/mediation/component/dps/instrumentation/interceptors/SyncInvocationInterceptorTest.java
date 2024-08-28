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

import java.util.HashMap;
import java.util.Map;

import javax.interceptor.InvocationContext;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SYNC_TYPE_HEADER_NAME;

import com.ericsson.nms.mediation.component.dps.common.SyncType;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DeltaDpsHandlerInstrumentation;
import com.ericsson.oss.itpf.common.event.ComponentEvent;

/**
 * SyncInvocationInterceptorTest.
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncInvocationInterceptorTest {

    private static final String EXCEPTION_MESSAGE = "Test Exception";

    final Map<String, Object> eventHeaders = new HashMap<>();

    @Mock
    private ComponentEvent mockEvent;
    @Mock
    private InvocationContext mockInvocationContext;
    @Mock
    private DeltaDpsHandlerInstrumentation mockInstrumentation;

    @InjectMocks
    private SyncInvocationInterceptor syncInvocationInterceptor;

    @Before
    public void setUp() throws Exception {
        final Object[] parameters = new Object[1];
        parameters[0] = mockEvent;

        when(mockInvocationContext.getParameters()).thenReturn(parameters);
        when(mockEvent.getHeaders()).thenReturn(eventHeaders);
        eventHeaders.put(SYNC_TYPE_HEADER_NAME, SyncType.DELTA.getType());
    }

    @Test
    public void testIntercept_incrementDeltaCounter_Success() throws Exception {
        when(mockInvocationContext.proceed()).thenReturn(new Object());

        syncInvocationInterceptor.intercept(mockInvocationContext);
        Mockito.verify(mockInstrumentation).increaseDpsDeltaInvocationAttributeSync();
    }

    @Test
    public void testIntercept_incrementDeltaCounter_Failure() throws Exception {
        when(mockInvocationContext.proceed()).thenReturn(new Object());
        eventHeaders.put(SYNC_TYPE_HEADER_NAME, SyncType.FULL.getType());

        syncInvocationInterceptor.intercept(mockInvocationContext);
        Mockito.verifyNoMoreInteractions(mockInstrumentation);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testThrowsInboundDpsHandlerException_Success() throws Exception {
        when(mockInvocationContext.proceed()).thenThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE));
        syncInvocationInterceptor.intercept(mockInvocationContext);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testThrowsRuntimeException_Success() throws Exception {
        when(mockInvocationContext.proceed()).thenThrow(new RuntimeException(EXCEPTION_MESSAGE));
        syncInvocationInterceptor.intercept(mockInvocationContext);
    }

}
