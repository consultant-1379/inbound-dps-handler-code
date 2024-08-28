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
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.PENDING_SYNC_STATUS_ATTR_VALUE;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.enm.mediation.handler.common.dps.api.MimSwitchException;
import com.ericsson.enm.mediation.handler.common.dps.api.MoDpsOperatorException;
import com.ericsson.enm.mediation.handler.common.dps.api.OssModelIdentityException;
import com.ericsson.enm.mediation.handler.common.dps.operators.CmSupervisionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.NetworkElementMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.MimSwitchHelper;
import com.ericsson.nms.mediation.component.dps.common.SyncType;
import com.ericsson.nms.mediation.component.dps.event.MediationEventSender;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.handlers.context.SyncInvocationContext;
import com.ericsson.nms.mediation.component.dps.handlers.exception.SoftwareSyncFailureException;
import com.ericsson.nms.mediation.component.dps.instrumentation.MimSwitchInstrumentation;
import com.ericsson.nms.mediation.component.dps.operators.ControllerMonitorOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.SyncTypeEvaluationDpsOperator;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.cm.events.StartDeltaSyncEvent;
import com.ericsson.oss.mediation.cm.events.StartSubscriptionEvent;
import com.ericsson.oss.mediation.cm.events.StartSyncEvent;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;

/**
 * Entry handler for the Sync Node use case. First and last handler within the <em>SyncStarterFlow</em>.
 * <p>
 * Responsible for initiating a second flow, <em>SyncNodeFlow</em>, upon calling which the actual sync of a node starts up.
 * <p>
 * Controller checks the <code>syncStatus</code> of the node and based on its value the controller determines if there is a sync ongoing.
 * <p>
 * If there is a sync in progress, this information is simply logged and the execution of this handler is finished (at the same time concluding the
 * entire flow as this is the only step within the <em>SyncStarterFlow</em> flow). <em>SyncNodeFlow</em> is not being called at all in this case.
 * <p>
 * When there is no sync ongoing, the controller changes the <code>syncStatus</code> to indicate that a sync is pending and sends a relevant event to
 * initiate the <code>SyncNodeFlow</code>.
 */
@EventHandler
public class ControllerHandler extends DpsHandler {
    private static final String SUPERVISION_NOT_ACTIVE_ERROR_MESSAGE = "Synchronization request for this node has failed. Please ensure that the "
            + ACTIVE_ATTR_NAME + " attribute in the " + CM_NODE_HEARTBEAT_SUPERVISION_MO_TYPE + " MO has been set to true.";

    private final ThreadLocal<Map<String, Object>> contextHeaders = new ThreadLocal<>();

