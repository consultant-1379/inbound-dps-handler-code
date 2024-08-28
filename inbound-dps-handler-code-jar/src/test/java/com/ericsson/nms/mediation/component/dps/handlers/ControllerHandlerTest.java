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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ME_CONTEXT_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.NETWORK_ELEMENT_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.PENDING_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.TOPOLOGY_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.api.MimSwitchException;
import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.CmSupervisionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.NetworkElementMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.enm.mediation.handler.common.dps.util.MimSwitchHelper;
import com.ericsson.nms.mediation.component.dps.common.SyncType;
import com.ericsson.nms.mediation.component.dps.event.MediationEventSender;
import com.ericsson.nms.mediation.component.dps.handlers.context.SyncInvocationContext;
import com.ericsson.nms.mediation.component.dps.instrumentation.DpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.instrumentation.MimSwitchInstrumentation;
import com.ericsson.nms.mediation.component.dps.operators.ControllerMonitorOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.SyncTypeEvaluationDpsOperator;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.cm.events.StartDeltaSyncEvent;
import com.ericsson.oss.mediation.cm.events.StartSubscriptionEvent;
import com.ericsson.oss.mediation.cm.events.StartSyncEvent;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;

@RunWith(MockitoJUnitRunner.class)
public class ControllerHandlerTest {

    private static final String NODE_NAME = "VooDoo";
    private static final String ME_CONTEXT_FDN = ME_CONTEXT_MO_TYPE + "=" + NODE_NAME;
    private static final String NETWORK_ELEMENT_FDN = NETWORK_ELEMENT_MO_TYPE + "=" + NODE_NAME;
    private static final String CM_FUNCTION_FDN = FdnUtil.appendCmFunction(NETWORK_ELEMENT_FDN);
    private static final String CM_SUPERVISION_FDN = FdnUtil.appendCmSupervision(NETWORK_ELEMENT_FDN);
    private static final String SW_SYNC_FAILURE_MSG = "Error";

    private static final Logger SYSREC = LoggerFactory.getLogger("SystemRecorder");
    private static final Map<String, Object> CONTEXT_HEADERS = new HashMap<>(5);
    private static final ComponentEvent INPUT_EVENT = new MediationComponentEvent(new HashMap<>(), null);

    @Mock
    private SystemRecorder systemRecorder;
    @Mock
    private DpsHandlerInstrumentation instrumentation;
    @Mock
    private MimSwitchInstrumentation mimSwitchInstrumentation;
    @Mock
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperator;
    @Mock
    private MediationEventSender mediationEventSender;
    @Mock
    private SyncTypeEvaluationDpsOperator syncTypeEvaluationDpsOperator;
    @Mock
    private NetworkElementMoDpsOperator networkElementMoDpsOperator;
    @Mock
    private CmSupervisionMoDpsOperator cmSupervisionMoDpsOperator;
    @Mock
    private ControllerMonitorOperator controllerMonitorOperator;
    @Mock
    private FailedSyncResultDpsOperator failedSyncResultDpsOperator;
    @Mock
    private MimSwitchHelper mimSwitchHelper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EventHandlerContext eventHandlerContext;
    @Captor
    private ArgumentCaptor<MediationTaskRequest> mtrCaptor;
    @InjectMocks
    private ControllerHandler controllerHandler;

    @Before
    public void setUp() throws Exception {
        CONTEXT_HEADERS.clear();
        CONTEXT_HEADERS.put(INVOKING_FDN_HEADER_NAME, CM_FUNCTION_FDN);

        when(eventHandlerContext.getEventHandlerConfiguration().getAllProperties()).thenReturn(CONTEXT_HEADERS);
        controllerHandler.init(eventHandlerContext);
        when(networkElementMoDpsOperator.getOssPrefix(NETWORK_ELEMENT_FDN)).thenReturn(ME_CONTEXT_FDN);
        when(cmSupervisionMoDpsOperator.getActive(CM_SUPERVISION_FDN)).thenReturn(true);
        when(syncTypeEvaluationDpsOperator.evaluateSyncType(any(SyncInvocationContext.class))).thenReturn(SyncType.FULL);
        when(controllerMonitorOperator.verifySyncStatus(anyObject(),
                eq(controllerHandler.handlerName), eq(false))).thenReturn(true);

        // mock systemRecorder
        doAnswer(invocation -> {
            SYSREC.info("{}", Arrays.toString(invocation.getArguments()));
            return null;
        }).when(systemRecorder).recordEvent(anyString(), any(EventLevel.class), anyString(), anyString(), anyString());
    }

    @Test
    public void invalidSupervisionState() {
        when(cmSupervisionMoDpsOperator.getActive(CM_SUPERVISION_FDN)).thenReturn(false);
        // executes the test, InboundDpsHandlerException should be thrown as supervision is inactive
        controllerHandler.onEvent(INPUT_EVENT);
        // the controller should handle the occurred exception then interrupt the flow execution
        verify(failedSyncResultDpsOperator).handleErrorAndRethrowException(
                eq(CM_FUNCTION_FDN), eq(ME_CONTEXT_FDN), anyObject(), eq(controllerHandler.handlerName));
        verifyZeroInteractions(controllerMonitorOperator);
        verifyZeroInteractions(mediationEventSender);
    }

    @Test
    public void syncCannotProceed() {
        when(controllerMonitorOperator.verifySyncStatus(anyObject(),
                eq(controllerHandler.handlerName), eq(false))).thenReturn(false);
        // executes the test
        controllerHandler.onEvent(INPUT_EVENT);
        // the controller should end the flow execution
        verifyZeroInteractions(mediationEventSender);
    }

