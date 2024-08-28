/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.handlers;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.TOPOLOGY_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.INVOKING_FDN_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.LARGE_NODE_FLAG;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.LARGE_NODE_MO_THRESHOLD;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.MO_INSTANCE_COUNT;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.NODE_TYPE_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_MODEL_IDENTITY_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_PREFIX_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SOFTWARE_SYNC_MESSAGE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SOFTWARE_SYNC_SUCCESS;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SYNC_START_TIME_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SYNC_TYPE_HEADER_NAME;

import java.util.Map;

import javax.inject.Inject;

import com.ericsson.enm.mediation.handler.common.dps.api.OssModelIdentityException;
import com.ericsson.enm.mediation.handler.common.dps.operators.NetworkElementMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.common.SyncType;
import com.ericsson.nms.mediation.component.dps.handlers.exception.SoftwareSyncFailureException;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;

/**
 * Entry handler for the Full Sync Node use case. First handler within the <em>SyncNodeFlow</em>.
 * <p>
 * Responsible for obtaining the following information (which are then put in the headers) required for the consequent Full Sync handlers:
 * <ul>
 * <li>syncStartTime</li>
 * <li>SyncType</li>
 * <li>cmFunctionfdn</li>
 * <li>networkElement.neType attribute</li>
 * <li>networkElement.ossModelIIdentity attribute</li>
 * <li>networkElement.ossPrefix attribute</li>
 * </ul>
 */
@EventHandler(contextName = "")
public class NodeInfoDpsHandler extends DpsHandler {

    private final ThreadLocal<Map<String, Object>> contextHeaders = new ThreadLocal<>();

    @Inject
    private FailedSyncResultDpsOperator failedSyncResultDpsOperator;
    @Inject
    private NetworkElementMoDpsOperator networkElementMoDpsOperator;

    @Override
    protected void initHandler(final EventHandlerContext eventHandlerContext) {
        contextHeaders.set(eventHandlerContext.getEventHandlerConfiguration().getAllProperties());
    }

    @Override
    public ComponentEvent onEvent(final ComponentEvent event) {
        final long startTime = System.currentTimeMillis();

        final String cmFunctionFdn = (String) contextHeaders.get().get(INVOKING_FDN_HEADER_NAME);
        String ossPrefix = null;
        final Integer moInstanceCount = (Integer) contextHeaders.get().get(MO_INSTANCE_COUNT);
        final Boolean softwareSyncSuccess = (Boolean) contextHeaders.get().get(SOFTWARE_SYNC_SUCCESS);
        final String softwareSyncMessage = (String) contextHeaders.get().get(SOFTWARE_SYNC_MESSAGE);
        contextHeaders.remove();

        recordInvocationDetails(cmFunctionFdn, event);

        final Map<String, Object> headers = event.getHeaders();
        headers.put(SYNC_START_TIME_HEADER_NAME, startTime);
        headers.put(SYNC_TYPE_HEADER_NAME, SyncType.FULL.getType());
        headers.put(INVOKING_FDN_HEADER_NAME, cmFunctionFdn);

        final boolean largeNode = moInstanceCount != null && moInstanceCount > LARGE_NODE_MO_THRESHOLD;
        logger.debug("Adding largeNode attribute for fdn {} to the headers with value {} as moInstanceCount value is: {}", cmFunctionFdn, largeNode,
                moInstanceCount);
        headers.put(LARGE_NODE_FLAG, largeNode);

        try {
            failedSyncResultDpsOperator.checkHeartbeatSupervision(cmFunctionFdn);
            final String networkElementFdn = FdnUtil.extractParentFdn(cmFunctionFdn);
            ossPrefix = networkElementMoDpsOperator.getOssPrefix(networkElementFdn);
            headers.put(OSS_PREFIX_HEADER_NAME, ossPrefix);
            headers.put(NODE_TYPE_HEADER_NAME, networkElementMoDpsOperator.getNeType(networkElementFdn));
            headers.put(OSS_MODEL_IDENTITY_HEADER_NAME, networkElementMoDpsOperator.getOssModelIdentity(networkElementFdn));
            if (Boolean.FALSE.equals(softwareSyncSuccess)) {
                throw new SoftwareSyncFailureException(softwareSyncMessage);
            }
            cmFunctionMoDpsOperator.setSyncStatus(cmFunctionFdn, ossPrefix, TOPOLOGY_SYNC_STATUS_ATTR_VALUE, largeNode);
        } catch (final OssModelIdentityException | SoftwareSyncFailureException exception) {
            failedSyncResultDpsOperator.handleErrorAndRethrowException(cmFunctionFdn, ossPrefix, exception, handlerName,
                    FailedSyncResultDpsOperator.SUPPRESS_STACK);
        } catch (final Exception exception) {
            failedSyncResultDpsOperator.handleErrorAndRethrowException(cmFunctionFdn, ossPrefix, exception, handlerName);
        }

        recordTimeTaken(cmFunctionFdn, startTime);
        logger.debug("Finished {} ('{}').", handlerName, cmFunctionFdn);
        return new MediationComponentEvent(headers, event.getPayload());
    }

}
