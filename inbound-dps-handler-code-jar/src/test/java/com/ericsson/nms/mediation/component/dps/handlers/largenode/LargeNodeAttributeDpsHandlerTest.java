/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.handlers.largenode;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ATTRIBUTE_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.CM_FUNCTION_MO_RDN;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ME_CONTEXT_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.IS_FIRST_SYNC_FLAG;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.SYNC_START_TIME_HEADER_NAME;

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
import com.ericsson.nms.mediation.component.dps.operators.dps.AttributeWriteDpsOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

@RunWith(MockitoJUnitRunner.class)
public class LargeNodeAttributeDpsHandlerTest {

    private static final String NODE_NAME = "VooDoo";
    private static final String OSS_PREFIX_HEADER_VALUE = ME_CONTEXT_MO_TYPE + "=" + NODE_NAME;
    private static final String CM_FUNCTION_FDN = FdnUtil.getNetworkElementFdn(NODE_NAME) + CM_FUNCTION_MO_RDN;
    private static final int TOTAL_ELEMENTS = 50000;

    private static final long START_TIME = 1000L;
    private static final String EXCEPTION_MESSAGE = "Test exception!";

    @Mock
    private EventHandlerContext contextMock;
    @Mock
    private ComponentEvent eventMock;
    @Mock
    private SystemRecorder systemRecorderMock;
    @Mock
    private DpsHandlerInstrumentation instrumentationMock;
    @Mock
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperatorMock;
    @Mock
    private CmSupervisionMoDpsOperator cmSupervisionMoDpsOperatorMock;
    @Mock
    private AttributeWriteDpsOperator attributeWriteDpsOperatorMock;
    @Mock
    private FailedSyncResultDpsOperator failedSyncResultDpsOperatorMock;

    @InjectMocks
    private LargeNodeAttributeDpsHandler largeNodeAttributeDpsHandler;

    @Before
    public void setUp() throws Exception {
        final Map<String, Object> handlerHeaders = new HashMap<>();
        handlerHeaders.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, CM_FUNCTION_FDN);
        handlerHeaders.put(MiscellaneousConstants.OSS_PREFIX_HEADER_NAME, OSS_PREFIX_HEADER_VALUE);
        handlerHeaders.put(SYNC_START_TIME_HEADER_NAME, START_TIME);
        handlerHeaders.put(IS_FIRST_SYNC_FLAG, Boolean.FALSE);
        when(eventMock.getHeaders()).thenReturn(handlerHeaders);
        when(eventMock.getPayload()).thenReturn(new Object());
        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(),
                anyString(), any(Exception.class), anyString());

        largeNodeAttributeDpsHandler.init(contextMock);
    }

    @Test
    public void testOnEvent() throws Exception {
        when(attributeWriteDpsOperatorMock.handleAttributesPersistence(eventMock, OSS_PREFIX_HEADER_VALUE, Boolean.FALSE)).thenReturn(TOTAL_ELEMENTS);
        when(cmSupervisionMoDpsOperatorMock.getActive(FdnUtil.convertToCmNodeHeartbeatSupervision(CM_FUNCTION_FDN))).thenReturn(true);

        largeNodeAttributeDpsHandler.onEvent(eventMock);
        verify(attributeWriteDpsOperatorMock).handleAttributesPersistence(eventMock, OSS_PREFIX_HEADER_VALUE, Boolean.FALSE);
        verify(cmFunctionMoDpsOperatorMock).updateAttrsUponSuccessfulSync(CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE, Boolean.TRUE);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_CatchesException() throws Exception {
        when(attributeWriteDpsOperatorMock.handleAttributesPersistence(eventMock, OSS_PREFIX_HEADER_VALUE, Boolean.FALSE))
            .thenThrow(new RuntimeException(EXCEPTION_MESSAGE));

        largeNodeAttributeDpsHandler.onEvent(eventMock);
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(), anyString(), any(Exception.class), anyString());
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_PayloadIsException() throws Exception {
        when(eventMock.getPayload()).thenReturn(new Exception());
        largeNodeAttributeDpsHandler.onEvent(eventMock);
        verify(cmFunctionMoDpsOperatorMock, never()).setSyncStatus(CM_FUNCTION_FDN, SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void testOnEvent_EventIsNull() throws Exception {
        largeNodeAttributeDpsHandler.onEvent(null);
        verify(cmFunctionMoDpsOperatorMock, never()).setSyncStatus(CM_FUNCTION_FDN, ATTRIBUTE_SYNC_STATUS_ATTR_VALUE);
        verify(instrumentationMock, never()).increaseDpsAttributeSyncInvocations();
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_CatchesHeartbeatException() throws Exception {
        doThrow(new SupervisionNotActiveException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock)
                .checkHeartbeatSupervision(CM_FUNCTION_FDN);

        largeNodeAttributeDpsHandler.onEvent(eventMock);
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(), anyString(), any(Exception.class), anyString());
    }

}
