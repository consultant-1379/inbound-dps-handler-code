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

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.api.SyncStatusChangeMessage;
import com.ericsson.nms.mediation.component.dps.operators.ControllerMonitorOperator;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Consumes;

/**
 * Observer class for listening to {@link SyncStatusChangeMessage} from SyncStatusChangeTopic.
 */
@ApplicationScoped
public class SyncStatusChangeEventReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncStatusChangeEventReceiver.class);

    @Inject
    private ControllerMonitorOperator controllerMonitorOperator;

    /**
     * Observer for {@link SyncStatusChangeMessage} which asynchronously delegates processing of this message to
     * {@link ControllerMonitorOperator}.
     *
     * @param syncStatusChangeEvent
     *            The event containing sync status change
     */
    public void receiveMessage(@Observes @Consumes(endpoint = "jms:/topic/SyncStatusChangeTopic")
            final SyncStatusChangeMessage syncStatusChangeEvent) {
        LOGGER.debug("received sync status change event: {}", syncStatusChangeEvent);
        if (syncStatusChangeEvent == null) {
            LOGGER.warn("Received null SyncStatusChangeMessage");
            return;
        }
        // check syncStatus to avoid the asynchronous call for intermediate transitions
        final String syncStatus = syncStatusChangeEvent.getSyncStatus();
        if (SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE.equals(syncStatus) || UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE.equals(syncStatus)) {
            controllerMonitorOperator.processSyncCompleted(syncStatusChangeEvent.getNodeId(), syncStatus);
        }
    }
}
