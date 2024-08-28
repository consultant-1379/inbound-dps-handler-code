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

import java.util.HashMap;
import java.util.Map;

import javax.interceptor.InvocationContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.DELTA_SYNC_START_TIME_HEADER_NAME;

import org.mockito.Matchers;
import org.mockito.Mockito;

import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DeltaDpsHandlerInstrumentation;
import com.ericsson.oss.itpf.common.event.ComponentEvent;

@RunWith(MockitoJUnitRunner.class)
public class DeltaSyncOverallTimeTakenInterceptorTest {

    private static final long SAMPLE_TIME_TAKEN = 0L;
    private static final String EXCEPTION_MESSAGE = "DeltaSyncOverallTimeTakenInterceptor Exception Message";

    @Mock
    private ComponentEvent mockEvent;
    @Mock
    private InvocationContext mockInvocationContext;
    @Mock
    private DeltaDpsHandlerInstrumentation mockInstrumentation;

    @InjectMocks
    private DeltaSyncOverallTimeTakenInterceptor deltaSyncOverallTimeTakenInterceptor;

    @Before
    public void setUp() throws Exception {
        final Map<String, Object> eventHeaders = new HashMap<>();
        eventHeaders.put(DELTA_SYNC_START_TIME_HEADER_NAME, SAMPLE_TIME_TAKEN);

        final Object[] parameters = new Object[1];
        parameters[0] = mockEvent;

        when(mockInvocationContext.getParameters()).thenReturn(parameters);
        when(mockInvocationContext.proceed()).thenReturn(new Object());
        when(mockEvent.getHeaders()).thenReturn(eventHeaders);
    }

    @Test
    public void testIntercept_incrementCounterSuccess() {
        deltaSyncOverallTimeTakenInterceptor.intercept(mockInvocationContext);
        Mockito.verify(mockInstrumentation).addToAverageOverallDeltaSyncTimesTaken(Matchers.anyLong());
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testincreaseFailedDeltaSync_Success() throws Exception {
        when(mockInvocationContext.proceed()).thenThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE));
        deltaSyncOverallTimeTakenInterceptor.intercept(mockInvocationContext);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testFailDuetoException_Success() throws Exception {
        when(mockInvocationContext.proceed()).thenThrow(new RuntimeException(EXCEPTION_MESSAGE));
        deltaSyncOverallTimeTakenInterceptor.intercept(mockInvocationContext);
    }
}
