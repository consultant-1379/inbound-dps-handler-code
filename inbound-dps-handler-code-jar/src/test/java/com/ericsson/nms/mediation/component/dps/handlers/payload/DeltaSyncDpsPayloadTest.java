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

package com.ericsson.nms.mediation.component.dps.handlers.payload;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;

@RunWith(MockitoJUnitRunner.class)
public class DeltaSyncDpsPayloadTest {

    private final Collection<NodeNotification> createsAndUpdateChanges = new ArrayList<NodeNotification>();
    private final Collection<NodeNotification> deleteChanges = new ArrayList<NodeNotification>();
    private final Long generationCounter = 0L;

    private DeltaSyncDpsPayload deltaSyncDpsPayload;

    @Before
    public void setUp() {
        deltaSyncDpsPayload = new DeltaSyncDpsPayload(createsAndUpdateChanges, deleteChanges, generationCounter);
    }

    @Test
    public void testSettersAndGetters() {
        deltaSyncDpsPayload.setCreateAndUpdateChanges(createsAndUpdateChanges);
        assertEquals(createsAndUpdateChanges, deltaSyncDpsPayload.getCreateAndUpdateChanges());

        deltaSyncDpsPayload.setDeleteChanges(deleteChanges);
        assertEquals(deleteChanges, deltaSyncDpsPayload.getDeleteChanges());

        deltaSyncDpsPayload.setGenerationCounter(generationCounter);
        assertEquals(generationCounter, deltaSyncDpsPayload.getGenerationCounter());
    }

}
