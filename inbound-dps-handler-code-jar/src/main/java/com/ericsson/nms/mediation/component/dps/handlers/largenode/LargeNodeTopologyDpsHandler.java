/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.handlers.largenode;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ATTRIBUTE_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.INVOKING_FDN_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.IS_FIRST_SYNC_FLAG;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_PREFIX_HEADER_NAME;

import java.util.Map;

import javax.inject.Inject;

import com.ericsson.enm.mediation.handler.common.dps.operators.NetworkElementMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.handlers.DpsHandler;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.TopologyWriteDpsOperator;
import com.ericsson.nms.mediation.component.dps.utility.InstrumentationUtil;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;

/**
 * Responsible for writing the topology data of the node (read and passed on by the previous step in the flow) to DPS.
 * <p>
 * Note: The topology data is a tree of MOs and does not contain the attributes associated with these MOs. The attributes data is read and written by
 * the consequent steps within the flow.
 */
@EventHandler(contextName = "")
public class LargeNodeTopologyDpsHandler extends DpsHandler {

    private final ThreadLocal<Map<String, Object>> contextHeaders = new ThreadLocal<>();

    @Inject
    private TopologyWriteDpsOperator topologyWriter;
    @Inject
    private NetworkElementMoDpsOperator networkElementMoReader;
    @Inject
    private FailedSyncResultDpsOperator failedSyncResultDpsOperator;

    @Override
    protected void initHandler(final EventHandlerContext eventHandlerContext) {
        contextHeaders.set(eventHandlerContext.getEventHandlerConfiguration().getAllProperties());
    }

    @Override
    public ComponentEvent onEvent(final ComponentEvent event) {
        final long startTime = System.currentTimeMillis();
        final Map<String, Object> headers = event.getHeaders();
        final String cmFunctionFdn = (String) contextHeaders.get().get(INVOKING_FDN_HEADER_NAME);
        String ossPrefix = null;
        headers.put(INVOKING_FDN_HEADER_NAME, cmFunctionFdn);
        contextHeaders.remove();

        logger.debug("LargeNodeAttributeDpsHandler header values: {}", headers);

        recordInvocationDetails(cmFunctionFdn, event);
        instrumentation.increaseDpsTopologySyncInvocations();

        try {
            rethrowExceptionIfOneFoundInPayload(event.getPayload());
            failedSyncResultDpsOperator.checkHeartbeatSupervision(cmFunctionFdn);
            ossPrefix = networkElementMoReader.getOssPrefix(FdnUtil.extractParentFdn(cmFunctionFdn));
            headers.put(OSS_PREFIX_HEADER_NAME, ossPrefix);
            final boolean isFirstSync = topologyWriter.isFirstSync((String) headers.get(OSS_PREFIX_HEADER_NAME));
            headers.put(IS_FIRST_SYNC_FLAG, isFirstSync);
            final int totalMos = topologyWriter.persistTopology(event, (String) headers.get(OSS_PREFIX_HEADER_NAME), isFirstSync);
            instrumentation.addToNumberOfMosBeingSyncedSamples((long) totalMos);
            cmFunctionMoDpsOperator.setSyncStatus(cmFunctionFdn, ossPrefix, ATTRIBUTE_SYNC_STATUS_ATTR_VALUE, null);
            cmFunctionMoDpsOperator.setTotalMOCount(cmFunctionFdn, totalMos);
        } catch (final Exception exception) {
            failedSyncResultDpsOperator.handleErrorAndRethrowException(cmFunctionFdn, ossPrefix, exception, handlerName);
        }

        recordTimeTaken(cmFunctionFdn, startTime);
        // TODO Include this in the recordTimeTaken() when instrumentation is defined globally across the handlers
        instrumentation.addToAverageDpsTopologyDataTimesTaken(InstrumentationUtil.calculateTimeTaken(startTime));

        logger.debug("Finished {} ('{}').", handlerName, cmFunctionFdn);
        return new MediationComponentEvent(headers, event.getPayload());
    }

}
