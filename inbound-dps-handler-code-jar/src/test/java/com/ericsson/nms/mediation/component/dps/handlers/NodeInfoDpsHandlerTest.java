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

package com.ericsson.nms.mediation.component.dps.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.collection.IsMapContaining.hasValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.PENDING_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.TOPOLOGY_SYNC_STATUS_ATTR_VALUE;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.api.OssModelIdentityException;
import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.NetworkElementMoDpsOperator;
import com.ericsson.nms.mediation.component.dps.common.SyncType;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.exception.SupervisionNotActiveException;
import com.ericsson.nms.mediation.component.dps.handlers.exception.SoftwareSyncFailureException;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.config.Configuration;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

@RunWith(MockitoJUnitRunner.class)
public class NodeInfoDpsHandlerTest {

    private static final String INVOKING_FDN_HEADER_VALUE = "NetworkElement=VooDoo,CmFunction=1";
    private static final String OSS_PREFIX_HEADER_VALUE = "MeContext=VooDoo";
    private static final String OSS_MODEL_IDENTITY_VALUE = "111-2222-3333";
    private static final String SYNC_TYPE_HEADER_VALUE = SyncType.FULL.getType();
    private static final String NETWORK_ELEMENT_FDN = "NetworkElement=VooDoo";
    private static final String CM_FUNCTION_FDN = NETWORK_ELEMENT_FDN + ",CmFunction=1";
    private static final String EXCEPTION_MESSAGE = "Test exception!";
    private static final int SMALL_NODE_MO_INSTANCE_COUNT = 5000;
    private static final int LARGE_NODE_MO_INSTANCE_COUNT = 25000;

    @Mock
    private SystemRecorder systemRecorderMock;
    @Mock
    private EventHandlerContext eventHandlerContextMock;
    @Mock
    private ComponentEvent componentEventMock;
    @Mock
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperatorMock;
    @Mock
    private Configuration configurationMock;
    @Mock
    private NetworkElementMoDpsOperator networkElementMoOperatorMock;
    @Mock
    private FailedSyncResultDpsOperator failedSyncResultDpsOperatorMock;

    @InjectMocks
    private NodeInfoDpsHandler nodeInfoDpsHandler;

    @Before
    public void setUp() throws Exception {
        final Map<String, Object> eventHeaders = new HashMap<>();
        eventHeaders.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, INVOKING_FDN_HEADER_VALUE);

        // mockEvent
        when(componentEventMock.getPayload()).thenReturn(new Object());

        // other mocks
        when(eventHandlerContextMock.getEventHandlerConfiguration()).thenReturn(configurationMock);
        when(configurationMock.getAllProperties()).thenReturn(eventHeaders);

