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

package com.ericsson.nms.mediation.component.dps.operators;

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_RESOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_SOURCE;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.event.MediationEventSender;
import com.ericsson.nms.mediation.component.dps.handlers.context.SyncInvocationContext;
import com.ericsson.nms.mediation.component.dps.operators.dps.SyncTypeEvaluationDpsOperator;
import com.ericsson.nms.mediation.component.dps.utility.LoggingUtil;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.cm.events.SyncNotificationStarterEvent;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Operator to execute sync in progress check and set sync status to PENDING in mutual exclusion for the same node.
 * <p>
 * If there is a sync in progress and ModelIdentity is changed defer the sync until the previous is finished.
 * In case of multiple requests only the last one is resent.
 * <p>
 * Else with a sync in progress the request is simply logged and discarded.
 * <p>
 * This logic cannot be executed concurrently for the same node, but can (and has to) be executed concurrently for different nodes.
 * Given how sticky distribution works, it can be assumed that same MediationService will always take care of one specific node, so it can be
 * guaranteed that this check will not be done for the same node at the same time in different MediationService containers.
 */
@Singleton
@LocalBean
@TransactionManagement(TransactionManagementType.BEAN)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ControllerMonitorOperator {
    static final String SYNC_DEFERRED_EVENT_TYPE = "SYNC_DEFERRED";
    static final String SYNC_IN_PROGRESS_EVENT_TYPE = "SYNC_ALREADY_IN_PROGRESS";
    private static final String HANDLER_NAME = LoggingUtil.convertToUpperCaseWithSpaces(ControllerMonitorOperator.class.getSimpleName());
    private static final String SYNC_RESCHEDULED_EVENT_TYPE = "SYNC_RESCHEDULED";
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerMonitorOperator.class);
    private final LoadingCache<String, Lock> nodeLocks = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, Lock>() {
                @Override
                public Lock load(final String neFdn) {
                    return new ReentrantLock(true);
                }
            });
    private final Map<String, Boolean> deferredNodeSync = new ConcurrentHashMap<>();

    @Inject
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperator;
    @Inject
    private MediationEventSender mediationEventSender;
    @Inject
    private CppCiMoDpsOperator cppCiMoDpsOperator;
    @Inject
    private SyncTypeEvaluationDpsOperator syncTypeEvaluationDpsOperator;
    @Inject
    private SystemRecorder systemRecorder;

    static String buildEventType(final String handlerName, final String eventType) {
        return LoggingUtil.convertToRecordingFormat(handlerName) + "_" + eventType;
    }

    /**
     * When an ongoing sync completes for a NE whose OMI was updated or node restart triggered, send a deferred sync request.
     *
     * @param nodeId     the name of the node for which a sync has been completed
     * @param syncStatus the sync status
     */
    @Asynchronous
    public void processSyncCompleted(final String nodeId, final String syncStatus) {
        LOGGER.debug("Received SyncStatus completed event for node {}: {}", nodeId, syncStatus);
        sendDeferredSync(nodeId);
    }

    private void sendDeferredSync(final String nodeId) {
        final String networkElementFdn = FdnUtil.getNetworkElementFdn(nodeId);
        final String cmFunctionFdn = FdnUtil.appendCmFunction(networkElementFdn);
        if (deferredNodeSync.containsKey(cmFunctionFdn)) {
            final Lock lock = nodeLocks.getUnchecked(cmFunctionFdn);
            lock.lock();
            try {
                if (deferredNodeSync.containsKey(cmFunctionFdn)) {
                    final boolean ossModelIdentityChanged = deferredNodeSync.remove(cmFunctionFdn);
                    sendResyncEvent(cmFunctionFdn, ossModelIdentityChanged);
                    recordDeferredSyncResend(networkElementFdn);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void sendResyncEvent(final String cmFunctionFdn, final boolean ossModelIdentityChanged) {
        final SyncNotificationStarterEvent startSyncEvent = new SyncNotificationStarterEvent(cmFunctionFdn);
        startSyncEvent.setOssModelIdentityUpdated(ossModelIdentityChanged);
        mediationEventSender.send(startSyncEvent);
    }

    public boolean verifySyncStatus(final SyncInvocationContext ctx, final String handlerName, final boolean ignoreDeferringSync) {
        final String cmFunctionFdn = ctx.getCmFunctionFdn();
        final Lock lock = nodeLocks.getUnchecked(cmFunctionFdn);
        lock.lock();
        try {
            final boolean updated = cmFunctionMoDpsOperator.checkAndSetSyncInProgress(cmFunctionFdn);
            LOGGER.debug("The result for the sync status set 'IN PROGRESS' for '{}' is {}.", cmFunctionFdn, updated);
            if (updated) {
                if (cmFunctionMoDpsOperator.isNodeSynced(cmFunctionFdn)) {
                    cmFunctionMoDpsOperator.setLostSynchronizationToCurrentDate(cmFunctionFdn);
                }
                cancelDeferredSync(cmFunctionFdn);
                return true;
            }

            if (ignoreDeferringSync) {
                recordSyncInProgress(ctx.getNetworkElementFdn(), handlerName);
            } else {
                checkAndDeferSync(ctx, handlerName);
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    private void addDeferredSync(final String cmFunctionFdn, final boolean ossModelIdentityUpdated) {
        deferredNodeSync.put(cmFunctionFdn, ossModelIdentityUpdated);
    }

    private void cancelDeferredSync(final String cmFunctionFdn) {
        deferredNodeSync.remove(cmFunctionFdn);
    }

    private void checkAndDeferSync(final SyncInvocationContext ctx, final String handlerName) {
        boolean deferSync = ctx.isOssModelIdentityUpdated();
        if (!deferSync) {
            deferSync = hasNodeRestarted(ctx.getOssPrefix());
        }

        if (deferSync) {
            addDeferredSync(ctx.getCmFunctionFdn(), ctx.isOssModelIdentityUpdated());
            recordSyncDeferred(ctx.getNetworkElementFdn(), handlerName);
            return;
        }
        recordSyncInProgress(ctx.getNetworkElementFdn(), handlerName);
    }

    private boolean hasNodeRestarted(final String ossPrefix) {
        final String cppCiFdn = FdnUtil.getCppCiFdn(ossPrefix);
        final String nodeIpAddress = cppCiMoDpsOperator.getIpAddress(cppCiFdn);
        final Date nodeRestartDate = syncTypeEvaluationDpsOperator.getNodeRestartDate(ossPrefix, nodeIpAddress);
        final Date dpsRestartDate = cppCiMoDpsOperator.getRestartTimestamp(cppCiFdn);
        LOGGER.debug("Comparing DPS restart date {} and Node restart date {} for ossPrefix: {}", dpsRestartDate, nodeRestartDate, ossPrefix);
        return nodeRestartDate == null || !nodeRestartDate.equals(dpsRestartDate);
    }

    private void recordSyncDeferred(final String networkElementFdn, final String handlerName) {
        final String deferredLogLine = "A sync operation is already in progress for the node: '" + networkElementFdn
                + "'. Sync request has been deferred until the current sync operation completes.";
        systemRecorder.recordEvent(buildEventType(handlerName, SYNC_DEFERRED_EVENT_TYPE), EventLevel.DETAILED,
                EVENT_SOURCE, EVENT_RESOURCE, deferredLogLine);
    }

    private void recordDeferredSyncResend(final String networkElementFdn) {
        final String deferredLogLine = "A previously deferred sync operation for the node: '" + networkElementFdn + "' has been rescheduled";
        systemRecorder.recordEvent(buildEventType(HANDLER_NAME, SYNC_RESCHEDULED_EVENT_TYPE),
                EventLevel.DETAILED, EVENT_SOURCE, EVENT_RESOURCE, deferredLogLine);
    }

    private void recordSyncInProgress(final String networkElementFdn, final String handlerName) {
        final String inProgressLogLine = "A sync operation is already in progress for the node: '" + networkElementFdn
                + "'. Will not invoke another sync until this one has completed.";
        systemRecorder.recordEvent(buildEventType(handlerName, SYNC_IN_PROGRESS_EVENT_TYPE), EventLevel.DETAILED,
                EVENT_SOURCE, EVENT_RESOURCE, inProgressLogLine);
    }
}
