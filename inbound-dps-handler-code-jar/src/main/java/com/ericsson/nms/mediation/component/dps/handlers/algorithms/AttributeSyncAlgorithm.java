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

package com.ericsson.nms.mediation.component.dps.handlers.algorithms;

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SHM_INVENTORY_MO_WITH_EQUALS;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.util.DpsFacade;
import com.ericsson.enm.mediation.handler.common.event.CmEventSender;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.ericsson.oss.mediation.network.api.notifications.NotificationType;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

/**
 * Algorithm for traversing the topology tree and persisting the attribute data on the relevant MOs.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class AttributeSyncAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(AttributeSyncAlgorithm.class);

    @Inject
    private DpsFacade dpsFacade;
    @Inject
    private CmEventSender cmEventSender;

    private long attrWriteMaxTime = MiscellaneousConstants.DEFAULT_ATTRIBUTE_WRITE_MAX_TIME;

    /**
     * Writes the attributes provided in the attribute map to DPS.
     * @param rootMoFdn
     *            Root of the topology tree for example: MeContext=LTE001
     * @param attributesMap
     *            The attributes map obtained from the node to be written to DPS
     * @param attributesBatchSize
     *            The attributes batch size
     * @return The number of attributes persisted to DPS
     */
    public int writeAttributes(final String rootMoFdn, final Map<String, Map<String, Object>> attributesMap,
                               final long attributesBatchSize, final boolean isFirstSync) {
        int attributesCounter = 0;
        final long startTime = System.currentTimeMillis();
        final Iterator<ManagedObject> iter = getContainmentQueryIterator(rootMoFdn);
        final Collection<Serializable> listOfNotifs = new LinkedList<>();
        while (iter.hasNext() && attributesCounter < attributesBatchSize && !hasTimedOut(startTime)) {
            final ManagedObject mo = iter.next();
            final String moFdn = mo.getFdn();

            final Map<String, Object> moAtts = attributesMap.get(moFdn);
            if (mo.getFdn().contains(SHM_INVENTORY_MO_WITH_EQUALS) || moAtts == null || moAtts.isEmpty()) {
                attributesMap.remove(moFdn);
                logger.debug("Removed mo fdn {} for node {}", moFdn, rootMoFdn);
                continue;
            }
            if (!isFirstSync) {
                final NodeNotification attributeChangeNotif = buildAttributeChangeNotif(mo.getFdn(), mo.getAllAttributes(), moAtts);
                if (attributeChangeNotif != null) {
                    listOfNotifs.add(attributeChangeNotif);
                }
            }
            mo.setAttributes(moAtts);
            attributesCounter += moAtts.size();
            attributesMap.remove(moFdn);
        }

        if (!isFirstSync) {
            cmEventSender.sendBatchEvents(listOfNotifs);
            logger.trace("Attribute change notifications sent to NBI, total: {}", listOfNotifs.size());
        }

        if (!attributesMap.isEmpty() && attributesCounter < attributesBatchSize && !hasTimedOut(startTime)) {
            logger.warn("Ignoring extra FDNs which are not present in the topology tree. {}", attributesMap.keySet());
            attributesMap.clear();
        }
        logger.debug("{} attributes have been applied for the MO's topology within {} ms.",
                attributesCounter, System.currentTimeMillis() - startTime);
        return attributesCounter;
    }

    /**
     * This method is used by DpsWriteConfigurationStorage whenever the value change for config param attributes_dps_write_max_time.
     *
     * @param newAttrDpsWriteMaxTime
     *            new value of config param attributes_dps_write_max_time.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setAttrWriteMaxTime(final long newAttrDpsWriteMaxTime) {
        attrWriteMaxTime = newAttrDpsWriteMaxTime;
    }

    /**
     * Creates Node Notification if there are any differences between.
     *
     * @param fdn
     *           String fdn of the MO.
     * @param dpsAttributes
     *           Map String, Object attributes in dps.
     * @param moAtts
     *           Map String, Object attributes on the node.
     * @return NodeNotification of type update.
     */
    public NodeNotification buildAttributeChangeNotif(final String fdn, final Map<String, Object> dpsAttributes, final Map<String, Object> moAtts) {
        final Map<String, Object> attributesDelta = findDifferenceInAttributes(dpsAttributes, moAtts);
        if (!attributesDelta.isEmpty()) {
            logger.debug("Found an change for fdn: {}", fdn);
            final NodeNotification nodeNotif = new NodeNotification();
            nodeNotif.setFdn(fdn);
            nodeNotif.setCreationTimestamp(new Date());
            nodeNotif.setAction(NotificationType.UPDATE);
            nodeNotif.setUpdateAttributes(attributesDelta);
            return nodeNotif;
        } else {
            return null;
        }
    }
    //similar code is in DeltaSyncAlgorithm, could be reuser or refactored
    private Map<String, Object> findDifferenceInAttributes(final Map<String, Object> dpsAtts, final Map<String, Object> moAtts) {
        final MapDifference<String, Object> difference = Maps.difference(dpsAtts, moAtts);
        final Map<String, MapDifference.ValueDifference<Object>> entriesDiffering = difference.entriesDiffering();
        final Map<String, Object> changedAttributes = new HashMap<>();
        for (final String key : entriesDiffering.keySet()) {
            changedAttributes.put(key, moAtts.get(key));
        }
        return changedAttributes;
    }

    private Iterator<ManagedObject> getContainmentQueryIterator(final String rootMoFdn) {
        final QueryBuilder qb = dpsFacade.getQueryBuilder();
        final Query<ContainmentRestrictionBuilder> q = qb.createContainmentQuery(rootMoFdn);
        return dpsFacade.executeQuery(q);
    }

    private boolean hasTimedOut(final long startTime) {
        if (System.currentTimeMillis() - startTime > attrWriteMaxTime) {
            logger.debug("Timed out while persisting sync attribute to DPS");
            return true;
        } else {
            return false;
        }
    }

}
