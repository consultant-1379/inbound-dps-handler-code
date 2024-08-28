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

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.COMMA;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.EQUALS;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_RESOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_SOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.NODE_TYPE_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_MODEL_IDENTITY_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SHM_INVENTORY_MO_WITH_EQUALS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.blacklist.BlacklistConfigurationStorage;
import com.ericsson.enm.mediation.handler.common.event.CmEventSender;
import com.ericsson.nms.mediation.component.dps.handlers.config.DpsWriteConfigurationStorage;
import com.ericsson.nms.mediation.component.dps.utility.LoggingUtil;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.ericsson.oss.mediation.network.api.notifications.NotificationType;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

/**
 * Persists MO tree elements in DPS. Topology tree is initially extracted using Sync Node MOCI Handler.
 */
@Stateless
public class TopologyWriteDpsOperator {

    private static final Logger logger = LoggerFactory.getLogger(TopologyWriteDpsOperator.class);

    @Inject
    private BlacklistConfigurationStorage blacklistStorage;
    @Inject
    private TopologyWriteTxDpsOperator topologyTxOperator;
    @Inject
    private CmEventSender cmEventSender;
    @Inject
    private SystemRecorder systemRecorder;
    @SuppressWarnings("unused")
    @Inject
    private DpsWriteConfigurationStorage dpsWriteConfigurationStorage;
    private int topDpsWriteBatchSize = MiscellaneousConstants.DEFAULT_DPS_WRITE_BATCH_SIZE;

    /**
     * Persists all MOs read previously from the node (payload of the <code>event</code>) into DPS.
     *
     * @param event
     *            <code>ComponentEvent</code> containing all the topology MO data (in payload) that
     *            is to be persisted
     * @param rootMoFdn
     *            FDN of root MO, uniquely identifying the node on which the action is to be
     *            performed
     * @return number of MOs persisted into DPS
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int persistTopology(final ComponentEvent event, final String rootMoFdn, final boolean isFirstSync) {
        final Map<String, ModelInfo> payload = (Map<String, ModelInfo>) event.getPayload();
        final Map<String, Object> headers = event.getHeaders();
        final String nodeType = (String) headers.get(NODE_TYPE_HEADER_NAME);
        final String nodeVersion = (String) headers.get(OSS_MODEL_IDENTITY_HEADER_NAME);
        logger.debug("Starting to persist the topology for node '{}' with total MO count: {}", rootMoFdn, payload.size());
        final List<String> blackListedMOs = readBlackList(rootMoFdn, nodeType, nodeVersion);
        persistTopology(rootMoFdn, payload, blackListedMOs, isFirstSync);
        return payload.size();
    }

    private void persistTopology(final String rootMoFdn, final Map<String, ModelInfo> payload, final List<String> blackListedMOs,
            final boolean isFirstSync) {
        final List<String> fdnsToBeDeletedFromDps = new ArrayList<>();
        final Set<String> fdnsToBePersisted = new HashSet<>(payload.keySet().size());

        // Retrieve current topology start
        final Stopwatch getCurrentTopologyStopwatch = Stopwatch.createStarted();
        final int preSyncTopologySize = topologyTxOperator.retrieveDeltaTopology(rootMoFdn, payload, fdnsToBePersisted, fdnsToBeDeletedFromDps);
        getCurrentTopologyStopwatch.stop();
        // Retrieve current topology complete

        // Insert topology Start
        final Stopwatch persistTopologyStopwatch = Stopwatch.createStarted();
        final int totalMosCreated = writeToDps(rootMoFdn, fdnsToBePersisted, payload, blackListedMOs, fdnsToBeDeletedFromDps, isFirstSync);
        persistTopologyStopwatch.stop();
        // Insert topology Complete

        // Delete operation Start
        final Stopwatch deleteMosStopwatch = Stopwatch.createStarted();
        final int deletedMosSize = deleteMosFromDps(fdnsToBeDeletedFromDps, isFirstSync);
        deleteMosStopwatch.stop();
        // Delete operation Complete

        recordResults(rootMoFdn, payload.size(), totalMosCreated, preSyncTopologySize, deletedMosSize, getCurrentTopologyStopwatch,
                deleteMosStopwatch, persistTopologyStopwatch);
    }

    /**
     * Deletes all MOs in payload from DPS including all of its descendants.
     *
     * @param fdnsToBeDeleted
     *            The FDN of the ManagedObject to deleted from DPS
     * @return The total number of ManagedObjects deleted from DPS
     */
    private int deleteMosFromDps(final List<String> fdnsToBeDeleted, final boolean isFirstSync) {
        int deletedMosCount = 0;
        if (fdnsToBeDeleted.size() <= topDpsWriteBatchSize) {
            deletedMosCount += topologyTxOperator.deleteMosFromDps(fdnsToBeDeleted);
            if (!isFirstSync) {
                sendEventForDelete(fdnsToBeDeleted);
            }
        } else {
            final List<List<String>> batches = Lists.partition(fdnsToBeDeleted, topDpsWriteBatchSize);
            for (final List<String> batch : batches) {
                deletedMosCount += topologyTxOperator.deleteMosFromDps(batch);
                if (!isFirstSync) {
                    sendEventForDelete(batch);
                }
            }
        }
        logger.debug("Total count of MOs deleted from DPS: {}", deletedMosCount);
        return deletedMosCount;
    }

