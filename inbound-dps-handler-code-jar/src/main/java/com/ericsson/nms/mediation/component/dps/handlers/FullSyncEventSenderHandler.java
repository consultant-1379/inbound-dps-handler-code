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

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.PENDING_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.INVOKING_FDN_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_PREFIX_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.PO_ID_HEADER_NAME;

import javax.inject.Inject;

import com.ericsson.nms.mediation.component.dps.event.MediationEventSender;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.cm.events.StartSyncEvent;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;

/**
 * Handler used for creating and sending a <code>StartSyncEvent</code>.
 * <p>
 * <code>StartSyncEvent</code> is responsible for initiating another flow - SyncStarterFlow - which performs a full sync.
 */
@EventHandler(contextName = "")
public class FullSyncEventSenderHandler extends DpsHandler {
    @Inject
    private MediationEventSender mediationEventSender;
    @Inject
    private FailedSyncResultDpsOperator failedSyncResultDpsOperator;

    @Override
    protected void initHandler(final EventHandlerContext eventHandlerContext) {
        // Nothing to initialize
    }

    @Override
    public ComponentEvent onEvent(final ComponentEvent event) {
        final long startTime = System.currentTimeMillis();

        final String cmFunctionFdn = (String) event.getHeaders().get(INVOKING_FDN_HEADER_NAME);
        final String ossPrefix = (String) event.getHeaders().get(OSS_PREFIX_HEADER_NAME);
        final Long poId = (Long) event.getHeaders().get(PO_ID_HEADER_NAME);

        recordInvocationDetails(cmFunctionFdn, event);

        try {
            failedSyncResultDpsOperator.checkHeartbeatSupervision(cmFunctionFdn);
            cmFunctionMoDpsOperator.setSyncStatus(cmFunctionFdn, ossPrefix, PENDING_SYNC_STATUS_ATTR_VALUE, null);
            final MediationTaskRequest syncEvent = new StartSyncEvent(poId);
            syncEvent.setNodeAddress(cmFunctionFdn);
            mediationEventSender.send(syncEvent);
        } catch (final Exception exception) {
            failedSyncResultDpsOperator.handleErrorAndRethrowException(cmFunctionFdn, ossPrefix, exception, handlerName);
        }

        recordTimeTaken(cmFunctionFdn, startTime);

        logger.debug("Finished {} ('{}').", handlerName, cmFunctionFdn);
        return event;
    }

}
