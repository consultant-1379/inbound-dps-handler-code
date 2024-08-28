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

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ACTIVE_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.CM_NODE_HEARTBEAT_SUPERVISION_MO_TYPE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SWSYNCINITIATOR_CM_FUNCTION_SYNC_VALUE;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.enm.mediation.handler.common.dps.api.MoDpsOperatorException;
import com.ericsson.enm.mediation.handler.common.dps.operators.CmSupervisionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.NetworkElementMoDpsOperator;
import com.ericsson.nms.mediation.component.dps.event.MediationEventSender;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.handlers.context.SyncInvocationContext;
import com.ericsson.nms.mediation.component.dps.operators.ControllerMonitorOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.events.SoftwareSyncStarterEvent200;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;

/**
 * Handler for the CmFunction sync action use case, executed within the <em>CppCmFunctionSyncFlow</em>.
 * <p>
 * Responsible for checking current sync status and initiating a Software Sync flow (<em>CppSoftwareSyncFlow</em>).
 * <p>
 * The handler checks the <code>syncStatus</code> of the node and based on its value it determines if there is a sync ongoing.
 * <p>
 * If there is a sync in progress, this information is simply logged and the execution of this handler is finished. No other flow
 * will be called at this time.
 * <p>
 * When there is no sync ongoing, the controller changes the <code>syncStatus</code> to indicate that a sync is pending and sends
 * a relevant event to initiate the <code>CppSoftwareSyncFlow</code>. The <code>initiator</code> attribute of the event is set to
 * <code>CM_FUNCTION_SYNC</code> to make sure that a Full Sync is executed after Software Sync completion.
 */
@EventHandler(contextName = "")
public class CppCmFunctionSyncHandler extends DpsHandler {
    private static final String SUPERVISION_NOT_ACTIVE_ERROR_MESSAGE = "Synchronization request for this node has failed. Please ensure that the "
            + ACTIVE_ATTR_NAME + " attribute in the " + CM_NODE_HEARTBEAT_SUPERVISION_MO_TYPE + " MO has been set to true.";

    private final ThreadLocal<Map<String, Object>> contextHeaders = new ThreadLocal<>();

    @Inject
    private MediationEventSender mediationEventSender;
    @Inject
    private NetworkElementMoDpsOperator networkElementMoDpsOperator;
    @Inject
    private CmSupervisionMoDpsOperator cmSupervisionMoDpsOperator;
    @Inject
    private ControllerMonitorOperator controllerMonitorOperator;
    @Inject
    private FailedSyncResultDpsOperator failedSyncResultDpsOperator;

    @Override
    protected void initHandler(final EventHandlerContext eventHandlerContext) {
        contextHeaders.set(eventHandlerContext.getEventHandlerConfiguration().getAllProperties());
    }

    @Override
    public ComponentEvent onEvent(final ComponentEvent event) {
        final long startTime = System.currentTimeMillis();

        final SyncInvocationContext ctx = new SyncInvocationContext(contextHeaders.get());
        contextHeaders.remove();

        recordInvocationDetails(ctx.getNetworkElementFdn(), event);
        instrumentation.increaseDpsControllerInvocations();

        try {
            readOssPrefix(ctx);
            validateSupervisionOn(ctx);
            final boolean canSyncProceed = controllerMonitorOperator.verifySyncStatus(ctx, handlerName, true);
            if (canSyncProceed) {
                initiateSoftwareSync(ctx);
            }
        } catch (final Exception exception) {
            failedSyncResultDpsOperator.handleErrorAndRethrowExceptionNoResync(ctx.getCmFunctionFdn(), ctx.getOssPrefix(),
                    exception, handlerName);
        } finally {
            logger.debug("Finished {} ('{}').", handlerName, ctx.getNetworkElementFdn());
        }

        recordTimeTaken(ctx.getNetworkElementFdn(), startTime);

        // Cannot return original event headers as they may contain classes unresolvable by other containers
        return new MediationComponentEvent(new HashMap<String, Object>(), event.getPayload());
    }

    /**
     * Reads the ossPrefix from the NetworkElement and stores it in the current sync context.
     *
     * @param ctx
     *            sync invocation context
     * @throws MoDpsOperatorException
     *             if the ossPrefix is {@code null}
     */
    private void readOssPrefix(final SyncInvocationContext ctx) throws MoDpsOperatorException {
        final String ossPrefix = networkElementMoDpsOperator.getOssPrefix(ctx.getNetworkElementFdn());
        ctx.setOssPrefix(ossPrefix);
    }

    /**
     * Ensures that CM Supervision is active.
     *
     * @param ctx
     *            sync invocation context
     * @throws InboundDpsHandlerException
     *             if supervision is not active
     */
    private void validateSupervisionOn(final SyncInvocationContext ctx) throws InboundDpsHandlerException {
        if (!cmSupervisionMoDpsOperator.getActive(ctx.getCmSupervisionFdn())) {
            throw new InboundDpsHandlerException(SUPERVISION_NOT_ACTIVE_ERROR_MESSAGE);
        }
    }

    /**
     * Initiates a Software Sync for the node.
     *
     * @param ctx
     *            sync invocation context
     */
    private void initiateSoftwareSync(final SyncInvocationContext ctx) {
        final MediationTaskRequest swSyncEvent =
                new SoftwareSyncStarterEvent200(ctx.getNetworkElementFdn(), SWSYNCINITIATOR_CM_FUNCTION_SYNC_VALUE);
        mediationEventSender.send(swSyncEvent);
    }

}
