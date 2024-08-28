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

package com.ericsson.nms.mediation.component.dps.handlers.config;

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.ATTRIBUTES_DPS_WRITE_MAX_TIME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.TOPOLOGY_DPS_WRITE_BATCH_SIZE;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.nms.mediation.component.dps.handlers.algorithms.AttributeSyncAlgorithm;
import com.ericsson.nms.mediation.component.dps.operators.dps.TopologyWriteDpsOperator;
import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

/**
 * Listens for batch size for topology and write time for attribute Dps write.
 */

@ApplicationScoped
public class DpsWriteConfigurationStorage {

    private static final Logger logger = LoggerFactory.getLogger(DpsWriteConfigurationStorage.class);

    @Inject
    private TopologyWriteDpsOperator topologyWriter;
    @Inject
    private AttributeSyncAlgorithm attributeSyncAlgorithm;

    @Inject
    @Configured(propertyName = TOPOLOGY_DPS_WRITE_BATCH_SIZE)
    private int topDpsWriteBatchSize;

    @Inject
    @Configured(propertyName = ATTRIBUTES_DPS_WRITE_MAX_TIME)
    private long attrDpsWriteMaxTime;

    public int getTopDpsWriteBatchSize() {
        return topDpsWriteBatchSize;
    }

    public long getAttrDpsWriteMaxTime() {
        return attrDpsWriteMaxTime;
    }

    public void listenForTopDpsWriteBatchSizeChange(@Observes @ConfigurationChangeNotification(
            propertyName = TOPOLOGY_DPS_WRITE_BATCH_SIZE) final int newTopDpsWriteBatchSize) {
        final int oldTopDpsWriteBatchSize = topDpsWriteBatchSize;
        topDpsWriteBatchSize = newTopDpsWriteBatchSize;
        topologyWriter.setTopDpsWriteBatchSize(newTopDpsWriteBatchSize);
        logger.info("Changed the Config Param {} from [{}] to [{}].", TOPOLOGY_DPS_WRITE_BATCH_SIZE,
                oldTopDpsWriteBatchSize, newTopDpsWriteBatchSize);
    }

    public void listenForAttrDpsWriteMaxTimeChange(@Observes @ConfigurationChangeNotification(
            propertyName = ATTRIBUTES_DPS_WRITE_MAX_TIME) final long newAttrDpsWriteMaxTime) {
        final long oldAttrDpsWriteMaxTime = attrDpsWriteMaxTime;
        attrDpsWriteMaxTime = newAttrDpsWriteMaxTime;
        attributeSyncAlgorithm.setAttrWriteMaxTime(newAttrDpsWriteMaxTime);
        logger.info("Changed the Config Param {} from [{}] to [{}].", ATTRIBUTES_DPS_WRITE_MAX_TIME,
                oldAttrDpsWriteMaxTime, newAttrDpsWriteMaxTime);
    }
}
