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

package com.ericsson.nms.mediation.component.dps.operators.dps;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.RESTART_TIMESTAMP_ATTR_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.SHM_INVENTORY_MO_WITH_EQUALS;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.util.DpsFacade;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.handlers.algorithms.TopologySyncAlgorithm;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;

/**
 * Class responsible to perform operations on DPS.
 */
@Stateless
@TransactionAttribute(REQUIRES_NEW)
public class TopologyWriteTxDpsOperator {

    private static final Logger logger = LoggerFactory.getLogger(TopologyWriteTxDpsOperator.class);

    @Inject
    private TopologySyncAlgorithm topologySyncAlgorithm;
    @Inject
    private DpsFacade dpsFacade;

    /**
     * Write the topology.
     *
     * @param topologyMapping
     *            The original mapping of complete topology
     * @param fdnsToProcess
     *            The list of topology FDNs need to be written to DPS.
     * @param rootFdn
     *            The root of the topology mapping.
     * @return Number of MOs written to DPS
     */
    public int invokeWriteMosToDps(final Map<String, ModelInfo> topologyMapping, final List<String> fdnsToProcess, final String rootFdn,
            final boolean isFirstSync) {
        logger.debug("Starting topology write of size: {} to DPS for FDN: '{}'", fdnsToProcess.size(), rootFdn);
        return topologySyncAlgorithm.storeTopologyInDps(topologyMapping, fdnsToProcess, rootFdn, isFirstSync);
    }

    /**
     * Deletes an MO from DPS including all of its descendants.
     *
     * @param fdnsToBeDeleted
     *            The FDN of the ManagedObject to deleted from DPS
     * @return The number of ManagedObjects deleted from DPS
     */
    public int deleteMosFromDps(final List<String> fdnsToBeDeleted) {
        int numberOfDeletedMos = 0;
        final DataBucket liveBucket = dpsFacade.getLiveBucket();

        for (final String fdnToBeDeleted : fdnsToBeDeleted) {
            final ManagedObject managedObject = liveBucket.findMoByFdn(fdnToBeDeleted);
            if (managedObject != null) {
                numberOfDeletedMos += liveBucket.deletePo(managedObject);
                logger.debug("{} MO(s) has been deleted from DPS including FDN: {}", numberOfDeletedMos, managedObject.getFdn());
            } else {
                logger.debug("MO: {} was not present in DPS", fdnToBeDeleted);
            }
        }
        return numberOfDeletedMos;
    }

    /**
     * Iterates the complete topology tree beneath the parent ManagedObject defined by <code>rootMoFdn</code>
     * and finds out delta change between DPS data and payload data.
     *
     * @param rootMoFdn
     *            FDN of the parent MO
     * @param payload
     *            The payload data obtained from the node
     * @param fdnsToBePersisted
     *            FDNs to be persisted in DPS i.e FDNs present in payload but not in DPS
     * @param fdnsToBeDeleted
     *            FDNs to be deleted from DPS i.e FDNs present in DPS but not in payload
     * @return The total mo count in DPS under the parent ManagedObject defined by <code>rootMoFdn</code>
     */
    public int retrieveDeltaTopology(final String rootMoFdn,
                                     final Map<String, ModelInfo> payload,
                                     final Set<String> fdnsToBePersisted,
                                     final List<String> fdnsToBeDeleted) {
        int preSyncTopologySize = 0;
        final Iterator<ManagedObject> iter = getContainmentQueryIterator(rootMoFdn);

        fdnsToBePersisted.addAll(payload.keySet());

        while (iter.hasNext()) {
            final ManagedObject mo = iter.next();
            preSyncTopologySize++;
            final String moFdn = mo.getFdn();
            fdnsToBePersisted.remove(moFdn);
            if (!payload.containsKey(moFdn) && !moFdn.contains(SHM_INVENTORY_MO_WITH_EQUALS)) {
                fdnsToBeDeleted.add(moFdn);
            }
        }
        logger.debug("Presync topology : {}, FDNs to be persisted : {}, FDNs to be deleted : {}", preSyncTopologySize, fdnsToBePersisted.size(),
                fdnsToBeDeleted.size());
        return preSyncTopologySize;
    }

    private Iterator<ManagedObject> getContainmentQueryIterator(final String rootMoFdn) {
        final QueryBuilder qb = dpsFacade.getQueryBuilder();
        final Query<ContainmentRestrictionBuilder> q = qb.createContainmentQuery(rootMoFdn);
        return dpsFacade.executeQuery(q);
    }
    /**
     * Checks if sync is executed for first time.
     *
     * @param rootMoFdn
     *            The root fdn.
     * @return Boolean value if true or not.
     */
    public boolean checkIsFirstSync(final String rootMoFdn) {
        final String cppCiFdn = FdnUtil.getCppCiFdn(rootMoFdn);
        final ManagedObject mo = dpsFacade.getLiveBucket().findMoByFdn(cppCiFdn);
        final Date restartTime = mo.getAttribute(RESTART_TIMESTAMP_ATTR_NAME);
        logger.info("Checking the cm function status P; {}", restartTime);
        return restartTime == null;
    }
}
