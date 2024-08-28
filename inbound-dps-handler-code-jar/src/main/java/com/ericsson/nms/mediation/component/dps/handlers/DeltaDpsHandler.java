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

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.COMPLETE_SYNC_LABEL;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.DELTA_SYNC_START_TIME_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_RESOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_SOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.INVOKING_FDN_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_PREFIX_HEADER_NAME;

import javax.inject.Inject;

import com.ericsson.nms.mediation.component.dps.instrumentation.bindings.DeltaDpsHandlerBinding;
import com.ericsson.nms.mediation.component.dps.instrumentation.bindings.DeltaSyncOverAllTimeTakenBinding;
import com.ericsson.nms.mediation.component.dps.instrumentation.bindings.SyncInvocationBinding;
import com.ericsson.nms.mediation.component.dps.operators.dps.DeltaSyncDpsOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.nms.mediation.component.dps.utility.InstrumentationUtil;
import com.ericsson.nms.mediation.component.dps.utility.LoggingUtil;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;

/**
 * Responsible for writing the attributes data of the node (read and passed on by the previous steps in the flow) to DPS.
 */
@EventHandler(contextName = "")
public class DeltaDpsHandler extends DpsHandler {

    @Inject
    private DeltaSyncDpsOperator deltaSyncWriter;
    @Inject
    private FailedSyncResultDpsOperator failedSyncResultDpsOperator;

    @Override
    protected void initHandler(final EventHandlerContext eventHandlerContext) {
        // Nothing to initialize
    }

    @Override
    @DeltaDpsHandlerBinding
    @SyncInvocationBinding
    @DeltaSyncOverAllTimeTakenBinding
    public ComponentEvent onEvent(final ComponentEvent event) {
        final long startTime = System.currentTimeMillis();
        final long deltaSyncStartTime = (Long) event.getHeaders().get(DELTA_SYNC_START_TIME_HEADER_NAME);
        final String cmFunctionFdn = (String) event.getHeaders().get(INVOKING_FDN_HEADER_NAME);
        final String ossPrefix = (String) event.getHeaders().get(OSS_PREFIX_HEADER_NAME);

        recordInvocationDetails(cmFunctionFdn, event);

        try {
            failedSyncResultDpsOperator.checkHeartbeatSupervision(cmFunctionFdn);
            deltaSyncWriter.persistDeltaSyncChanges(event, ossPrefix);
            cmFunctionMoDpsOperator.updateAttrsUponSuccessfulSync(cmFunctionFdn, ossPrefix);
            recordTimeTaken(cmFunctionFdn, startTime);
            recordOverallSyncStats(cmFunctionFdn, deltaSyncStartTime);
        } catch (final Exception exception) {
            failedSyncResultDpsOperator.handleErrorAndRethrowException(cmFunctionFdn, ossPrefix, exception, handlerName);
        }
        logger.debug("Finished {} ('{}').", handlerName, cmFunctionFdn);
        return event;
    }

    private void recordOverallSyncStats(final String cmFunctionFdn, final long deltaSyncStartTime) {
        final Long overallSyncTimeTaken = InstrumentationUtil.calculateTimeTaken(deltaSyncStartTime);
        final String overallSyncTimeTakenLogLine = LoggingUtil.constructTimeTakenLogLine(COMPLETE_SYNC_LABEL, cmFunctionFdn, overallSyncTimeTaken);
        logger.debug(overallSyncTimeTakenLogLine);
        systemRecorder.recordEvent(LoggingUtil.convertToRecordingFormat(handlerName + "_" + COMPLETE_SYNC_LABEL), EventLevel.DETAILED, EVENT_SOURCE,
                EVENT_RESOURCE, overallSyncTimeTakenLogLine);
    }
}
