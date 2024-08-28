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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.enm.mediation.handler.common.dps.util.DpsFacade;
import com.ericsson.nms.mediation.component.dps.handlers.algorithms.TopologySyncAlgorithm;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ TopologyWriteTxDpsOperatorTest.class })
public class TopologyWriteTxDpsOperatorTest {

    private static final String MANAGED_ELEMENT_RDN = "ManagedElement=1";
    private static final String ME_CONTEXT_FDN = "MeContext=Erbs01";

    private static final String MANAGED_ELEMENT_FDN = ME_CONTEXT_FDN + "," + MANAGED_ELEMENT_RDN;

    @Mock
    private TopologySyncAlgorithm topologySyncAlgorithm;
    @Mock
    private DpsFacade dpsFacade;
    @Mock
    private ManagedObject managedObject;
    @InjectMocks
    private TopologyWriteTxDpsOperator topologyWriteTxDpsOperator;

    private final Map<String, ModelInfo> payloadMock = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        when(dpsFacade.getQueryBuilder()).thenReturn(new QueryBuilderMock());
        final Iterator<Object> iterator = new Iterator<Object>() {
            List<ManagedObject> mos = Arrays.asList(managedObject);
            int iterCounter;

            @Override
            public void remove() {
            }

            @Override
            public ManagedObject next() {
                iterCounter++;
                return mos.get(0);
            }

            @Override
            public boolean hasNext() {
                return mos.size() != iterCounter;
            }
        };
        when(dpsFacade.executeQuery(Matchers.any(Query.class))).thenReturn(iterator);
        when(managedObject.getFdn()).thenReturn(MANAGED_ELEMENT_FDN);
    }

    /**
     * Tests the case when MO is present in DPS but not available in the node.
     */
    @Test
    public void testWhenMoNotPresentInpayload() {
        final Set<String> fdnsToBePersisted = new HashSet<>();
        final List<String> fdnsToBeDeleted = new ArrayList<>();
        topologyWriteTxDpsOperator.retrieveDeltaTopology(MANAGED_ELEMENT_FDN, payloadMock, fdnsToBePersisted, fdnsToBeDeleted);
        assertEquals(0, fdnsToBePersisted.size());
        assertArrayEquals(Arrays.asList(managedObject.getFdn()).toArray(), fdnsToBeDeleted.toArray());
    }

    /**
     * Tests the case when MO is present in DPS and also in the node.
     */
    @Test
    public void testWhenMoPresentInpayload() {
        final Set<String> fdnsToBePersisted = new HashSet<>();
        final List<String> fdnsToBeDeleted = new ArrayList<>();
        payloadMock.put(managedObject.getFdn(), null);
        topologyWriteTxDpsOperator.retrieveDeltaTopology(MANAGED_ELEMENT_FDN, payloadMock, fdnsToBePersisted, fdnsToBeDeleted);
        assertEquals(0, fdnsToBePersisted.size());
        assertEquals(0, fdnsToBeDeleted.size());
    }

}
