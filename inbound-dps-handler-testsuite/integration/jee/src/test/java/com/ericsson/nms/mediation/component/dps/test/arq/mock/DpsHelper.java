/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.test.arq.mock;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.SYNC_STATUS_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.TOPOLOGY_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;

import java.util.Iterator;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.BucketProperties;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;

@Stateless
public class DpsHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DpsHelper.class);
    private static final String GENERATION_COUNTER_HEADER_NAME = "generationCounter";

    @EJB(lookup = DataPersistenceService.JNDI_LOOKUP_NAME)
    private DataPersistenceService dataPersistenceService;

    public void cleanupTypes() throws Exception {
        LOGGER.debug("delete data from versant");
        this.deleteTypeFromDatabase("OSS_TOP", "MeContext");
        this.deleteTypeFromDatabase("OSS_NE_DEF", "NetworkElement");
    }

    protected void deleteTypeFromDatabase(final String namespace, final String type) {
        deleteTypeFromDatabase(namespace, type,
                dataPersistenceService.getDataBucket("Live", BucketProperties.SUPPRESS_MEDIATION, BucketProperties.SUPPRESS_CONSTRAINTS));
    }

    protected void deleteTypeFromDatabase(final String namespace, final String type, final DataBucket bucket) {
        LOGGER.debug("Deleting objects of type {} in namespace {} from {} bucket", type, namespace, bucket.getName());
        final Query<TypeRestrictionBuilder> typeQuery = dataPersistenceService.getQueryBuilder().createTypeQuery(namespace, type);
        final Iterator<PersistenceObject> iterator = bucket.getQueryExecutor().execute(typeQuery);
        while (iterator.hasNext()) {
            final PersistenceObject po = iterator.next();
            safeDeletePo(bucket, po);
        }
    }

    private void safeDeletePo(final DataBucket bucket, final PersistenceObject persistenceObject) {
        // only delete topmost mos otherwise MOs will get deleted twice ..
        final boolean doDelete = isPersistenceObjectOrTopRootMo(persistenceObject);
        if (doDelete) {
            logDeletion(persistenceObject);
            LOGGER.debug("VERSION OF PO IS: {}", persistenceObject.getVersion());
            bucket.deletePo(persistenceObject);
        }
    }

    private void logDeletion(final PersistenceObject persistenceObject) {
        if (persistenceObject instanceof ManagedObject) {
            LOGGER.debug("Deleting MO: {}", ((ManagedObject) persistenceObject).getFdn());
        } else {
            LOGGER.debug("Deleting PO ({}) of type: {}", persistenceObject.getPoId(), persistenceObject.getType());
        }
    }

    private boolean isPersistenceObjectOrTopRootMo(final PersistenceObject persistenceObject) {
        boolean meetsCriteria = true;
        if (persistenceObject instanceof ManagedObject) {
            final ManagedObject managedObject = (ManagedObject) persistenceObject;
            LOGGER.debug("Checking if MO {} is a topmost root", managedObject.getFdn());
            meetsCriteria = managedObject.getParent() == null;
        }
        return meetsCriteria;
    }

    public String getNodeSyncStatus(final String nodefdn) {
        final ManagedObject root = dataPersistenceService.getLiveBucket().findMoByFdn(nodefdn);
        final String syncStatus = root.getAttribute(SYNC_STATUS_ATTR_NAME);
        LOGGER.debug("Got sync status value of {} from {} ", syncStatus, nodefdn);
        return syncStatus;
    }

    public String getParentFdn(final String moFdn) {
        final ManagedObject mo = dataPersistenceService.getLiveBucket().findMoByFdn(moFdn);
        final String parentFdn = mo.getParent().getFdn();
        LOGGER.debug("MO FDN  : {} has Parent FDN :{} ", moFdn, parentFdn);
        return parentFdn;
    }

    public int getNoOfChildren(final String moFdn) {
        final ManagedObject mo = dataPersistenceService.getLiveBucket().findMoByFdn(moFdn);
        final int noOfChildren = mo.getChildren().size();
        LOGGER.debug("MO FDN  : {} has {}  children", moFdn, noOfChildren);
        return noOfChildren;
    }

    public String getAttribute(final String moFdn, final String attribute) {
        final ManagedObject mo = dataPersistenceService.getLiveBucket().findMoByFdn(moFdn);
        final String attrValue = mo.getAttribute(attribute);
        LOGGER.debug("MO {} has attribute : {} and has value of {}", moFdn, attribute, attrValue);
        return attrValue;
    }

    public long getGeneratinCounterValue(final String moFdn) {
        final String attribute = GENERATION_COUNTER_HEADER_NAME;
        LOGGER.debug("MOFdn is {} and Attribute is {}", moFdn, attribute);
        final ManagedObject mo = dataPersistenceService.getLiveBucket().findMoByFdn(moFdn);
        final long gcValue = mo.getAttribute(attribute);
        LOGGER.debug("MO {} has attribute : {} and has value of {}", moFdn, attribute, gcValue);
        return gcValue;
    }

    public ManagedObject getMo(final String moFdn) {
        return dataPersistenceService.getLiveBucket().findMoByFdn(moFdn);
    }

    public void resetSyncStatusToUnsynchronized(final String cmFunctionLte001) {
        final ManagedObject mo = dataPersistenceService.getLiveBucket().findMoByFdn(cmFunctionLte001);
        mo.setAttribute(SYNC_STATUS_ATTR_NAME, UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
    }

    public void resetSyncStatusToTopology(final String cmFunctionLte001) {
        final ManagedObject mo = dataPersistenceService.getLiveBucket().findMoByFdn(cmFunctionLte001);
        mo.setAttribute(SYNC_STATUS_ATTR_NAME, TOPOLOGY_SYNC_STATUS_ATTR_VALUE);
    }

    public void setNetworkElementType(final String nodeName, final String neType) {
        dataPersistenceService.getLiveBucket().findMoByFdn("NetworkElement=" + nodeName).setAttribute("neType", neType);
    }
}
