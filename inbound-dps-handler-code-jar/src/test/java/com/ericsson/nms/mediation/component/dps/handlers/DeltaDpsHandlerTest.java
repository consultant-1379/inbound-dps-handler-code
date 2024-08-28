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

package com.ericsson.nms.mediation.component.dps.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ME_CONTEXT_MO_TYPE;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.CmSupervisionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.exception.SupervisionNotActiveException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.operators.dps.DeltaSyncDpsOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

@RunWith(MockitoJUnitRunner.class)
public class DeltaDpsHandlerTest {
    private static final String NODE_NAME = "VooDoo";
    private static final String ME_CONTEXT_FDN = ME_CONTEXT_MO_TYPE + "=" + NODE_NAME;

    private static final String CM_FUNCTION_FDN = FdnUtil.appendCmFunction(FdnUtil.getNetworkElementFdn(NODE_NAME));

    private static final String EXCEPTION_MESSAGE = "Test exception!";
    private static final long DELTA_SYNC_START_TIME = 12000L;

    @Mock
    private EventHandlerContext contextMock;
    @Mock
    private ComponentEvent eventMock;
    @Mock
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperatorMock;
    @Mock
    private CmSupervisionMoDpsOperator cmSupervisionMoDpsOperatorMock;
    @Mock
    private SystemRecorder systemRecorderMock;
    @Mock
    private DpsHandlerInstrumentation instrumentationMock;
    @Mock
    private DeltaSyncDpsOperator deltaSyncDpsOperatorMock;
    @Mock
    private FailedSyncResultDpsOperator failedSyncResultDpsOperatorMock;

    @InjectMocks
    private DeltaDpsHandler deltaSyncDpsHandler;

    @Before
    public void setUp() throws Exception {
        // event mock
        final Map<String, Object> eventHeaders = new HashMap<>();
        eventHeaders.put(AttributeDpsHandler.class.getCanonicalName(), new HashMap<>());
        eventHeaders.put(MiscellaneousConstants.DELTA_SYNC_START_TIME_HEADER_NAME, DELTA_SYNC_START_TIME);
        eventHeaders.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, CM_FUNCTION_FDN);
        eventHeaders.put(MiscellaneousConstants.OSS_PREFIX_HEADER_NAME, ME_CONTEXT_FDN);
        when(eventMock.getHeaders()).thenReturn(eventHeaders);
        when(eventMock.getPayload()).thenReturn(new Object());

        deltaSyncDpsHandler.init(contextMock);
    }

    @Test
    public void testOnEvent() throws Exception {
        when(cmSupervisionMoDpsOperatorMock.getActive(FdnUtil.convertToCmNodeHeartbeatSupervision(CM_FUNCTION_FDN))).thenReturn(true);
        final ComponentEvent resultEvent = deltaSyncDpsHandler.onEvent(eventMock);
        verify(cmFunctionMoDpsOperatorMock).updateAttrsUponSuccessfulSync(CM_FUNCTION_FDN, ME_CONTEXT_FDN);
        assertEquals(eventMock, resultEvent);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_CatchesException() throws Exception {
        when(deltaSyncDpsOperatorMock.persistDeltaSyncChanges(eventMock, ME_CONTEXT_FDN)).thenThrow(new RuntimeException(EXCEPTION_MESSAGE));
        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(),
                anyString(), any(Exception.class), anyString());

        deltaSyncDpsHandler.onEvent(eventMock);
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(), anyString(), any(Exception.class), anyString());
    }

    @Test(expected = NullPointerException.class)
    public void testOnEvent_EventIsNull() throws Exception {
        deltaSyncDpsHandler.onEvent(null);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_CatchesHeartbeatException() throws Exception {
        doThrow(new SupervisionNotActiveException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock)
                .checkHeartbeatSupervision(CM_FUNCTION_FDN);
        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(),
                anyString(), any(Exception.class), anyString());

        deltaSyncDpsHandler.onEvent(eventMock);
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(), anyString(), any(Exception.class), anyString());
    }

}
