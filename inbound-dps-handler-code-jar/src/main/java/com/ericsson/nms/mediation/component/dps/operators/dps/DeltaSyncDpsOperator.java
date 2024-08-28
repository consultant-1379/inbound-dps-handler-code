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

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_RESOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_SOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.INVOKING_FDN_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.NODE_TYPE_HEADER_NAME;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.DpsFacade;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.handlers.algorithms.DeltaSyncAlgorithm;
import com.ericsson.nms.mediation.component.dps.handlers.payload.DeltaSyncDpsPayload;
import com.ericsson.nms.mediation.component.dps.instrumentation.bindings.DeltaSyncDpsOperationBinding;
import com.ericsson.nms.mediation.component.dps.operators.RetryOperator;
import com.ericsson.nms.mediation.component.dps.utility.LoggingUtil;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.google.common.base.Stopwatch;

/**
 * Operation which persists changes retrieved form the node when executing a Delta Sync.
 */
@Stateless
public class DeltaSyncDpsOperator {

    private static final Logger logger = LoggerFactory.getLogger(DeltaSyncDpsOperator.class);

    @Inject
    private DpsFacade dpsFacade;
    @Inject
    private RetryOperator dpsOperator;
    @Inject
    private DeltaSyncAlgorithm deltaSyncAlgorithm;
    @Inject
    private CppCiMoDpsOperator cppCiMoDpsOperator;
    @Inject
    private SystemRecorder systemRecorder;

    /**
     * Persists all the Delta Sync changes (held in the payload of the <code>event</code>) into DPS.
     *
     * @param event
     *            <code>ComponentEvent</code> containing all the Delta Sync changes data (in payload) that is to be persisted
     * @param rootFdn
     *            FDN of root MO, uniquely identifying the node on which the action is to be performed
     * @return number of Delta Sync changes persisted into DPS
     */
    @DeltaSyncDpsOperationBinding
    public int persistDeltaSyncChanges(final ComponentEvent event, final String rootFdn) {
        logger.trace("Invoking Delta Sync DPS operation ('{}')...", rootFdn);

        final String cmFunctionFdn = (String) event.getHeaders().get(INVOKING_FDN_HEADER_NAME);
        final String neType = (String) event.getHeaders().get(NODE_TYPE_HEADER_NAME);
        final DeltaSyncDpsPayload payload = (DeltaSyncDpsPayload) event.getPayload();
        final Collection<NodeNotification> createAndUpdateChanges = payload.getCreateAndUpdateChanges();
        final Collection<NodeNotification> deleteChanges = payload.getDeleteChanges();

        final DataBucket liveBucket = dpsFacade.getLiveBucket();
        final Stopwatch stopwatchProcessDelete = Stopwatch.createStarted();
        deltaSyncAlgorithm.processDeleteChanges(deleteChanges, liveBucket, rootFdn);
        stopwatchProcessDelete.stop();
        final Stopwatch stopwatchProcessCreateUpdate = Stopwatch.createStarted();
        deltaSyncAlgorithm.processCreateUpdateChanges(createAndUpdateChanges, liveBucket, rootFdn);
        dpsOperator.setNeTypeAttribute(rootFdn, neType);
        stopwatchProcessCreateUpdate.stop();

        final long lastGenerationCounterFromEvents = payload.getGenerationCounter();
        final long generationCounterFromDps = (long) event.getHeaders().get(MiscellaneousConstants.GENERATION_COUNTER_HEADER_NAME);
        long generationCounterDifference = 0;
        if (lastGenerationCounterFromEvents > 0) {
            final String cppCiFdn = FdnUtil.getCppCiFdn(rootFdn);
            cppCiMoDpsOperator.setGenerationCounter(cppCiFdn, lastGenerationCounterFromEvents);
            generationCounterDifference = lastGenerationCounterFromEvents - generationCounterFromDps;
        }

        final int totalChanges = createAndUpdateChanges.size() + deleteChanges.size();

        final Map<String, Object> deltaSyncMetricsMap = contructDeltaSyncMetricsMap(cmFunctionFdn, createAndUpdateChanges.size(),
                stopwatchProcessCreateUpdate.elapsed(TimeUnit.MILLISECONDS),
                deleteChanges.size(),
                stopwatchProcessDelete.elapsed(TimeUnit.MILLISECONDS),
                generationCounterDifference);

        systemRecorder.recordEvent(
                LoggingUtil.convertToRecordingFormat(MiscellaneousConstants.HANDLER_NAME_DELTA_SYNC),
                EventLevel.DETAILED,
                EVENT_SOURCE,
                EVENT_RESOURCE,
                LoggingUtil.constructDeltaSyncMetricsLogLine(deltaSyncMetricsMap));

        return totalChanges;
    }

    /**
     * TODO TORF-114720 has been written so that we can improve the way we log in general. If log statements are generic, then the code can
     * also be generic.
     */
    private Map<String, Object> contructDeltaSyncMetricsMap(final String cmFunctionFdn, final int createAndUpdateChanges,
            final long processCreateUpdateTime,
            final int deleteChanges, final long processDeleteTime,
            final long generationCounterDifference) {
        final Map<String, Object> syncMetricsMap = new HashMap<>();
        syncMetricsMap.put(MiscellaneousConstants.ROOT_MO, cmFunctionFdn);
        syncMetricsMap.put(MiscellaneousConstants.NUMBER_MOS_CREATED, createAndUpdateChanges);
        syncMetricsMap.put(MiscellaneousConstants.TIME_TAKEN_TO_CREATE_MOS, processCreateUpdateTime);
        syncMetricsMap.put(MiscellaneousConstants.NUMBER_MOS_DELETED, deleteChanges);
        syncMetricsMap.put(MiscellaneousConstants.TIME_TAKEN_TO_DELETE_MOS, processDeleteTime);
        syncMetricsMap.put(MiscellaneousConstants.GENERATION_COUNTER_DIFFERENCE, generationCounterDifference);
        return syncMetricsMap;
    }
}
