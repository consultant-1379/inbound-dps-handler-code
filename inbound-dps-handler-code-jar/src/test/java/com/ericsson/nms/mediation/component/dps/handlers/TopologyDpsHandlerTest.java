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

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ATTRIBUTE_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.TOPOLOGY_SYNC_STATUS_ATTR_VALUE;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.NetworkElementMoDpsOperator;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.exception.SupervisionNotActiveException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.TopologyWriteDpsOperator;
import com.ericsson.nms.mediation.component.dps.test.util.TestUtil;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.config.Configuration;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;

@RunWith(MockitoJUnitRunner.class)
public class TopologyDpsHandlerTest {

    private static final String OSS_PREFIX = "MeContext=Erbs01";
    private static final String NETWORK_ELEMENT_FDN = "NetworkElement=Erbs01";
    private static final String CM_FUNCTION_FDN = NETWORK_ELEMENT_FDN + ",CmFunction=1";
    private static final String EXCEPTION_MESSAGE = "Test exception!";
    private static final int TOTAL_ELEMENTS = 2000;

    private Map<String, Object> handlerHeaders;
    private Map<String, Object> initHeaders;

    @Mock
    private EventHandlerContext mockContext;
    @Mock
    private ComponentEvent mockEvent;
    @Mock
    private Configuration mockConfig;
    @Mock
    private CmFunctionMoDpsOperator mockCmFunctionMoDpsOperator;
    @Mock
    private SystemRecorder mockSystemRecorder;
    @Mock
    private DpsHandlerInstrumentation mockInstrumentation;
    @Mock
    private TopologyWriteDpsOperator mockTopologyWriteDpsOperator;
    @Mock
    private NetworkElementMoDpsOperator mockNetworkElementMoDpsOperator;
    @Mock
    private Object mockPayload;
    @Mock
    private ThreadLocal<Map<String, Object>> mockContextHeaders;
    @Mock
    private FailedSyncResultDpsOperator failedSyncResultDpsOperatorMock;

    @InjectMocks
    private TopologyDpsHandler topologyDpsHandler;

    @Before
    public void setUp() throws Exception {
        // configuration and context mocks
        initHeaders = new HashMap<>();
        initHeaders.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, CM_FUNCTION_FDN);

        when(mockConfig.getStringProperty(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME)).thenReturn(CM_FUNCTION_FDN);
        when(mockConfig.getAllProperties()).thenReturn(initHeaders);
        when(mockContext.getEventHandlerConfiguration()).thenReturn(mockConfig);

        // event mock
        handlerHeaders = new HashMap<>();
        handlerHeaders.put(TopologyDpsHandler.class.getCanonicalName(), new HashMap<>());
        handlerHeaders.put(MiscellaneousConstants.OSS_PREFIX_HEADER_NAME, OSS_PREFIX);
        when(mockEvent.getHeaders()).thenReturn(handlerHeaders);
        when(mockEvent.getPayload()).thenReturn(mockPayload);
        handlerHeaders.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, CM_FUNCTION_FDN);

        // contextHeaders mock
        when(mockContextHeaders.get()).thenReturn(initHeaders);
        TestUtil.setFieldValue("contextHeaders", mockContextHeaders, TopologyDpsHandler.class, topologyDpsHandler);

        // other mocks
        when(mockNetworkElementMoDpsOperator.getOssPrefix(NETWORK_ELEMENT_FDN)).thenReturn(OSS_PREFIX);
        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(),
                anyString(), any(Exception.class), anyString());

        topologyDpsHandler.init(mockContext);
        verify(mockContextHeaders).set(initHeaders);
    }

    @Test
    public void testOnEvent() throws Exception {
        when(mockTopologyWriteDpsOperator.persistTopology(mockEvent, OSS_PREFIX, Boolean.FALSE)).thenReturn(TOTAL_ELEMENTS);
        when(mockCmFunctionMoDpsOperator.getSyncStatus(CM_FUNCTION_FDN)).thenReturn(TOPOLOGY_SYNC_STATUS_ATTR_VALUE);

        final ComponentEvent resultEvent = topologyDpsHandler.onEvent(mockEvent);
        verify(mockTopologyWriteDpsOperator).persistTopology(mockEvent, OSS_PREFIX, Boolean.FALSE);
        verify(mockCmFunctionMoDpsOperator).setSyncStatus(CM_FUNCTION_FDN, OSS_PREFIX, ATTRIBUTE_SYNC_STATUS_ATTR_VALUE, null);

        assertEquals(new MediationComponentEvent(handlerHeaders, mockPayload), resultEvent);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_PayloadIsException() throws Exception {
        when(mockEvent.getPayload()).thenReturn(new Exception());
        topologyDpsHandler.onEvent(mockEvent);
        verify(mockCmFunctionMoDpsOperator, never()).setSyncStatus(CM_FUNCTION_FDN, SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void testOnEvent_EventIsNull() throws Exception {
        topologyDpsHandler.onEvent(null);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_CatchesSyncStatusException() throws Exception {
        doThrow(new SupervisionNotActiveException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock)
            .checkHeartbeatSupervision(CM_FUNCTION_FDN);

        topologyDpsHandler.onEvent(mockEvent);
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(), anyString(), any(Exception.class), anyString());
        verify(mockCmFunctionMoDpsOperator, never()).setSyncStatus(CM_FUNCTION_FDN, OSS_PREFIX, TOPOLOGY_SYNC_STATUS_ATTR_VALUE,
                false);
    }

}