    @Inject
    private MediationEventSender mediationEventSender;
    @Inject
    private SyncTypeEvaluationDpsOperator syncTypeEvaluationDpsOperator;
    @Inject
    private NetworkElementMoDpsOperator networkElementMoDpsOperator;
    @Inject
    private CmSupervisionMoDpsOperator cmSupervisionMoDpsOperator;
    @Inject
    private ControllerMonitorOperator controllerMonitorOperator;
    @Inject
    private FailedSyncResultDpsOperator failedSyncResultDpsOperator;
    @Inject
    private MimSwitchHelper mimSwitchHelper;
    @Inject
    private MimSwitchInstrumentation mimSwitchInstrumentation;

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
            if (canSyncProceed(ctx)) {
                checkSoftwareSyncFailure(ctx);
                performMimSwitchIfNeeded(ctx);
                updateSubscriptionIfOmiChanged(ctx);
                initiateSync(ctx);
            }
        } catch (final OssModelIdentityException | SoftwareSyncFailureException | MimSwitchException exception) {
            if (exception instanceof MimSwitchException) {
                mimSwitchInstrumentation.increaseNumberOfMibUpgradePerformed(false);
            }
            failedSyncResultDpsOperator.handleErrorAndRethrowException(ctx.getCmFunctionFdn(), ctx.getOssPrefix(), exception, handlerName,
                    FailedSyncResultDpsOperator.SUPPRESS_STACK);
        } catch (final Exception exception) {
            failedSyncResultDpsOperator.handleErrorAndRethrowException(ctx.getCmFunctionFdn(), ctx.getOssPrefix(), exception, handlerName);
        } finally {
            logger.debug("Finished {} ('{}').", handlerName, ctx.getNetworkElementFdn());
        }

        recordTimeTaken(ctx.getNetworkElementFdn(), startTime);

        // Cannot return original event headers as they may contain classes unresolvable by other containers
        return new MediationComponentEvent(new HashMap<>(), event.getPayload());
    }

    /**
     * If the Oss Model Identity has changed, updates the subscription for the node.
     *
     * @param ctx
     *            sync invocation context
     */
    private void updateSubscriptionIfOmiChanged(final SyncInvocationContext ctx) {
        if (ctx.isOssModelIdentityUpdated()) {
            logger.debug("Sending StartSubscriptionEvent for updating subscription for Node {}", ctx.getCmSupervisionFdn());
            final StartSubscriptionEvent event = new StartSubscriptionEvent(ctx.getCmSupervisionFdn());
            event.setUpdateOnly(true);
            mediationEventSender.send(event);
        }
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
        logger.debug("[{}] OSS Prefix is: {}", ctx.getNetworkElementFdn(), ossPrefix);
        ctx.setOssPrefix(ossPrefix);
    }

    /*
     * TODO: This should not be handled by exception. Possibility of the supervision being off is relatively high and it should be handled gracefully
     * by the system, giving the user proper feedback that he must turn on supervision before attempting to sync the node manually. Should be done
     * similarly to detection of sync in progress.
     */
    private void validateSupervisionOn(final SyncInvocationContext ctx) throws InboundDpsHandlerException {
        if (!cmSupervisionMoDpsOperator.getActive(ctx.getCmSupervisionFdn())) {
            throw new InboundDpsHandlerException(SUPERVISION_NOT_ACTIVE_ERROR_MESSAGE);
        }
    }

    private boolean canSyncProceed(final SyncInvocationContext ctx) {
        if (ctx.isCmFunctionSyncAction()) {
            final String currentSyncStatus = cmFunctionMoDpsOperator.getSyncStatus(ctx.getCmFunctionFdn());
            if (currentSyncStatus.equals(PENDING_SYNC_STATUS_ATTR_VALUE)) {
                // sync status set to pending by the CM Function sync action, must proceed with sync
                return true;
            }
        }
        return controllerMonitorOperator.verifySyncStatus(ctx, handlerName, false);
    }

    /**
     * Throws an exception if Software Sync signalled a critical failure by setting the {@code softwareSyncFailure}
     * flag in its completion event.
     *
     * @param ctx
     *            sync invocation context
     * @throws SoftwareSyncFailureException
     *             if Software Sync signalled a critical failure
     */
    private void checkSoftwareSyncFailure(final SyncInvocationContext ctx) throws SoftwareSyncFailureException {
        if (ctx.isSoftwareSyncFailure()) {
            throw new SoftwareSyncFailureException(ctx.getSoftwareSyncFailureMsg());
        }
    }

    /**
     * Performs MIM Switch as needed.
     *
     * @param ctx
     *            sync invocation context
     * @throws MimSwitchException
     *             if mim switch fails
     * @see MimSwitchHelper
     */
    private void performMimSwitchIfNeeded(final SyncInvocationContext ctx) throws MimSwitchException {
        final boolean mimSwitchPerformed = mimSwitchHelper.performMimSwitchIfNeeded(ctx.getNetworkElementFdn());
        logger.debug("[{}] MIM Switch performed: {}", ctx.getNetworkElementFdn(), mimSwitchPerformed);
        if (mimSwitchPerformed) {
            mimSwitchInstrumentation.increaseNumberOfMibUpgradePerformed(true);
        }
        ctx.setMimSwitchPerformed(mimSwitchPerformed);
    }

    /**
     * Evaluates the sync type to perform and send the event to initiate it.
     *
     * @param ctx
     *            sync invocation context
     */
    private void initiateSync(final SyncInvocationContext ctx) {
        final SyncType syncType = syncTypeEvaluationDpsOperator.evaluateSyncType(ctx);
        logger.debug("{} sync will be performed for node [{}].", syncType.name(), ctx.getNetworkElementFdn());
        final MediationTaskRequest syncEvent = createSyncEvent(syncType);
        syncEvent.setNodeAddress(ctx.getCmFunctionFdn());
        mediationEventSender.send(syncEvent);
    }

    private MediationTaskRequest createSyncEvent(final SyncType syncType) {
        // no need to pass the poId as the nodeAddress is set to the CM Function FDN before sending
        if (syncType == SyncType.DELTA) {
            return new StartDeltaSyncEvent(0L);
        } else {
            return new StartSyncEvent(0L);
        }
    }
}
