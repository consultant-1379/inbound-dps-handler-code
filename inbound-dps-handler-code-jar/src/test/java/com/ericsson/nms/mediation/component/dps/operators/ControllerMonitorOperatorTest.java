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

package com.ericsson.nms.mediation.component.dps.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ME_CONTEXT_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.NETWORK_ELEMENT_MO_TYPE;
import static com.ericsson.nms.mediation.component.dps.operators.ControllerMonitorOperator.SYNC_DEFERRED_EVENT_TYPE;
import static com.ericsson.nms.mediation.component.dps.operators.ControllerMonitorOperator.SYNC_IN_PROGRESS_EVENT_TYPE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_RESOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_SOURCE;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants;
import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.event.MediationEventSender;
import com.ericsson.nms.mediation.component.dps.handlers.context.SyncInvocationContext;
import com.ericsson.nms.mediation.component.dps.operators.dps.SyncTypeEvaluationDpsOperator;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.cm.events.SyncNotificationStarterEvent;

@RunWith(MockitoJUnitRunner.class)
public class ControllerMonitorOperatorTest {
    private static final String NODE_NAME = "VooDoo";
    private static final String NETWORK_ELEMENT_FDN = NETWORK_ELEMENT_MO_TYPE + "=" + NODE_NAME;
    private static final String CM_FUNCTION_FDN = FdnUtil.appendCmFunction(NETWORK_ELEMENT_FDN);
    private static final String OSS_PREFIX = ME_CONTEXT_MO_TYPE + "=" + NODE_NAME;
    private static final String HANDLER_NAME = "HANDLER";
    private static final String NODE_IP_ADDRESS = "192.160.2.2";

    @Mock
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperator;
    @Mock
    private MediationEventSender mediationEventSender;
    @Mock
    private SystemRecorder systemRecorder;
    @Mock
    private CppCiMoDpsOperator cppCiMoDpsOperator;
    @Mock
    private SyncTypeEvaluationDpsOperator syncTypeEvaluationDpsOperator;
    @Mock
    private SyncInvocationContext syncInvocationContext;
    @InjectMocks
    private ControllerMonitorOperator controllerMonitorOperator;

    @Captor
    private ArgumentCaptor<SyncNotificationStarterEvent> sentEventsCaptor;

    @Before
    public void setUp() {
        when(cmFunctionMoDpsOperator.checkAndSetSyncInProgress(syncInvocationContext.getCmFunctionFdn())).thenReturn(false);
        // mocks sync invocation context object
        when(syncInvocationContext.getOssPrefix()).thenReturn(OSS_PREFIX);
        when(syncInvocationContext.getNetworkElementFdn()).thenReturn(NETWORK_ELEMENT_FDN);
        when(syncInvocationContext.getCmFunctionFdn()).thenReturn(CM_FUNCTION_FDN);
    }

