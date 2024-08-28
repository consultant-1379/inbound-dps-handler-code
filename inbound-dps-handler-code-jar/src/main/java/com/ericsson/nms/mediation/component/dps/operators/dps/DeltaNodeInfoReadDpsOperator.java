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

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.NODE_TYPE_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_MODEL_IDENTITY_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_PREFIX_HEADER_NAME;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.GENERATION_COUNTER_HEADER_NAME;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.NODE_NAMESPACE_KEY;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.NODE_VERSION_KEY;

import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.MoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.NetworkElementMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;

/**
 * Responsible for reading information from DPS that are required by the consequent Delta Sync handlers.
 */
@ApplicationScoped
public class DeltaNodeInfoReadDpsOperator {
    @EJB
    private MoDpsOperator moDpsOperator;
    @Inject
    private NetworkElementMoDpsOperator networkElementMoDpsOperator;
    @Inject
    private CppCiMoDpsOperator cppCiMoDpsOperator;

    /**
     * Resolves and returns a map of the following headers.
     * <ul>
     * <li>generationCounter</li>
     * <li>ossPrefix</li>
     * </ul>
     * @param cmFunctionFdn
     *            CmFunction FDN identifying the node
     * @return map of the selected header mentioned above
     */
    public Map<String, Object> createHeaders(final String cmFunctionFdn) {
        final Map<String, Object> headers = new HashMap<>();

        final String networkElementFdn = FdnUtil.extractParentFdn(cmFunctionFdn);
        final String cppCiFdn = FdnUtil.appendCppCi(networkElementFdn);
        headers.put(GENERATION_COUNTER_HEADER_NAME, cppCiMoDpsOperator.getGenerationCounter(cppCiFdn));
        headers.put(OSS_PREFIX_HEADER_NAME, networkElementMoDpsOperator.getOssPrefix(networkElementFdn));
        headers.put(NODE_TYPE_HEADER_NAME, networkElementMoDpsOperator.getNeType(networkElementFdn));
        headers.put(OSS_MODEL_IDENTITY_HEADER_NAME, networkElementMoDpsOperator.getOssModelIdentity(networkElementFdn));
        return headers;
    }

    /**
     * Resolves and returns a map containing the following elements:
     * <ul>
     * <li>node model namespace</li>
     * <li>node model version</li>
     * </ul>
     * <p>
     * <b>Note:</b> Currently obtaining this information from the ENodeBFunction MO, as all of the relevant MOs on the node will have the same
     * namespace-version pair.
     * @param meContextFdn
     *            MeContext FDN identifying the node
     * @return Collection of <code>MapTo</code> objects representing model namespace-version pairs.
     */
    public Object createPayload(final String meContextFdn) {
        final Map<String, String> payload = new HashMap<>();

        final String managedElementFdn = FdnUtil.appendManagedElement(meContextFdn);
        payload.put(NODE_NAMESPACE_KEY, moDpsOperator.getNamespace(managedElementFdn));
        payload.put(NODE_VERSION_KEY, moDpsOperator.getVersion(managedElementFdn));
        return payload;
    }
}
