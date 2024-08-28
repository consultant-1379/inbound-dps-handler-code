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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.PENDING_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;

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
import com.ericsson.nms.mediation.component.dps.event.MediationEventSender;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.exception.SupervisionNotActiveException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.cm.events.StartSyncEvent;

@RunWith(MockitoJUnitRunner.class)
public class FullSyncEventSenderHandlerTest {
    private static final String NODE_NAME = "VooDoo";
    private static final String OSS_PREFIX_HEADER_VALUE = "MeContext=VooDoo";
    private static final String CM_FUNCTION_FDN = FdnUtil.appendCmFunction(FdnUtil.getNetworkElementFdn(NODE_NAME));
    private static final String EXCEPTION_MESSAGE = "Test exception!";
    private static final long PO_ID_HEADER_VALUE = 12345L;

    @Mock
    private EventHandlerContext contextMock;
    @Mock
    private ComponentEvent eventMock;
    @Mock
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperatorMock;
    @Mock
    private CmSupervisionMoDpsOperator cmSupervisionMoDpsOperatorMock;
    @Mock
    private MediationEventSender mediationEventSenderMock;
    @Mock
    private SystemRecorder systemRecorderMock;
    @Mock
    private DpsHandlerInstrumentation instrumentationMock;
    @Mock
    private FailedSyncResultDpsOperator failedSyncResultDpsOperatorMock;

    @InjectMocks
    private FullSyncEventSenderHandler fullSyncEventSenderHandler;

    @Before
    public void setup() throws Exception {
        // event mock
        final Map<String, Object> handlerHeaders = new HashMap<>();
        handlerHeaders.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, CM_FUNCTION_FDN);
        handlerHeaders.put(MiscellaneousConstants.OSS_PREFIX_HEADER_NAME, OSS_PREFIX_HEADER_VALUE);
        handlerHeaders.put(MiscellaneousConstants.PO_ID_HEADER_NAME, PO_ID_HEADER_VALUE);
        when(eventMock.getHeaders()).thenReturn(handlerHeaders);

        fullSyncEventSenderHandler.init(contextMock);
    }

    @Test
    public void testOnEvent() throws Exception {
        when(cmSupervisionMoDpsOperatorMock.getActive(FdnUtil.convertToCmNodeHeartbeatSupervision(CM_FUNCTION_FDN))).thenReturn(true);
        fullSyncEventSenderHandler.onEvent(eventMock);
        verify(cmFunctionMoDpsOperatorMock).setSyncStatus(CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE, PENDING_SYNC_STATUS_ATTR_VALUE, null);
        verify(mediationEventSenderMock).send(any(StartSyncEvent.class));
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_EventSenderException() throws Exception {
        doThrow(new RuntimeException()).when(mediationEventSenderMock).send(any(StartSyncEvent.class));
        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(),
                anyString(), any(Exception.class), anyString());

        fullSyncEventSenderHandler.onEvent(eventMock);
        verify(cmFunctionMoDpsOperatorMock).setSyncStatus(CM_FUNCTION_FDN, UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void testOnEvent_NullEvent() throws Exception {
        fullSyncEventSenderHandler.onEvent(null);
        verify(mediationEventSenderMock, never()).send(any(StartSyncEvent.class));
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_CatchesHeartbeatException() throws Exception {
        doThrow(new SupervisionNotActiveException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock)
                .checkHeartbeatSupervision(CM_FUNCTION_FDN);
        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(),
                anyString(), any(Exception.class), anyString());

        fullSyncEventSenderHandler.onEvent(eventMock);
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(), anyString(), any(Exception.class), anyString());
    }

}
