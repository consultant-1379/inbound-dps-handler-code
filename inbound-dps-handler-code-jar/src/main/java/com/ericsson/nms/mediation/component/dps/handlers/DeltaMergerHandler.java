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

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.*;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.DELTA_SYNC_CHANGES;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

import javax.inject.Inject;

import com.ericsson.nms.mediation.component.dps.handlers.payload.DeltaSyncDpsPayload;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.ericsson.oss.mediation.network.api.notifications.NotificationType;

/**
 * This handler merges the output as follows. of <code>SyncNodeMociAttributesHandler</code> with the collection of changes retrieved previously in the
 * <code>DeltaSyncMociTopologyHandler</code> and provide the payload for the <code>DeltaSyncDpsHandler</code> where they are written to DPS.
 * <p>
 * Note: The payload passed to the <code>DeltaSyncDpsHandler</code> is a <code>DeltaSyncDpsPayload</code> object. This contains a collection of create
 * and update changes in root to leaf order, a collection of delete changes in leaf to root order and the largest generation counter from the changes.
 */
@EventHandler(contextName = "")
public class DeltaMergerHandler extends DpsHandler {
    @Inject
    private FailedSyncResultDpsOperator failedSyncResultDpsOperator;

    @Override
    protected void initHandler(final EventHandlerContext eventHandlerContext) {
        // No initialization required
    }

    @Override
    public ComponentEvent onEvent(final ComponentEvent event) {
        final long startTime = System.currentTimeMillis();
        final String cmFunctionFdn = (String) event.getHeaders().get(INVOKING_FDN_HEADER_NAME);
        final String ossPrefix = (String) event.getHeaders().get(OSS_PREFIX_HEADER_NAME);
        recordInvocationDetails(cmFunctionFdn, event);

        DeltaSyncDpsPayload deltaSyncDpsPayload = null;
        try {
            rethrowExceptionIfOneFoundInPayload(event.getPayload());
            deltaSyncDpsPayload = processChanges(event);
        } catch (final Exception exception) {
            failedSyncResultDpsOperator.handleErrorAndRethrowException(cmFunctionFdn, ossPrefix, exception, handlerName);
        }

        recordTimeTaken(cmFunctionFdn, startTime);

        logger.debug("Finished {} ('{}').", handlerName, cmFunctionFdn);
        return new MediationComponentEvent(event.getHeaders(), deltaSyncDpsPayload);
    }

    /**
     * Processes the delta sync changes, splitting the creates and updates from the delete changes.<br>
     * Orders the creates and updates in ascending order. The deletes are sorted in descending order.<br>
     * Also ensures the largest generation counter from the changes is retrieved.
     *
     * @param event
     *            event
     * @return <code>DeltaSyncDpsPayload</code> object. This contains a collection of create and update changes in root to leaf order, a collection of
     *         delete changes in leaf to root order and the largest generation counter from the changes.
     */
    @SuppressWarnings("unchecked")
    private DeltaSyncDpsPayload processChanges(final ComponentEvent event) {
        final Collection<NodeNotification> createsAndUpdates = new TreeSet<NodeNotification>(new AscendingNodeNotificationComparator());
        final Collection<NodeNotification> deletes = new TreeSet<NodeNotification>(new DescendingNodeNotificationComparator());
        Long generationCounter = 0L;

        final Map<String, Map<String, Object>> payload = (Map<String, Map<String, Object>>) event.getPayload();
        final Collection<NodeNotification> deltaSyncChanges = (Collection<NodeNotification>) event.getHeaders().get(DELTA_SYNC_CHANGES);

        for (final NodeNotification change : deltaSyncChanges) {
            final NotificationType actionType = change.getAction();
            final Long changeGenCounter = change.getGenerationCounter();
            logger.trace("Change type: [{}], generation counter: [{}]", actionType, generationCounter);

            if (!actionType.equals(NotificationType.DELETE)) {
                change.setUpdateAttributes(payload.get(change.getFdn()));
                createsAndUpdates.add(change);
            } else {
                deletes.add(change);
            }

            if (changeGenCounter > generationCounter) {
                generationCounter = changeGenCounter;
            }
        }

        return new DeltaSyncDpsPayload(createsAndUpdates, deletes, generationCounter);
    }

    /**
     * Comparator for sorting the order of Delta Sync changes retrieved from the NE in descending order i.e. from leaf to root of tree.
     */
    private static class DescendingNodeNotificationComparator implements Comparator<NodeNotification> {
        @Override
        public int compare(final NodeNotification nodeNotification1, final NodeNotification nodeNotification2) {
            return -nodeNotification1.getFdn().compareTo(nodeNotification2.getFdn());
        }
    }

    /**
     * Comparator for sorting the order of Delta Sync changes retrieved from the NE in ascending order i.e. from root to leaf of tree.
     */
    private static class AscendingNodeNotificationComparator implements Comparator<NodeNotification> {
        @Override
        public int compare(final NodeNotification nodeNotification1, final NodeNotification nodeNotification2) {
            return nodeNotification1.getFdn().compareTo(nodeNotification2.getFdn());
        }
    }
}