    /**
     * Sends NBI messages of deleted MOs.
     * @param deletedFdns
     *            List of strings that are deleted.
     */
    private void sendEventForDelete(final List<String> deletedFdns) {
        final Collection<Serializable> deletedNotifs = new LinkedList<>();
        for (final String fdn : deletedFdns) {
            final NodeNotification nodeNotification = new NodeNotification();
            nodeNotification.setFdn(fdn);
            nodeNotification.setCreationTimestamp(new Date());
            nodeNotification.setAction(NotificationType.DELETE);
            deletedNotifs.add(nodeNotification);
        }
        cmEventSender.sendBatchEvents(deletedNotifs);
    }

    /**
     * This method is used by DpsWriteConfigurationStorage whenever the value change for config param topology_dps_write_batch_size.
     *
     * @param topDpsWriteBatchSize
     *            new value for config param topology_dps_write_batch_size.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setTopDpsWriteBatchSize(final int topDpsWriteBatchSize) {
        this.topDpsWriteBatchSize = topDpsWriteBatchSize;
    }

    private boolean isMoBlackListed(final String fdnToProcess, final List<String> blackListedMos) {
        for (final String blackListedMo : blackListedMos) {
            // Below condition ensures that the string concatenation is avoided for every call unless there is a match for managed object
            if (fdnToProcess.contains(blackListedMo) && fdnToProcess.contains(COMMA + blackListedMo + EQUALS)) {
                return true;
            }
        }
        return false;
    }

    private int writeToDps(final String rootMoFdn, final Set<String> fdnsToBePersisted, final Map<String, ModelInfo> payload,
            final List<String> blackListedMOs, final List<String> fdnsToBeDeletedFromDps, final boolean isFirstSync) {
        int numberOfMosWritten = 0;
        final List<String> payloadChunk = new ArrayList<>();
        final Iterator<Entry<String, ModelInfo>> payloadIterator = payload.entrySet().iterator();
        while (payloadIterator.hasNext()) {
            final Entry<String, ModelInfo> payloadEntrySet = payloadIterator.next();
            final String fdnToProcess = payloadEntrySet.getKey();
            // If this is an Blacklisted MO skip processing it
            if (isMoBlackListed(fdnToProcess, blackListedMOs)) {
                logger.debug("Skipping Blacklisted MO {} ", fdnToProcess);
                fdnsToBeDeletedFromDps.add(fdnToProcess);
                payloadIterator.remove();
                continue;
            }
            if (!fdnsToBePersisted.contains(fdnToProcess) || fdnToProcess.contains(SHM_INVENTORY_MO_WITH_EQUALS)) {
                logger.trace("Skipping MO {} ", fdnToProcess);
                continue;
            }
            payloadChunk.add(fdnToProcess);
            if (payloadChunk.size() > topDpsWriteBatchSize) {
                logger.debug("The payload size {} is greater than {}, writing it to Dps", payloadChunk.size(), topDpsWriteBatchSize);
                numberOfMosWritten += write(rootMoFdn, Collections.unmodifiableMap(payload), payloadChunk, isFirstSync);
            }
        }
        if (!payloadChunk.isEmpty()) {
            numberOfMosWritten += write(rootMoFdn, Collections.unmodifiableMap(payload), payloadChunk, isFirstSync);
        }
        logger.debug("Total Topology MOs written to DPS: {} for FDN: '{}'", numberOfMosWritten, rootMoFdn);
        return numberOfMosWritten;
    }

    private int write(final String rootMoFdn, final Map<String, ModelInfo> payload, final List<String> payloadChunk, final boolean isFirstSync) {
        logger.trace("Writing topology of size: {} for node {}", payloadChunk.size(), rootMoFdn);
        final int mosWriten = topologyTxOperator.invokeWriteMosToDps(payload, payloadChunk, rootMoFdn, isFirstSync);
        logger.debug("Completed writing topology of size: {} for node {}", payloadChunk.size(), rootMoFdn);
        payloadChunk.clear();
        return mosWriten;
    }

    @SuppressWarnings("parameternumber")
    private void recordResults(final String rootMoFdn, final int totalElements, final int totalMosCreated, final int preSyncTopologySize,
            final int deletedMosSize, final Stopwatch getCurrentTopologyStopwatch, final Stopwatch deleteMosStopwatch,
            final Stopwatch persistTopologyStopwatch) {
        final Map<String, Object> syncMetricsMap = contructSyncMetricsMap(rootMoFdn, totalElements, preSyncTopologySize,
                getCurrentTopologyStopwatch.elapsed(TimeUnit.MILLISECONDS), totalMosCreated,
                persistTopologyStopwatch.elapsed(TimeUnit.MILLISECONDS), deletedMosSize, deleteMosStopwatch.elapsed(TimeUnit.MILLISECONDS));
        systemRecorder.recordEvent(LoggingUtil.convertToRecordingFormat(MiscellaneousConstants.HANDLER_NAME_TOPOLOGY_SYNC), EventLevel.DETAILED,
                EVENT_SOURCE, EVENT_RESOURCE, LoggingUtil.constructSyncMetricsLogLine(syncMetricsMap));
    }

    /**
     * TODO TORF-114720 has been written so that we can improve the way we log in general. If log
     * statements are generic, then the code can also be generic.
     */
    @SuppressWarnings("parameternumber")
    private Map<String, Object> contructSyncMetricsMap(final String rootMoFdn, final int totalElements,
            final int preSyncTopologySize, final long retrieveTopologyTime, final int totalMosCreated,
            final long createMosTime, final int mosToBeDeletedFromDpsSize, final long deleteMosTime) {
        final Map<String, Object> syncMetricsMap = new HashMap<>();
        syncMetricsMap.put(MiscellaneousConstants.ROOT_MO, rootMoFdn);
        syncMetricsMap.put(MiscellaneousConstants.TOTAL_NUMBER_OF_MOS, totalElements);
        syncMetricsMap.put(MiscellaneousConstants.PRE_TOPOLOGY_SIZE, preSyncTopologySize);
        syncMetricsMap.put(MiscellaneousConstants.TIME_TAKEN_READ_TOPOLOGY, retrieveTopologyTime);
        syncMetricsMap.put(MiscellaneousConstants.NUMBER_MOS_CREATED, totalMosCreated);
        syncMetricsMap.put(MiscellaneousConstants.TIME_TAKEN_TO_CREATE_MOS, createMosTime);
        syncMetricsMap.put(MiscellaneousConstants.NUMBER_MOS_DELETED, mosToBeDeletedFromDpsSize);
        syncMetricsMap.put(MiscellaneousConstants.TIME_TAKEN_TO_DELETE_MOS, deleteMosTime);
        return syncMetricsMap;
    }

    private List<String> readBlackList(final String rootMoFdn, final String nodeType, final String nodeVersion) {
        List<String> blackListMos = null;
        logger.debug("Retrieving blacklist for node: {} version: {}", nodeType, nodeVersion);
        if (nodeType != null && nodeVersion != null) {
            blackListMos = blacklistStorage.getBlacklistMoForSync(nodeVersion, nodeType);
            logger.debug("Blacklist content for node:{} is: {}", rootMoFdn, blackListMos);
        }
        return blackListMos;
    }

    /**
     * Checks that sync is the first one.
     * @param rootMoFdn
     *         String FDN of root managed object.
     * @return Boolean result is sync first in the ENM.
     */
    public boolean isFirstSync(final String rootMoFdn) {
        return topologyTxOperator.checkIsFirstSync(rootMoFdn);
    }

}