    @Test
    public void syncCanProceed() {
        when(syncTypeEvaluationDpsOperator.evaluateSyncType(any(SyncInvocationContext.class))).thenReturn(SyncType.DELTA);
        // executes the test
        controllerHandler.onEvent(INPUT_EVENT);
        // the controller should evaluate to a DELTA sync type and send start sync event
        verify(syncTypeEvaluationDpsOperator).evaluateSyncType(anyObject());
        verify(mediationEventSender).send(mtrCaptor.capture());
        assertTrue(mtrCaptor.getValue() instanceof StartDeltaSyncEvent);
    }

    @Test
    public void manualSyncCanProceed() {
        CONTEXT_HEADERS.put(CM_FUNCTION_SYNC_ACTION_HEADER_NAME, true);
        when(cmFunctionMoDpsOperator.getSyncStatus(CM_FUNCTION_FDN)).thenReturn(PENDING_SYNC_STATUS_ATTR_VALUE);
        // executes the test
        controllerHandler.onEvent(INPUT_EVENT);
        // the controller should evaluate to a FULL sync type and send start sync event
        verify(syncTypeEvaluationDpsOperator).evaluateSyncType(anyObject());
        verify(mediationEventSender).send(mtrCaptor.capture());
        assertTrue(mtrCaptor.getValue() instanceof StartSyncEvent);
    }

    @Test
    public void manualSyncCannotProceed() {
        CONTEXT_HEADERS.put(CM_FUNCTION_SYNC_ACTION_HEADER_NAME, true);
        when(cmFunctionMoDpsOperator.getSyncStatus(CM_FUNCTION_FDN)).thenReturn(TOPOLOGY_SYNC_STATUS_ATTR_VALUE);
        when(controllerMonitorOperator.verifySyncStatus(anyObject(),
                eq(controllerHandler.handlerName), eq(false))).thenReturn(false);
        // executes the test
        controllerHandler.onEvent(INPUT_EVENT);
        // the controller should end the flow execution
        verifyZeroInteractions(mediationEventSender);
    }

    @Test
    public void softwareSyncCheckFailed() {
        CONTEXT_HEADERS.put(SOFTWARE_SYNC_FAILURE_HEADER_NAME, true);
        CONTEXT_HEADERS.put(SOFTWARE_SYNC_FAILURE_MSG_HEADER_NAME, SW_SYNC_FAILURE_MSG);
        // executes the test
        controllerHandler.onEvent(INPUT_EVENT);
        // the controller should handle the occurred exception then interrupt the flow execution
        verify(failedSyncResultDpsOperator).handleErrorAndRethrowException(eq(CM_FUNCTION_FDN), eq(ME_CONTEXT_FDN),
                anyObject(), eq(controllerHandler.handlerName), eq(FailedSyncResultDpsOperator.SUPPRESS_STACK));
        verifyZeroInteractions(mediationEventSender);
    }

    @Test
    public void performMimSwitchRequired() throws MimSwitchException {
        when(mimSwitchHelper.performMimSwitchIfNeeded(NETWORK_ELEMENT_FDN)).thenReturn(true);
        // executes the test
        controllerHandler.onEvent(INPUT_EVENT);
        // the controller should increase the number of performed MIB upgrades then initiate a sync
        verify(mimSwitchInstrumentation).increaseNumberOfMibUpgradePerformed(true);
        verify(mediationEventSender).send(anyObject());
    }

    @Test
    public void performMimSwitchCheckFailed() throws MimSwitchException {
        doThrow(MimSwitchException.class).when(mimSwitchHelper).performMimSwitchIfNeeded(NETWORK_ELEMENT_FDN);
        // executes the test
        controllerHandler.onEvent(INPUT_EVENT);
        // the controller should handle the occurred exception then interrupt the flow execution
        verify(failedSyncResultDpsOperator).handleErrorAndRethrowException(eq(CM_FUNCTION_FDN), eq(ME_CONTEXT_FDN),
                any(), eq(controllerHandler.handlerName), eq(FailedSyncResultDpsOperator.SUPPRESS_STACK));
        verify(mimSwitchInstrumentation).increaseNumberOfMibUpgradePerformed(false);
        verifyZeroInteractions(mediationEventSender);
    }

    @Test
    public void omiChangeRequiresNewSubscription() {
        CONTEXT_HEADERS.put(OSS_MODEL_IDENTITY_UPDATED_HEADER_NAME, true);
        // executes the test
        controllerHandler.onEvent(INPUT_EVENT);
        // the controller should send two MTRs, StartSubscriptionEvent and StartSyncEvent
        verify(mediationEventSender, times(2)).send(mtrCaptor.capture());
        assertTrue(mtrCaptor.getAllValues().get(0) instanceof StartSubscriptionEvent);
        assertTrue(mtrCaptor.getAllValues().get(1) instanceof StartSyncEvent);
    }

    @Test
    public void mediationEventSenderFailed() {
        doThrow(new RuntimeException()).when(mediationEventSender).send(anyObject());
        // executes the test
        controllerHandler.onEvent(INPUT_EVENT);
        // the controller should handle the occurred exception then interrupt the flow execution
        verify(failedSyncResultDpsOperator).handleErrorAndRethrowException(eq(CM_FUNCTION_FDN),
                eq(ME_CONTEXT_FDN), anyObject(), eq(controllerHandler.handlerName));
    }
}
