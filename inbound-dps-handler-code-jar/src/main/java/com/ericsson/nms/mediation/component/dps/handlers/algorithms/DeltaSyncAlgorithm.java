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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.enm.mediation.handler.common.event.CmEventSender;
import com.ericsson.nms.mediation.component.dps.handlers.exception.InternalModelException;
import com.ericsson.nms.mediation.component.dps.operators.model.IntegrityModelOperator;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.itpf.modeling.schema.util.SchemaConstants;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.ericsson.oss.mediation.network.api.notifications.NotificationType;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

/**
 * Delta sync changes processing in DPS.
 */
@RequestScoped
public class DeltaSyncAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(DeltaSyncAlgorithm.class);

    @Inject
    private IntegrityModelOperator integrityModelOperator;
    @Inject
    private CmEventSender cmEventSender;

    private final Map<String, ManagedObject> retrievedMos = new HashMap<String, ManagedObject>();

    /**
     * Processes delete changes, removing related MOs from DPS if they exist.
     *
     * @param changes
     *            list of delete changes to execute
     * @param liveBucket
     *            DPS live bucket
     * @param meContext
     *            Processed node MeContext
     */
    public void processDeleteChanges(final Collection<NodeNotification> changes, final DataBucket liveBucket, final String meContext) {
        for (final NodeNotification change : changes) {
            final String fdnToProcess = FdnUtil.prependMeContext(meContext, change.getFdn());
            logger.trace("Processing FDN: '{}' for delete", fdnToProcess);

            final ManagedObject moToDelete = liveBucket.findMoByFdn(fdnToProcess);

            if (moToDelete != null) {
                liveBucket.deletePo(moToDelete);
                sendEventForDelete(fdnToProcess, change);
                logger.trace("FDN: '{}' deleted from DPS", fdnToProcess);
            }
        }
    }

    private void sendEventForDelete(final String fdn, final NodeNotification nodeNotification) {
        nodeNotification.setFdn(fdn);
        nodeNotification.setCreationTimestamp(new Date());
        cmEventSender.sendEvent(nodeNotification);
    }

    private void sendEventForUpdate(final String fdn, final NodeNotification nodeNotification, final Map<String, Object> currentValuesFromDps) {
        nodeNotification.setFdn(fdn);
        nodeNotification.setCreationTimestamp(new Date());
        final NotificationType notificationType = nodeNotification.getAction();
        if (notificationType.equals(NotificationType.UPDATE) || notificationType.equals(NotificationType.SEQUENCE_DELTA)) {
            getChangedAttributes(nodeNotification, currentValuesFromDps);
        }
        if (!nodeNotification.getUpdateAttributes().isEmpty()) {
            cmEventSender.sendEvent(nodeNotification);
        }
    }

    /**
     * Performs create and update operations in DPS. It assumes that changes list is ordered by FDN
     *
     * @param changes
     *            create and update changes information
     * @param liveBucket
     *            DPS live bucket
     * @param meContext
     *            MeContext for the processing node
     */
    public void processCreateUpdateChanges(final Collection<NodeNotification> changes, final DataBucket liveBucket, final String meContext) {
        final Set<String> notModelledFdns = new HashSet<String>();
        for (final NodeNotification change : changes) {
            final String changeFdn = change.getFdn();
            try {
                logger.trace("Processing FDN: '{}' for {}", changeFdn, change.getAction());
                final String fdnToProcess = FdnUtil.prependMeContext(meContext, changeFdn);
                change.setFdn(fdnToProcess);

                if (!isMoContainedInNotModelledMos(changeFdn, notModelledFdns)) {
                    continue;
                }
                final ManagedObject moToChange = retrieveMoForChange(change, liveBucket);

                if (change.getUpdateAttributes() != null) {
                    final Map<String, Object> currentValuesFromDps = moToChange.getAllAttributes();
                    moToChange.setAttributes(change.getUpdateAttributes());
                    sendEventForUpdate(fdnToProcess, change, currentValuesFromDps);
                }
            } catch (final InternalModelException internalModelException) {
                logger.warn("Failed to process FDN: '{}'. Exception message: '{}'.", changeFdn, internalModelException.getMessage());
                logger.trace("Adding '{}' to an internal buffer contraining not modelled MOs.", changeFdn);
                notModelledFdns.add(changeFdn);
            }
        }
    }

    void getChangedAttributes(final NodeNotification nodeNotification, final Map<String, Object> currentValuesFromDps) {
        final Map<String, Object> currentValuesFromNode = nodeNotification.getUpdateAttributes();
        final MapDifference<String, Object> difference = Maps.difference(currentValuesFromDps, currentValuesFromNode);
        final Map<String, MapDifference.ValueDifference<Object>> entriesDiffering = difference.entriesDiffering();

        final Map<String, Object> changedAttributes = new HashMap<>();
        for (final String key : entriesDiffering.keySet()) {
            changedAttributes.put(key, currentValuesFromNode.get(key));
        }
        nodeNotification.setUpdateAttributes(changedAttributes);
    }

    private boolean isMoContainedInNotModelledMos(final String fdn, final Set<String> notModelledFdns) {
        for (final String notModelledFdn : notModelledFdns) {
            if (fdn.contains(notModelledFdn)) {
                logger.warn("'{}' MO is not modelled.", notModelledFdn);
                return false;
            }
        }

        return true;
    }

    private ManagedObject retrieveMoForChange(final NodeNotification change, final DataBucket liveBucket) throws InternalModelException {
        final ManagedObject existingMo = getManagedObject(change.getFdn(), liveBucket);

        if (existingMo != null) {
            return existingMo;
        } else {
            return createMo(change, liveBucket);
        }
    }

    private ManagedObject createMo(final NodeNotification change, final DataBucket liveBucket) throws InternalModelException {
        final String fdnToProcess = change.getFdn();

        final String parentFdn = FdnUtil.extractParentFdn(fdnToProcess);
        ManagedObject parentMo = getManagedObject(parentFdn, liveBucket);
        logger.trace("Parent MO: '{}' for new FDN: '{}'", parentMo, fdnToProcess);
        final String parentMoType = FdnUtil.extractMoType(parentFdn);

        final String newMoName = FdnUtil.extractMoName(fdnToProcess);
        final String newMoType = change.getName();

        if (parentMo == null) {
            final ManagedObject managedElementMo = liveBucket.findMoByFdn(FdnUtil.extractManagedElementMo(fdnToProcess));
            logger.warn("Parent MO: '{}' for new fdn is not found in DPS, so will get root mo '{}'", parentMo, fdnToProcess);
            parentMo = managedElementMo;
        }

        final ModelInfo childModelInfo = new ModelInfo(SchemaConstants.DPS_PRIMARYTYPE, parentMo.getNamespace(),
                newMoType, parentMo.getVersion());
        final ModelInfo parentModelInfo = new ModelInfo(SchemaConstants.DPS_PRIMARYTYPE, parentMo.getNamespace(),
                parentMoType, parentMo.getVersion());

        if (!integrityModelOperator.isParentChildRelationshipAllowed(parentModelInfo, childModelInfo)) {
            final String errorMessage =
                    "Error creating child MO: '" + fdnToProcess + "', for parent MO: '" + parentMo.getFdn()
                            + "', child is not valid for the parent";
            throw new InternalModelException(errorMessage);
        }

        ManagedObject newMo;
        if (!change.getNameSpace().equals(parentMo.getNamespace())) {
            newMo = liveBucket.getMibRootBuilder().namespace(change.getNameSpace()).name(newMoName).type(newMoType).parent(parentMo)
                    .version(change.getVersion()).create();
        } else {
            newMo = liveBucket.getManagedObjectBuilder().name(newMoName).type(newMoType).parent(parentMo).create();
        }

        logger.trace("Created new MO: '{}' for FDN: '{}'", newMo, fdnToProcess);

        retrievedMos.put(fdnToProcess, newMo);
        return newMo;
    }

    private ManagedObject getManagedObject(final String fdn, final DataBucket liveBucket) {
        ManagedObject mo = retrievedMos.get(fdn);
        if (mo == null) {
            mo = liveBucket.findMoByFdn(fdn);
            if (mo != null) {
                retrievedMos.put(fdn, mo);
            }
        }
        logger.trace("Got MO '{}' for FDN: '{}'", mo, fdn);

        return mo;
    }

}