        nodeInfoDpsHandler.init(eventHandlerContextMock);
    }

    @Test
    public void onEvent() throws Exception {
        when(cmFunctionMoDpsOperatorMock.getSyncStatus(CM_FUNCTION_FDN)).thenReturn(PENDING_SYNC_STATUS_ATTR_VALUE);
        mockOssPrefixAndOssModelIdentity();

        final ComponentEvent resultEvent = nodeInfoDpsHandler.onEvent(componentEventMock);
        final Map<String, Object> eventHeaders = resultEvent.getHeaders();
        verify(cmFunctionMoDpsOperatorMock).setSyncStatus(CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE, TOPOLOGY_SYNC_STATUS_ATTR_VALUE,
                false);

        assertThat(eventHeaders, hasValue((Object) INVOKING_FDN_HEADER_VALUE));
        assertThat(eventHeaders, hasValue((Object) SYNC_TYPE_HEADER_VALUE));
        assertThat(eventHeaders, hasValue((Object) OSS_PREFIX_HEADER_VALUE));
        assertThat(eventHeaders, hasValue((Object) OSS_MODEL_IDENTITY_VALUE));
        assertThat(eventHeaders, hasKey(MiscellaneousConstants.SYNC_START_TIME_HEADER_NAME));
        assertThat(eventHeaders, hasEntry(MiscellaneousConstants.LARGE_NODE_FLAG, (Object) false));
    }

    @Test
    public void onEvent_smallMoInstanceCount() throws Exception {
        when(cmFunctionMoDpsOperatorMock.getSyncStatus(CM_FUNCTION_FDN)).thenReturn(PENDING_SYNC_STATUS_ATTR_VALUE);
        populateHeaders(SMALL_NODE_MO_INSTANCE_COUNT);
        mockOssPrefixAndOssModelIdentity();
        nodeInfoDpsHandler.init(eventHandlerContextMock);
        final Map<String, Object> eventHeaders = nodeInfoDpsHandler.onEvent(componentEventMock).getHeaders();
        verify(cmFunctionMoDpsOperatorMock).setSyncStatus(CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE, TOPOLOGY_SYNC_STATUS_ATTR_VALUE,
                false);
        assertThat(eventHeaders, hasEntry(MiscellaneousConstants.LARGE_NODE_FLAG, (Object) false));
    }

    @Test
    public void onEvent_largeMoInstanceCount() throws Exception {
        when(cmFunctionMoDpsOperatorMock.getSyncStatus(CM_FUNCTION_FDN)).thenReturn(PENDING_SYNC_STATUS_ATTR_VALUE);
        populateHeaders(LARGE_NODE_MO_INSTANCE_COUNT);
        mockOssPrefixAndOssModelIdentity();
        nodeInfoDpsHandler.init(eventHandlerContextMock);
        final Map<String, Object> eventHeaders = nodeInfoDpsHandler.onEvent(componentEventMock).getHeaders();
        verify(cmFunctionMoDpsOperatorMock)
                .setSyncStatus(CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE, TOPOLOGY_SYNC_STATUS_ATTR_VALUE, true);
        assertThat(eventHeaders, hasEntry(MiscellaneousConstants.LARGE_NODE_FLAG, (Object) true));
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void onEvent_OssPrefixNull_ThrowsException() throws Exception {
        final InboundDpsHandlerException inboundDpsHandlerException = new InboundDpsHandlerException(EXCEPTION_MESSAGE);
        when(networkElementMoOperatorMock.getOssPrefix(NETWORK_ELEMENT_FDN)).thenThrow(inboundDpsHandlerException);
        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(
                CM_FUNCTION_FDN, null, inboundDpsHandlerException, nodeInfoDpsHandler.handlerName);

        nodeInfoDpsHandler.onEvent(componentEventMock);

        verify(cmFunctionMoDpsOperatorMock, never()).setSyncStatus(CM_FUNCTION_FDN, TOPOLOGY_SYNC_STATUS_ATTR_VALUE);
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(CM_FUNCTION_FDN, null, inboundDpsHandlerException,
                nodeInfoDpsHandler.handlerName);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void onEvent_OssModelIdentityNull_ThrowsException() throws Exception {
        final OssModelIdentityException ossModelIdentityException = new OssModelIdentityException(EXCEPTION_MESSAGE);
        when(networkElementMoOperatorMock.getOssPrefix(NETWORK_ELEMENT_FDN)).thenReturn(OSS_PREFIX_HEADER_VALUE);
        when(networkElementMoOperatorMock.getOssModelIdentity(NETWORK_ELEMENT_FDN)).thenThrow(ossModelIdentityException);
        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(
                CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE, ossModelIdentityException, nodeInfoDpsHandler.handlerName,
                FailedSyncResultDpsOperator.SUPPRESS_STACK);

        nodeInfoDpsHandler.onEvent(componentEventMock);

        verify(cmFunctionMoDpsOperatorMock, never()).setSyncStatus(CM_FUNCTION_FDN, TOPOLOGY_SYNC_STATUS_ATTR_VALUE);
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE, ossModelIdentityException,
                nodeInfoDpsHandler.handlerName, FailedSyncResultDpsOperator.SUPPRESS_STACK);
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void onEvent_SoftwareSyncFailure_ThrowsException() throws Exception {
        configurationMock.getAllProperties().put(MiscellaneousConstants.SOFTWARE_SYNC_SUCCESS, false);
        configurationMock.getAllProperties().put(MiscellaneousConstants.SOFTWARE_SYNC_MESSAGE, EXCEPTION_MESSAGE);
        when(networkElementMoOperatorMock.getOssPrefix(NETWORK_ELEMENT_FDN)).thenReturn(OSS_PREFIX_HEADER_VALUE);
        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(
                eq(CM_FUNCTION_FDN), eq(OSS_PREFIX_HEADER_VALUE), any(SoftwareSyncFailureException.class), eq(nodeInfoDpsHandler.handlerName),
                eq(FailedSyncResultDpsOperator.SUPPRESS_STACK));

        nodeInfoDpsHandler.onEvent(componentEventMock);

        final ArgumentCaptor<SoftwareSyncFailureException> exCaptor = ArgumentCaptor.forClass(SoftwareSyncFailureException.class);
        verify(cmFunctionMoDpsOperatorMock, never()).setSyncStatus(CM_FUNCTION_FDN, TOPOLOGY_SYNC_STATUS_ATTR_VALUE);
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(eq(CM_FUNCTION_FDN), eq(OSS_PREFIX_HEADER_VALUE), exCaptor.capture(),
                eq(nodeInfoDpsHandler.handlerName), eq(FailedSyncResultDpsOperator.SUPPRESS_STACK));
        assertEquals(EXCEPTION_MESSAGE, exCaptor.getValue().getMessage());
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_CatchesSyncStatusException() throws Exception {
        doThrow(new SupervisionNotActiveException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock)
            .checkHeartbeatSupervision(CM_FUNCTION_FDN);
        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(),
                anyString(), any(Exception.class), anyString());

        mockOssPrefixAndOssModelIdentity();
        nodeInfoDpsHandler.onEvent(componentEventMock);
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(), anyString(), any(Exception.class), anyString());
        verify(cmFunctionMoDpsOperatorMock, never()).setSyncStatus(CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE,
                TOPOLOGY_SYNC_STATUS_ATTR_VALUE,
                false);
    }

    private void populateHeaders(final Integer moInstanceCount) {
        final Map<String, Object> eventHeaders = new HashMap<>();
        eventHeaders.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, INVOKING_FDN_HEADER_VALUE);
        eventHeaders.put(MiscellaneousConstants.MO_INSTANCE_COUNT, moInstanceCount);
        when(configurationMock.getAllProperties()).thenReturn(eventHeaders);
    }

    private void mockOssPrefixAndOssModelIdentity() {
        when(networkElementMoOperatorMock.getOssPrefix(NETWORK_ELEMENT_FDN)).thenReturn(OSS_PREFIX_HEADER_VALUE);
        when(networkElementMoOperatorMock.getOssModelIdentity(NETWORK_ELEMENT_FDN)).thenReturn(OSS_MODEL_IDENTITY_VALUE);
    }
}
