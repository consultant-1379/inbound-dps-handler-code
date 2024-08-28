/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.common;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.NE_TYPE_ATTR_NAME;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;

/**
 * Setting neType value under ManagedElement MO when there is a full or delta sync.
 */
@Stateless
public class NeTypeSetter {

    private static final Logger logger = LoggerFactory.getLogger(NeTypeSetter.class);

    /**
     * Update neType under related ManagedElement MO in DPS if not null.
     *
     * @param liveBucket
     *            live bucket on the Data Persistence Service
     * @param ossPrefix
     *            Processed node ossPrefix
     * @param neType
     *            neType of the node
     */
    public void checkAndSetNeTypeAttribute(final DataBucket liveBucket, final String ossPrefix, final String neType) {
        logger.debug("Setting neType with value {} for a ManagedElement MO with ossPrefix: {}", neType, ossPrefix);
        if (ossPrefix == null || ossPrefix.isEmpty()) {
            logger.error("Unable to get ManagedElement FDN from invalid ossPrefix FDN: '{}', neType attribute will not be set", ossPrefix);
            return;
        }
        final String managedElementFdn = FdnUtil.appendManagedElement(ossPrefix);
        final ManagedObject managedElementMo = liveBucket.findMoByFdn(managedElementFdn);
        if (managedElementMo == null) {
            logger.error("Unable to get ManagedElement MO in DPS with FDN: {}, neType attribute will not be set", managedElementFdn);
        } else if (managedElementMo.getAttribute(NE_TYPE_ATTR_NAME) == null) {
            managedElementMo.setAttribute(NE_TYPE_ATTR_NAME, neType);
            logger.debug("Finished setting the neType with value {} for MO {}", neType, managedElementFdn);
        } else {
            logger.debug("NeType with value {} for MO {} is already set", neType, managedElementFdn);
        }
    }
}
