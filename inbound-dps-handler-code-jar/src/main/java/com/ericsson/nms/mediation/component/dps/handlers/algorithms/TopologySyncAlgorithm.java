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

package com.ericsson.nms.mediation.component.dps.handlers.algorithms;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.util.DpsFacade;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.enm.mediation.handler.common.event.CmEventSender;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.handlers.exception.InternalModelException;
import com.ericsson.nms.mediation.component.dps.operators.model.IntegrityModelOperator;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.ericsson.oss.mediation.network.api.notifications.NotificationType;

/**
 * The algorithm that creates the topology in DPS for the node being synced.
 */
@RequestScoped
public class TopologySyncAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(TopologySyncAlgorithm.class);

    @Inject
    private DpsFacade dpsFacade;
    @Inject
    private CmEventSender cmEventSender;
    @Inject
    private IntegrityModelOperator integrityModelOperator;
    private DataBucket liveDataBucket;
    private Map<String, ModelInfo> topologyMapping;

    /**
     * Stores the topology in DPS.
     *
     * @param topologyMapping
     *            The original mapping of complete topology
     * @param fdnsToProcess
     *            The list of topology FDNs need to be written to DPS
     * @param rootFdn
     *            The root of the topology mapping.
     * @param isFirstSync
     *            First sync of the node.
     * @return Number of MOs written to DPS
     */
    public int storeTopologyInDps(final Map<String, ModelInfo> topologyMapping, final List<String> fdnsToProcess, final String rootFdn,
            final boolean isFirstSync) {
        this.topologyMapping = topologyMapping;
        this.liveDataBucket = dpsFacade.getLiveBucket();
        final ManagedObject moRoot = liveDataBucket.findMoByFdn(rootFdn);
        int numberOfMosWritten = 0;
        if (moRoot == null) {
            throw createInboundDpsHandlerException(null, "MO is unavailable, cannot continue to persist topology");
        }
        logger.trace("This is the ROOT: {} for FDN: {}", moRoot, rootFdn);
        final Map<String, ManagedObject> moTopologyMap = new HashMap<>();
        final Collection<Serializable> listOfNotifs = new LinkedList<>();
        ManagedObject iteratingParentMo = moRoot;
        for (final String fdnToProcess : fdnsToProcess) {
            try {
                iteratingParentMo = createChildMo(fdnToProcess, iteratingParentMo, moTopologyMap, rootFdn);
                moTopologyMap.put(fdnToProcess, iteratingParentMo);
                if (!isFirstSync) {
                    listOfNotifs.add(buildCreateNotification(fdnToProcess, iteratingParentMo.getAllAttributes()));
                }
                numberOfMosWritten++;
            } catch (final InternalModelException internalModelException) {
                logger.warn("Failed to process Fdn {}, exception {}", fdnToProcess, internalModelException.getMessage());
            }
        }
        logger.debug("Persisted {} Topology MOs for FDN '{}'", numberOfMosWritten, rootFdn);
        if (!isFirstSync) {
            cmEventSender.sendBatchEvents(listOfNotifs);
            logger.trace("Finished sending create notifications to NBI, total: {}", listOfNotifs.size());
        }
        return numberOfMosWritten;
    }

    protected ManagedObject createChildMoForParentMo(final ManagedObject parentMo, final String childFdn)
            throws InboundDpsHandlerException, InternalModelException {
        final ModelInfo childModelInfo = topologyMapping.get(childFdn);
        ManagedObject childMo = getChildMoFromParent(childFdn, childModelInfo, parentMo);
        try {
            logger.trace("FDN: '{}' is a child of MO: '{}'? {}", childFdn, parentMo.getFdn(), childMo != null);
            if (childMo == null) {
                logger.trace("Creating child MO: '{}'", childFdn);
                final ModelInfo parentModelInfo = getParentModelInfo(parentMo.getFdn());

                if (parentModelInfo != null && !integrityModelOperator.isParentChildRelationshipAllowed(parentModelInfo, childModelInfo)) {
                    final String errorMessage = "Error creating child MO: '" + childFdn + "' for parent MO: '" + parentMo.getFdn()
                            + "', child is not valid for parent.";
                    logger.error(errorMessage);

                    throw new InternalModelException(errorMessage);
                }
                childMo = createChildMoInDps(parentModelInfo, childModelInfo, parentMo, childFdn);
            }
        } catch (final InternalModelException internalModelException) {
            throw internalModelException;
        } catch (final Exception exception) {
            throw createInboundDpsHandlerException(exception, "Error creating child MO: '" + childFdn + "', for parent MO: '" + parentMo.getFdn()
                    + "'");
        }
        return childMo;
    }

    private InboundDpsHandlerException createInboundDpsHandlerException(final Exception exception, final String errorMessage) {
        logger.error(errorMessage, exception);
        if (exception != null) {
            return new InboundDpsHandlerException(errorMessage, exception);
        }
        return new InboundDpsHandlerException(errorMessage);
    }

    private ManagedObject getChildMoFromParent(final String childFdn, final ModelInfo childModelInfo, final ManagedObject parentMo) {
        final String childMoType = childModelInfo == null ? FdnUtil.extractMoType(childFdn) : childModelInfo.getName();
        final String childRdn = FdnUtil.extractRdn(childFdn, childMoType);
        return parentMo.getChild(childRdn);
    }

    private ManagedObject createChildMoWithParentMoFromTopologyMap(final String childFdn, final String meContextFdn,
                                                                   final Map<String, ManagedObject> moTopologyMap) throws InternalModelException {
        final String parentFdn = FdnUtil.extractParentFdn(childFdn, meContextFdn);
        final ManagedObject parentMoFromTopologyMap = moTopologyMap.get(parentFdn);
        if (parentMoFromTopologyMap != null) {
            logger.trace("Found parent MO in topology MO map: '{}'", parentMoFromTopologyMap.getFdn());
            return createChildMoForParentMo(parentMoFromTopologyMap, childFdn);
        }
        final ManagedObject parentFromDps = liveDataBucket.findMoByFdn(parentFdn);
        if (parentFromDps != null) {
            logger.trace("Found parent MO in DPS: '{}'", parentFromDps.getFdn());
            return createChildMoForParentMo(parentFromDps, childFdn);
        }
        final String parentMoNotFoundErrorMessage = "Could not find parent for FDN: " + childFdn;
        logger.error(parentMoNotFoundErrorMessage);
        throw new InboundDpsHandlerException(parentMoNotFoundErrorMessage);
    }

    private ManagedObject createChildMoInDps(final ModelInfo parentModelInfo, final ModelInfo childModelInfo, final ManagedObject parentMo,
                                             final String childFdn) {
        ManagedObject childMo = null;
        final String childMoName = FdnUtil.extractMoName(childFdn);
        final String childMoType = childModelInfo.getName();

        if (areParentAndChildMoNamespacesEqual(parentModelInfo, childModelInfo)) {
            logger.trace("Creating child MO as non-MIB root: '{}'...", childFdn);
            childMo = liveDataBucket.getManagedObjectBuilder().name(childMoName).type(childMoType).parent(parentMo).create();
        } else {
            logger.trace("Creating child MO as MIB root: '{}'...", childFdn);
            final String childMoVersion = childModelInfo.getVersion().toString();
            childMo = liveDataBucket.getMibRootBuilder().namespace(childModelInfo.getNamespace()).name(childMoName).type(childMoType).parent(parentMo)
                    .version(childMoVersion).create();
        }
        return childMo;
    }

    protected boolean areParentAndChildMoNamespacesEqual(final ModelInfo parentModelInfo, final ModelInfo childModelInfo) {
        if (parentModelInfo == null) {
            return false;
        }
        final String parentModelNamespace = parentModelInfo.getNamespace();
        final String childModelNamesapce = childModelInfo.getNamespace();
        return parentModelNamespace.equals(childModelNamesapce);
    }

    private ModelInfo getParentModelInfo(final String parentFdn) {
        ModelInfo parentModelInfo = null;
        if (!FdnUtil.isRootMo(parentFdn)) {
            parentModelInfo = topologyMapping.get(parentFdn);
            if (parentModelInfo == null) {
                logger.warn("Found no model info for FDN: '{}', trying without top parent.", parentFdn);
                parentModelInfo = topologyMapping.get(FdnUtil.extractFdnWithoutTopParent(parentFdn));
            }
            logger.trace("=> Parent Model Info: '{}'", parentModelInfo);
        }
        return parentModelInfo;
    }

    private ManagedObject createChildMo(final String childFdn, final ManagedObject parentMo, final Map<String, ManagedObject> moTopologyMap,
                                        final String meContextFdn) throws InternalModelException {
        final String parentFdn = parentMo.getFdn();
        if (FdnUtil.isParentOfFdn(parentMo, childFdn, meContextFdn)) {
            logger.trace("FDN: '{}' is child of: '{}'", childFdn, parentFdn);
            return createChildMoForParentMo(parentMo, childFdn);
        }
        logger.trace("'{}' is not parent of '{}', looking for parent in topology map", parentFdn, childFdn);
        return createChildMoWithParentMoFromTopologyMap(childFdn, meContextFdn, moTopologyMap);
    }

    private NodeNotification buildCreateNotification(final String fdn, final Map<String, Object> attributes) {
        final NodeNotification nodeNotification = new NodeNotification();
        nodeNotification.setFdn(fdn);
        nodeNotification.setCreationTimestamp(new Date());
        nodeNotification.setUpdateAttributes(attributes);
        nodeNotification.setAction(NotificationType.CREATE);
        return nodeNotification;
    }
}
