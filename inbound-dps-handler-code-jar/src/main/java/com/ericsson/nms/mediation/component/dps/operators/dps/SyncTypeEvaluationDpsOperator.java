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

package com.ericsson.nms.mediation.component.dps.operators.dps;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.RESET_GENERATION_COUNTER_ATTR_VALUE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_RESOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_SOURCE;

import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.common.SyncType;
import com.ericsson.nms.mediation.component.dps.handlers.context.SyncInvocationContext;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.network.api.MociCMConnectionProvider;
import com.ericsson.oss.mediation.network.api.util.ConnectionConfig;

/**
 * This is the operator used to determine which sync flow is to be used, Delta or Full sync. <br>
 * It contains a single method which checks if conditions have been met to perform a Delta Sync over a Full Sync.
 */
@ApplicationScoped
public class SyncTypeEvaluationDpsOperator {
    static final String COMMAND_NAME = "CONTROLLER_HANDLER";
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncTypeEvaluationDpsOperator.class);
    @Inject
    private CppCiMoDpsOperator cppCiMoDpsOperator;
    @Inject
    private SystemRecorder systemRecorder;
    private MociCMConnectionProvider mociCmConnectionProvider;

    /**
     * Evaluates the Sync Type to be performed.<br>
     * <p>
     * The following conditions require a FULL Sync to be performed:
     * <ol>
     * <li>If sync has been initiated via the CM Function sync action (flag {@code cmFunctionSyncAction} is set), or</li>
     * <li>If the ossModelIdentity has been updated (flag {@code ossModelIdentityUpdated} is set)</li>
     * <li>If a MIM Switch has been done</li>
     * </ol>
     * <br>
     * In all other cases, a DELTA Sync can be performed if the following three conditions are met:
     * <ol>
     * <li>The request comes from an Event-based Client</li>
     * <li>The restart date in the DPS is not null (confirming that at least one Full Sync have been run previously)</li>
     * <li>The restart date on the node matches the restart date in the DPS (confirming that the node has not been restarted)</li>
     * </ol>
     *
     * @param ctx
     *         sync invocation context
     * @return type of sync to take place (Full or Delta Sync)
     */
    public SyncType evaluateSyncType(final SyncInvocationContext ctx) {
        if (ctx.isCmFunctionSyncAction()) {
            LOGGER.debug("[{}] Sync initiated via the CM Function sync action, proceeding with FULL Sync.", ctx.getNetworkElementFdn());
            return SyncType.FULL;
        }
        if (ctx.isOssModelIdentityUpdated()) {
            LOGGER.debug("[{}] ossModelIdentity changed, proceeding with FULL Sync.", ctx.getNetworkElementFdn());
            return SyncType.FULL;
        }
        if (ctx.isMimSwitchPerformed()) {
            LOGGER.info("[{}] MIM Switch has been performed, proceeding with FULL Sync.", ctx.getNetworkElementFdn());
            return SyncType.FULL;
        }
        if (ctx.isEventBasedClient()) {
            final String rootFdn = ctx.getOssPrefix();
            final String cppCiFdn = FdnUtil.getCppCiFdn(rootFdn);
            final long generationCounter = cppCiMoDpsOperator.getGenerationCounter(cppCiFdn);
            Date dpsRestartDate = null;
            Date nodeRestartDate = null;
            if (generationCounter != RESET_GENERATION_COUNTER_ATTR_VALUE) {
                dpsRestartDate = cppCiMoDpsOperator.getRestartTimestamp(cppCiFdn);
                final String nodeIpAddress = cppCiMoDpsOperator.getIpAddress(cppCiFdn);
                LOGGER.debug("[{}] DPS Restart Timestamp for the node is: [{}].", rootFdn, dpsRestartDate);
                if (dpsRestartDate != null) {
                    nodeRestartDate = getNodeRestartDate(rootFdn, nodeIpAddress);
                    LOGGER.debug("[{}] Current Restart date on the node is: [{}]  DPS restart date is [{}] .", rootFdn, nodeRestartDate,
                            dpsRestartDate);
                    if (dpsRestartDate.equals(nodeRestartDate)) {
                        return SyncType.DELTA;
                    }
                }
            }
            systemRecorder.recordEvent(COMMAND_NAME, EventLevel.DETAILED, EVENT_SOURCE, EVENT_RESOURCE,
                    "Full sync was evaluated for the node " + ctx.getNetworkElementFdn()
                            + ", by pre-checks values [GC: " + generationCounter
                            + ", DPS Restart Timestamp: " + dpsRestartDate
                            + ", Node Restart Timestamp: " + nodeRestartDate + "].");
        }
        return SyncType.FULL;
    }

    public Date getNodeRestartDate(final String rootFdn, final String nodeIpAddress) {
        final ConnectionConfig connectionConfig = new ConnectionConfig(rootFdn, nodeIpAddress);
        try {
            final MociCMConnectionProvider mociConnectionProvider = getMociConnectionProviderInstance();
            return mociConnectionProvider.getRestartNodeDate(connectionConfig);
        } catch (final Exception e) {
            LOGGER.error("Node Connectivity Issue, Failure retrieving Restart Date for Node: {}, Connection {} {}", rootFdn, connectionConfig, e);
        }
        return null;
    }

    private MociCMConnectionProvider getMociConnectionProviderInstance() throws NamingException {
        if (mociCmConnectionProvider == null) {
            LOGGER.debug("Initiating the JNDI reference to Network element connector");
            final InitialContext jndiContext = new InitialContext();
            mociCmConnectionProvider = (MociCMConnectionProvider) jndiContext.lookup(MociCMConnectionProvider.VERSION_INDEPENDENT_JNDI_NAME);
            jndiContext.close();
        }
        return mociCmConnectionProvider;
    }
}