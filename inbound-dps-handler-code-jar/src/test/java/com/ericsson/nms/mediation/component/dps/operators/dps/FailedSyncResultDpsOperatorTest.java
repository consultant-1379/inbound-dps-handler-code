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

package com.ericsson.nms.mediation.component.dps.operators.dps;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.RESET_GENERATION_COUNTER_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.TOPOLOGY_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator.MAX_SYNC_RETRIES;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.CmSupervisionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.event.MediationEventSender;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.exception.SupervisionNotActiveException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DpsHandlerInstrumentation;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.cm.events.SubscriptionValidationEvent;
import com.ericsson.oss.mediation.cm.events.SyncNotificationStarterEvent;

@RunWith(MockitoJUnitRunner.class)
public class FailedSyncResultDpsOperatorTest {
    private static final String NODE_NAME = "VooDoo";
    private static final String OSS_PREFIX_HEADER_VALUE = "MeContext=VooDoo";
    private static final String NETWORK_ELEMETN_FDN = FdnUtil.getNetworkElementFdn(NODE_NAME);
    private static final String CM_FUNCTION_FDN = FdnUtil.appendCmFunction(NETWORK_ELEMETN_FDN);
    private static final String CPP_CI_FDN = FdnUtil.appendCppCi(NETWORK_ELEMETN_FDN);
    private static final String HANDLER_NAME = "handlerName";

    private int failedSyncsCountValue;

    @Mock
    private SystemRecorder systemRecorderMock;
    @Mock
    private DpsHandlerInstrumentation instrumentationMock;
    @Mock
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperatorMock;
    @Mock
    private CmSupervisionMoDpsOperator cmSupervisionMoDpsOperatorMock;
    @Mock
    private CppCiMoDpsOperator cppCiMoDpsOperatorMock;
    @Mock
    private MediationEventSender mediationEventSenderMock;
    @Mock
    private SubscriptionValidationEvent subscriptionValidationEventMock;
    @Mock
    private EventSender<SubscriptionValidationEvent> eventSenderMock;

    @InjectMocks
    private FailedSyncResultDpsOperator failedSyncResultDpsOperator;

    @Test(expected = InboundDpsHandlerException.class)
    public void testHandleErrorAndRethrowException_syncRetry() {
        failedSyncsCountValue = 1;
        when(cmFunctionMoDpsOperatorMock.getFailedSyncsCount(CM_FUNCTION_FDN)).thenReturn(failedSyncsCountValue);
        failedSyncResultDpsOperator.handleErrorAndRethrowException(CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE, new Exception(), HANDLER_NAME);
        verify(cppCiMoDpsOperatorMock).setGenerationCounter(CPP_CI_FDN, RESET_GENERATION_COUNTER_ATTR_VALUE);
        verify(cmFunctionMoDpsOperatorMock).updateAttrsUponFailedSync(CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE);
        verify(eventSenderMock).send(new SubscriptionValidationEvent());
        verify(mediationEventSenderMock).send(new SyncNotificationStarterEvent(CM_FUNCTION_FDN));
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testHandleErrorAndRethrowException_noSyncRetry() {
        failedSyncsCountValue = MAX_SYNC_RETRIES + 1;
        when(cmFunctionMoDpsOperatorMock.getFailedSyncsCount(CM_FUNCTION_FDN)).thenReturn(failedSyncsCountValue);

        failedSyncResultDpsOperator.handleErrorAndRethrowException(CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE, new Exception(), HANDLER_NAME);
        verify(cppCiMoDpsOperatorMock).setGenerationCounter(CPP_CI_FDN, RESET_GENERATION_COUNTER_ATTR_VALUE);
        verify(cmFunctionMoDpsOperatorMock).updateAttrsUponFailedSync(CM_FUNCTION_FDN, OSS_PREFIX_HEADER_VALUE);
        verify(mediationEventSenderMock, never()).send(new SyncNotificationStarterEvent(CM_FUNCTION_FDN));
        verify(eventSenderMock, never()).send(new SubscriptionValidationEvent());
    }

    @Test(expected = SupervisionNotActiveException.class)
    public void testCheckHeartbeatSupervision() {
        final String cmNodeHeartbeatSupervisionFdn = FdnUtil.convertToCmNodeHeartbeatSupervision(CM_FUNCTION_FDN);
        when(cmSupervisionMoDpsOperatorMock.getActive(cmNodeHeartbeatSupervisionFdn)).thenReturn(false);

        failedSyncResultDpsOperator.checkHeartbeatSupervision(CM_FUNCTION_FDN);
    }

    @Test(expected = SupervisionNotActiveException.class)
    public void testCheckSyncStatus() {
        when(cmFunctionMoDpsOperatorMock.getSyncStatus(CM_FUNCTION_FDN)).thenReturn(TOPOLOGY_SYNC_STATUS_ATTR_VALUE);

        failedSyncResultDpsOperator.checkHeartbeatSupervision(CM_FUNCTION_FDN);
    }

}
