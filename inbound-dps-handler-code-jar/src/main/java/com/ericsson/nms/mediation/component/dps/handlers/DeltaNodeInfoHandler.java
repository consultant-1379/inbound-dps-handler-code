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

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.DELTA_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.DELTA_SYNC_START_TIME_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.INVOKING_FDN_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_PREFIX_HEADER_NAME;

import java.util.Map;

import javax.inject.Inject;

import com.ericsson.nms.mediation.component.dps.instrumentation.bindings.DeltaNodeInfoRetrievalDpsHandlerBinding;
import com.ericsson.nms.mediation.component.dps.operators.dps.DeltaNodeInfoReadDpsOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;

/**
 * Entry handler for the Delta Sync Node use case. First handler within the <em>DeltaSyncFlow</em>.
 * <p>
 * Responsible for obtaining the following information required for the consequent Delta Sync handler(s):
 * <ul>
 * <li>cmFunctionFdn (sent in the headers)</li>
 * <li>generationCounter (sent in the headers)</li>
 * <li>ossPrefix (sent in the headers)</li>
 * <li>Map including two elements: NodeNamespace and NodeVersion strings, representing Namespace-Version pair (sent as payload)</li>
 * </ul>
 */
@EventHandler(contextName = "")
public class DeltaNodeInfoHandler extends DpsHandler {
    private final ThreadLocal<Map<String, Object>> contextHeaders = new ThreadLocal<>();

    @Inject
    private DeltaNodeInfoReadDpsOperator deltaNodeInfoReadDpsOperator;
    @Inject
    private FailedSyncResultDpsOperator failedSyncResultDpsOperator;

    @Override
    protected void initHandler(final EventHandlerContext eventHandlerContext) {
        contextHeaders.set(eventHandlerContext.getEventHandlerConfiguration().getAllProperties());
    }

    @Override
    @DeltaNodeInfoRetrievalDpsHandlerBinding
    public ComponentEvent onEvent(final ComponentEvent event) {
        final long startTime = System.currentTimeMillis();
        final String cmFunctionFdn = (String) contextHeaders.get().get(INVOKING_FDN_HEADER_NAME);
        contextHeaders.remove();
        recordInvocationDetails(cmFunctionFdn, event);

        final String ossPrefix = (String) event.getHeaders().get(OSS_PREFIX_HEADER_NAME);

        final Map<String, Object> headers = event.getHeaders();

        Object payload = null;
        try {
            failedSyncResultDpsOperator.checkHeartbeatSupervision(cmFunctionFdn);
            cmFunctionMoDpsOperator.setSyncStatus(cmFunctionFdn, ossPrefix, DELTA_SYNC_STATUS_ATTR_VALUE, null);
            headers.put(DELTA_SYNC_START_TIME_HEADER_NAME, startTime);
            headers.put(INVOKING_FDN_HEADER_NAME, cmFunctionFdn);

            headers.putAll(deltaNodeInfoReadDpsOperator.createHeaders(cmFunctionFdn));
            payload = deltaNodeInfoReadDpsOperator.createPayload((String) headers.get(OSS_PREFIX_HEADER_NAME));
        } catch (final Exception exception) {
            failedSyncResultDpsOperator.handleErrorAndRethrowException(cmFunctionFdn, ossPrefix, exception, handlerName);
        }

        recordTimeTaken(cmFunctionFdn, startTime);

        logger.debug("Finished {} ('{}').", handlerName, cmFunctionFdn);
        return new MediationComponentEvent(headers, payload);
    }

}
