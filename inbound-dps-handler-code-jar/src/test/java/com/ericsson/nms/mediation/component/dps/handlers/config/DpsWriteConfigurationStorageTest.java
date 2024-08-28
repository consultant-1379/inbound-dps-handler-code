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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.nms.mediation.component.dps.handlers.algorithms.AttributeSyncAlgorithm;
import com.ericsson.nms.mediation.component.dps.operators.dps.TopologyWriteDpsOperator;

@RunWith(MockitoJUnitRunner.class)
public class DpsWriteConfigurationStorageTest {

    private static final int TOP_DPS_WRITE_BATCH_SIZE = 10;
    private static final long ATTR_DPS_WRITE_MAX_TIME = 4 * 60;

    @Mock
    private TopologyWriteDpsOperator topologyWriter;
    @Mock
    private AttributeSyncAlgorithm attributeSyncAlgorithm;

    @InjectMocks
    private DpsWriteConfigurationStorage dpsWriteConfigurationStorage;

    @Test
    public void testListenForTopDpsWriteBatchSizeChange() {
        dpsWriteConfigurationStorage.listenForTopDpsWriteBatchSizeChange(TOP_DPS_WRITE_BATCH_SIZE);
        assertEquals(TOP_DPS_WRITE_BATCH_SIZE, dpsWriteConfigurationStorage.getTopDpsWriteBatchSize());
        verify(topologyWriter).setTopDpsWriteBatchSize(TOP_DPS_WRITE_BATCH_SIZE);
    }

    @Test
    public void testListenForAttrDpsWriteMaxTimeChange() {
        dpsWriteConfigurationStorage.listenForAttrDpsWriteMaxTimeChange(ATTR_DPS_WRITE_MAX_TIME);
        assertEquals(ATTR_DPS_WRITE_MAX_TIME, dpsWriteConfigurationStorage.getAttrDpsWriteMaxTime());
        verify(attributeSyncAlgorithm).setAttrWriteMaxTime(ATTR_DPS_WRITE_MAX_TIME);
    }

}
