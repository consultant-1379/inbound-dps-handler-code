/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.handlers.events;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.PENDING_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.api.SyncStatusChangeMessage;
import com.ericsson.nms.mediation.component.dps.operators.ControllerMonitorOperator;

@RunWith(MockitoJUnitRunner.class)
public class SyncStatusChangeEventReceiverTest {

    private static final String NODE_NAME = "VooDoo";
    private static final String NODE_MECONTEXT = "MeContext=" + NODE_NAME;

    @Mock
    private ControllerMonitorOperator controllerMonitorOperator;
    @InjectMocks
    private SyncStatusChangeEventReceiver syncStatusChangeEventReceiver;

    @Test
    public void testNull() throws InterruptedException {
        syncStatusChangeEventReceiver.receiveMessage(null);
        verifyZeroInteractions(controllerMonitorOperator);
    }

    @Test
    public void testReceiveMessageSynchronized() throws InterruptedException {
        final SyncStatusChangeMessage testMessage = new SyncStatusChangeMessage(NODE_NAME, NODE_MECONTEXT,
                SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE, false);
        syncStatusChangeEventReceiver.receiveMessage(testMessage);
        verify(controllerMonitorOperator).processSyncCompleted(NODE_NAME, SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
    }

    @Test
    public void testReceiveMessageUnsynchronized() throws InterruptedException {
        final SyncStatusChangeMessage testMessage = new SyncStatusChangeMessage(NODE_NAME, NODE_MECONTEXT,
                UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE, false);
        syncStatusChangeEventReceiver.receiveMessage(testMessage);
        verify(controllerMonitorOperator).processSyncCompleted(NODE_NAME, UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
    }

    @Test
    public void testReceiveMessagePending() throws InterruptedException {
        final SyncStatusChangeMessage testMessage = new SyncStatusChangeMessage(NODE_NAME, NODE_MECONTEXT,
                PENDING_SYNC_STATUS_ATTR_VALUE, false);
        syncStatusChangeEventReceiver.receiveMessage(testMessage);
        verifyZeroInteractions(controllerMonitorOperator);
    }
}
