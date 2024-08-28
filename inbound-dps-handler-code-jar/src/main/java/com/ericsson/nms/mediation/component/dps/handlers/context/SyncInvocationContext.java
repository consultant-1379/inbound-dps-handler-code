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

package com.ericsson.nms.mediation.component.dps.handlers.context;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.NETWORK_ELEMENT_MO_TYPE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.CLIENT_TYPE_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.CM_FUNCTION_SYNC_ACTION_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.INVOKING_FDN_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_MODEL_IDENTITY_UPDATED_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SOFTWARE_SYNC_FAILURE_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SOFTWARE_SYNC_FAILURE_MSG_HEADER_NAME;

import java.util.Map;

import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.oss.mediation.core.events.MediationClientType;

/**
 * Class used to hold contextual data related to a sync invocation.
 */
public class SyncInvocationContext {

    private final String clientType;
    private final String invokingFdn;
    private final String networkElementFdn;
    private final String cmFunctionFdn;
    private final String cmSupervisionFdn;
    private final boolean cmFunctionSyncAction;
    private final boolean ossModelIdentityUpdated;
    private final boolean softwareSyncFailure;
    private final String softwareSyncFailureMsg;
    private String ossPrefix;
    private boolean mimSwitchPerformed;

    /**
     * Constructs a new instance from the specified context and event headers.
     *
     * @param contextHeaders
     *            map of handler context headers
     */
    public SyncInvocationContext(final Map<String, Object> contextHeaders) {
        clientType = (String) contextHeaders.get(CLIENT_TYPE_HEADER_NAME);
        invokingFdn = (String) contextHeaders.get(INVOKING_FDN_HEADER_NAME);
        networkElementFdn = FdnUtil.extractRdn(invokingFdn, NETWORK_ELEMENT_MO_TYPE);
        cmFunctionFdn = FdnUtil.appendCmFunction(networkElementFdn);
        cmSupervisionFdn = FdnUtil.appendCmSupervision(networkElementFdn);
        // headers populated according to Software Sync results
        cmFunctionSyncAction = Boolean.TRUE.equals(contextHeaders.get(CM_FUNCTION_SYNC_ACTION_HEADER_NAME));
        ossModelIdentityUpdated = Boolean.TRUE.equals(contextHeaders.get(OSS_MODEL_IDENTITY_UPDATED_HEADER_NAME));
        softwareSyncFailure = Boolean.TRUE.equals(contextHeaders.get(SOFTWARE_SYNC_FAILURE_HEADER_NAME));
        softwareSyncFailureMsg = (String) contextHeaders.get(SOFTWARE_SYNC_FAILURE_MSG_HEADER_NAME);
    }

    public String getClientType() {
        return clientType;
    }

    public boolean isEventBasedClient() {
        return MediationClientType.EVENT_BASED.toString().equalsIgnoreCase(clientType);
    }

    public String getInvokingFdn() {
        return invokingFdn;
    }

    public String getNetworkElementFdn() {
        return networkElementFdn;
    }

    public String getCmFunctionFdn() {
        return cmFunctionFdn;
    }

    public String getCmSupervisionFdn() {
        return cmSupervisionFdn;
    }

    public boolean isCmFunctionSyncAction() {
        return cmFunctionSyncAction;
    }

    public boolean isOssModelIdentityUpdated() {
        return ossModelIdentityUpdated;
    }

    public boolean isSoftwareSyncFailure() {
        return softwareSyncFailure;
    }

    public String getSoftwareSyncFailureMsg() {
        return softwareSyncFailureMsg;
    }

    public void setOssPrefix(final String ossPrefix) {
        this.ossPrefix = ossPrefix;
    }

    public String getOssPrefix() {
        return ossPrefix;
    }

    public void setMimSwitchPerformed(final boolean mimSwitchPerformed) {
        this.mimSwitchPerformed = mimSwitchPerformed;
    }

    public boolean isMimSwitchPerformed() {
        return mimSwitchPerformed;
    }

}