    @Test
    public void syncReadyToStart() {
        when(cmFunctionMoDpsOperator.checkAndSetSyncInProgress(syncInvocationContext.getCmFunctionFdn())).thenReturn(true);
        when(cmFunctionMoDpsOperator.isNodeSynced(syncInvocationContext.getCmFunctionFdn())).thenReturn(true);
        // executes the test
        final boolean canSyncProceed = controllerMonitorOperator.verifySyncStatus(syncInvocationContext, HANDLER_NAME, false);
        // the sync is valid to proceed, any deferred syncs should be cancelled
        assertTrue(canSyncProceed);
        verify(cmFunctionMoDpsOperator).setLostSynchronizationToCurrentDate(CM_FUNCTION_FDN);
        // there should be no start sync events triggered as no defers occurred previously
        controllerMonitorOperator.processSyncCompleted(NODE_NAME, MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
        verifyZeroInteractions(mediationEventSender);
    }

    @Test
    public void syncAlreadyInProgressIgnoreDeferring() {
        // executes the test
        final boolean canSyncProceed = controllerMonitorOperator.verifySyncStatus(syncInvocationContext, HANDLER_NAME, true);
        // the 'IN PROGRESS' system log should be recorded, deferring is set to ignored
        assertFalse(canSyncProceed);
        final String expectedEventType = ControllerMonitorOperator.buildEventType(HANDLER_NAME, SYNC_IN_PROGRESS_EVENT_TYPE);
        verify(systemRecorder).recordEvent(eq(expectedEventType), eq(EventLevel.DETAILED), eq(EVENT_SOURCE), eq(EVENT_RESOURCE), anyString());
        // there should be no sync events triggered as the deferring was ignored
        controllerMonitorOperator.processSyncCompleted(NODE_NAME, MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
        verifyZeroInteractions(mediationEventSender);
    }

    @Test
    public void syncAlreadyInProgressNoDefer() {
        final Date restartDate = new Date();
        mockRestartDates(restartDate, restartDate);
        // executes the test
        final boolean canSyncProceed = controllerMonitorOperator.verifySyncStatus(syncInvocationContext, HANDLER_NAME, false);
        // the 'IN PROGRESS' system log should be recorded, no sync defer recorded as the dps and node restart dates match
        assertFalse(canSyncProceed);
        final String expectedEventType = ControllerMonitorOperator.buildEventType(HANDLER_NAME, SYNC_IN_PROGRESS_EVENT_TYPE);
        verify(systemRecorder).recordEvent(eq(expectedEventType), eq(EventLevel.DETAILED), eq(EVENT_SOURCE), eq(EVENT_RESOURCE), anyString());
        // there should be no sync events triggered as the no defer occurred previously
        controllerMonitorOperator.processSyncCompleted(NODE_NAME, MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
        verifyZeroInteractions(mediationEventSender);
    }

    @Test
    public void ossModelIdentityUpdatedDeferSync() {
        when(syncInvocationContext.isOssModelIdentityUpdated()).thenReturn(true);
        // executes the test
        final boolean canSyncProceed = controllerMonitorOperator.verifySyncStatus(syncInvocationContext, HANDLER_NAME, false);
        // the sync should be deferred, as the oss model identity is updated
        assertFalse(canSyncProceed);
        verify(cppCiMoDpsOperator, never()).getRestartTimestamp(anyString());
        final String expectedEventType = ControllerMonitorOperator.buildEventType(HANDLER_NAME, SYNC_DEFERRED_EVENT_TYPE);
        verify(systemRecorder).recordEvent(eq(expectedEventType), eq(EventLevel.DETAILED),
                eq(EVENT_SOURCE), eq(EVENT_RESOURCE), anyString());
        // there should be one sync event triggered with ossModelIdentityUpdated set to TRUE as a deferred sync occurred
        controllerMonitorOperator.processSyncCompleted(NODE_NAME, MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
        verify(mediationEventSender).send(sentEventsCaptor.capture());
        assertTrue(sentEventsCaptor.getValue().isOssModelIdentityUpdated());
        assertEquals(sentEventsCaptor.getValue().getNodeAddress(), CM_FUNCTION_FDN);
    }

    @Test
    public void nodeHasRestartedDeferSync() {
        final Date nodeRestartDate = new Date();
        final Date dpsRestartDate = new Date(nodeRestartDate.getTime() - 1000);
        mockRestartDates(dpsRestartDate, nodeRestartDate);
        // executes the test
        final boolean canSyncProceed = controllerMonitorOperator.verifySyncStatus(syncInvocationContext, HANDLER_NAME, false);
        // the sync should be deferred, as dps and node restart dates don't match
        assertFalse(canSyncProceed);
        verify(cppCiMoDpsOperator).getRestartTimestamp(FdnUtil.getCppCiFdn(OSS_PREFIX));
        verify(syncTypeEvaluationDpsOperator).getNodeRestartDate(OSS_PREFIX, NODE_IP_ADDRESS);
        final String expectedEventType = ControllerMonitorOperator.buildEventType(HANDLER_NAME, SYNC_DEFERRED_EVENT_TYPE);
        verify(systemRecorder).recordEvent(eq(expectedEventType), eq(EventLevel.DETAILED),
                eq(EVENT_SOURCE), eq(EVENT_RESOURCE), anyString());
        // there should be one sync event triggered with ossModelIdentityUpdated set to FALSE as a deferred sync occurred
        controllerMonitorOperator.processSyncCompleted(NODE_NAME, MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
        verify(mediationEventSender).send(sentEventsCaptor.capture());
        assertFalse(sentEventsCaptor.getValue().isOssModelIdentityUpdated());
        assertEquals(sentEventsCaptor.getValue().getNodeAddress(), CM_FUNCTION_FDN);
    }

    private void mockRestartDates(final Date dpsRestartDate, final Date nodeRestartDate) {
        final String cppCiFdn = FdnUtil.getCppCiFdn(OSS_PREFIX);
        when(cppCiMoDpsOperator.getIpAddress(cppCiFdn)).thenReturn(NODE_IP_ADDRESS);
        when(cppCiMoDpsOperator.getRestartTimestamp(cppCiFdn)).thenReturn(dpsRestartDate);
        when(syncTypeEvaluationDpsOperator.getNodeRestartDate(OSS_PREFIX, NODE_IP_ADDRESS)).thenReturn(nodeRestartDate);
    }
}
