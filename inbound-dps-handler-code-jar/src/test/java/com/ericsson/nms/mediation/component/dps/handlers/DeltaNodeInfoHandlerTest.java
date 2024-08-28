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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.DELTA_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ME_CONTEXT_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.PENDING_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.GENERATION_COUNTER_HEADER_NAME;
import static com.googlecode.catchexception.CatchException.verifyException;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.exception.SupervisionNotActiveException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.operators.dps.DeltaNodeInfoReadDpsOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.nms.mediation.component.dps.test.util.TestUtil;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.config.Configuration;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;

@RunWith(MockitoJUnitRunner.class)
public class DeltaNodeInfoHandlerTest {
    private static final String NODE_NAME = "VooDoo";
    private static final String ME_CONTEXT_FDN = ME_CONTEXT_MO_TYPE + "=" + NODE_NAME;
    private static final String CM_FUNCTION_FDN = FdnUtil.appendCmFunction(FdnUtil.getNetworkElementFdn(NODE_NAME));
    private static final String EXCEPTION_MESSAGE = "Test exception!";
    private static final long GENERATION_COUNTER_HEADER_VALUE = 654L;

    private Map<String, Object> initHeaders;
    private Map<String, Object> handlerHeaders;

    @Mock
    private ThreadLocal<Map<String, Object>> contextHeadersMock;
    @Mock
    private Object payloadMock;
    @Mock
    private EventHandlerContext contextMock;
    @Mock
    private ComponentEvent eventMock;
    @Mock
    private Configuration configurationMock;
    @Mock
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperatorMock;
    @Mock
    private DeltaNodeInfoReadDpsOperator deltaNodeInfoReadDpsOperatorMock;
    @Mock
    private SystemRecorder systemRecorderMock;
    @Mock
    private DpsHandlerInstrumentation instrumentationMock;
    @Mock
    private FailedSyncResultDpsOperator failedSyncResultDpsOperatorMock;

    @InjectMocks
    private DeltaNodeInfoHandler deltaNodeInfoHandler;

    @Before
    public void setUp() throws Exception {
        // configuration and context mocks
        initHeaders = new HashMap<>();
        initHeaders.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, CM_FUNCTION_FDN);
        when(configurationMock.getStringProperty(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME)).thenReturn(CM_FUNCTION_FDN);
        when(configurationMock.getAllProperties()).thenReturn(initHeaders);
        when(contextMock.getEventHandlerConfiguration()).thenReturn(configurationMock);

        // contextHeaders mock
        when(contextHeadersMock.get()).thenReturn(initHeaders);
        TestUtil.setFieldValue("contextHeaders", contextHeadersMock, DeltaNodeInfoHandler.class, deltaNodeInfoHandler);

        // event mock
        final Map<String, Object> eventHeaders = new HashMap<>();
        eventHeaders.put(MiscellaneousConstants.OSS_PREFIX_HEADER_NAME, ME_CONTEXT_FDN);

        when(eventMock.getHeaders()).thenReturn(eventHeaders);

        // deltaNodeInfoReadDpsOperation mock
        handlerHeaders = new HashMap<>();
        handlerHeaders.put(MiscellaneousConstants.OSS_PREFIX_HEADER_NAME, ME_CONTEXT_FDN);
        handlerHeaders.put(GENERATION_COUNTER_HEADER_NAME, GENERATION_COUNTER_HEADER_VALUE);
        when(deltaNodeInfoReadDpsOperatorMock.createHeaders(CM_FUNCTION_FDN)).thenReturn(handlerHeaders);
        when(deltaNodeInfoReadDpsOperatorMock.createPayload(ME_CONTEXT_FDN)).thenReturn(payloadMock);
        handlerHeaders.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, CM_FUNCTION_FDN);

        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(),
                anyString(), any(Exception.class), anyString());

        deltaNodeInfoHandler.init(contextMock);
        verify(contextHeadersMock).set(initHeaders);
    }

    @Test
    public void testOnEvent() throws Exception {
        when(cmFunctionMoDpsOperatorMock.getSyncStatus(CM_FUNCTION_FDN)).thenReturn(PENDING_SYNC_STATUS_ATTR_VALUE);
        final ComponentEvent resultEvent = deltaNodeInfoHandler.onEvent(eventMock);
        final long handlerStartTime = (Long) resultEvent.getHeaders().get(MiscellaneousConstants.DELTA_SYNC_START_TIME_HEADER_NAME);
        handlerHeaders.put(MiscellaneousConstants.DELTA_SYNC_START_TIME_HEADER_NAME, handlerStartTime);

        verify(cmFunctionMoDpsOperatorMock).setSyncStatus(CM_FUNCTION_FDN, ME_CONTEXT_FDN, DELTA_SYNC_STATUS_ATTR_VALUE, null);
        verify(deltaNodeInfoReadDpsOperatorMock).createPayload(ME_CONTEXT_FDN);
        final ComponentEvent expectedEvent = new MediationComponentEvent(handlerHeaders, payloadMock);
        assertEquals(expectedEvent, resultEvent);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_CatchesSyncStatusException() throws Exception {
        doThrow(new SupervisionNotActiveException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock)
            .checkHeartbeatSupervision(CM_FUNCTION_FDN);

        deltaNodeInfoHandler.onEvent(eventMock);
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(), anyString(), any(Exception.class), anyString());
        verify(cmFunctionMoDpsOperatorMock, never()).setSyncStatus(CM_FUNCTION_FDN, ME_CONTEXT_FDN,
                DELTA_SYNC_STATUS_ATTR_VALUE, null);
        verify(deltaNodeInfoReadDpsOperatorMock, never()).createPayload(ME_CONTEXT_FDN);
    }

    @Test
    public void testOnEvent_ThrowsException() throws Exception {
        when(deltaNodeInfoReadDpsOperatorMock.createHeaders(CM_FUNCTION_FDN)).thenThrow(new RuntimeException());

        verifyException(deltaNodeInfoHandler, RuntimeException.class).onEvent(eventMock);
        verify(deltaNodeInfoReadDpsOperatorMock, never()).createPayload(ME_CONTEXT_FDN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOnEvent_EventIsNull() throws Exception {
        deltaNodeInfoHandler.onEvent(null);
        verify(contextHeadersMock, never()).get();
    }

}
