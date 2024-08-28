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

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.INVOKING_FDN_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.IS_FIRST_SYNC_FLAG;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_PREFIX_HEADER_NAME;

import static com.ericsson.oss.mediation.cm.constants.CommonConstants.SYNC_START_TIME_HEADER_NAME;

import javax.inject.Inject;

import com.ericsson.nms.mediation.component.dps.operators.dps.AttributeWriteDpsOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;

/**
 * Responsible for writing the attributes data of the node (read and passed on by the previous step in the flow) to DPS.
 * <p>
 * Note: The attributes and their values are associated with the MOs that were read and persisted previously by the topology handlers.
 */
@EventHandler(contextName = "")
public class AttributeDpsHandler extends DpsHandler {
    @Inject
    private AttributeWriteDpsOperator attributeWriter;
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
        final Long syncStartTime = (Long) event.getHeaders().get(SYNC_START_TIME_HEADER_NAME);
        final boolean isFirstSync = (Boolean) event.getHeaders().get(IS_FIRST_SYNC_FLAG);

        recordInvocationDetails(cmFunctionFdn, event);
        instrumentation.increaseDpsAttributeSyncInvocations();

        try {
            rethrowExceptionIfOneFoundInPayload(event.getPayload());
            failedSyncResultDpsOperator.checkHeartbeatSupervision(cmFunctionFdn);
            final int totalAttrs = attributeWriter.handleAttributesPersistence(event, ossPrefix, isFirstSync);
            recordTotalAttributesCount(ossPrefix, totalAttrs);
            instrumentation.addToNumberOfAttrBeingSyncedSamples((long) totalAttrs);
            cmFunctionMoDpsOperator.updateAttrsUponSuccessfulSync(cmFunctionFdn, ossPrefix, Boolean.FALSE);
        } catch (final Exception exception) {
            failedSyncResultDpsOperator.handleErrorAndRethrowException(cmFunctionFdn, ossPrefix, exception, handlerName);
        }

        recordTimeTaken(cmFunctionFdn, startTime);
        recordOverallSyncStatistics(cmFunctionFdn, syncStartTime, startTime);

        logger.debug("Finished {} ('{}').", handlerName, cmFunctionFdn);
        return event;
    }
}
