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

package com.ericsson.nms.mediation.component.dps.operators.dps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.anyMapOf;
import static org.mockito.Mockito.anySetOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.RESTART_TIMESTAMP_ATTR_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.DEFAULT_DPS_WRITE_BATCH_SIZE;
import static com.googlecode.catchexception.CatchException.verifyException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.enm.mediation.handler.common.blacklist.BlacklistConfigurationStorage;
import com.ericsson.enm.mediation.handler.common.dps.util.DpsFacade;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.enm.mediation.handler.common.event.CmEventSender;
import com.ericsson.nms.mediation.component.dps.handlers.algorithms.TopologySyncAlgorithm;
import com.ericsson.nms.mediation.component.dps.handlers.config.DpsWriteConfigurationStorage;
import com.ericsson.nms.mediation.component.dps.test.util.TestUtil;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

//TODO add real data into tests
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TopologyWriteDpsOperator.class })
public class TopologyWriteDpsOperatorTest {

    private static final String SHM_FDN = "MeContext=LTE08ERBS00001,Inventory=1,LicenseInventory=1";
    private static final String PARENT_MO_NAME = "MeContext=Erbs01";
    private static final String BLACK_LIST_MO = "blacklistMO";
    private static final String BLACK_FDN_IN_DPS = PARENT_MO_NAME + "," + BLACK_LIST_MO + "=1";
    private static final String TEST_MO_FDN = PARENT_MO_NAME + ",TestMo=1";
    private static final String TEST_MO_SCHEMA = "dps_primarytype";
    private static final String TEST_MO_NAMESPACE = "test_namespace";
    private static final String TEST_MO_NAME = "test_name";
    private static final String TEST_MO_VERSION = "1.0.0";

    private Map<String, ModelInfo> eventPayload;

    @Mock
    private TopologySyncAlgorithm mockTopologySyncAlgorithm;
    @Mock
    private SystemRecorder systemRecorder;
    @Mock
    private ComponentEvent mockEvent;
    @Mock
    private ManagedObject mockRootMo;
    @Mock
    private ManagedObject mockShmMo;
    @Mock
    private ManagedObject mockCppCiMo;
    @Mock
    private DataBucket liveBucket;
    @Mock
    private BlacklistConfigurationStorage blackListStorage;
    @Mock
    private DpsFacade dpsFacade;
    @Mock
    private ManagedObject managedObject;
    @Mock
    private CmEventSender cmEventSender;
    @Mock
    private DpsWriteConfigurationStorage dpsWriteConfigurationStorage;
    @InjectMocks
    private TopologyWriteTxDpsOperator topologyTxOperator;
    @InjectMocks
    private TopologyWriteDpsOperator topologyWriteDpsOperator;
    private TopologyWriteTxDpsOperator topologyWriteTxDpsOperatorSpy;

    private int mosAddedToDps = 1;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        topologyWriteTxDpsOperatorSpy = Mockito.spy(topologyTxOperator);
        final Map<String, Object> headers = new HashMap<>();
        headers.put(MiscellaneousConstants.NODE_TYPE_HEADER_NAME, TEST_MO_NAME);
        headers.put(MiscellaneousConstants.OSS_MODEL_IDENTITY_HEADER_NAME, TEST_MO_VERSION);
        eventPayload = new HashMap<>();
        eventPayload.put(TEST_MO_FDN, new ModelInfo(TEST_MO_SCHEMA, TEST_MO_NAMESPACE, TEST_MO_NAME, TEST_MO_VERSION));
        when(mockEvent.getPayload()).thenReturn(eventPayload);
        when(mockEvent.getHeaders()).thenReturn(headers);
        when(mockRootMo.getName()).thenReturn(PARENT_MO_NAME);

        TestUtil.setFieldValue("topologyTxOperator", topologyWriteTxDpsOperatorSpy, TopologyWriteDpsOperator.class, topologyWriteDpsOperator);
        final List<String> blackListMos = Collections.singletonList(BLACK_LIST_MO);
        when(blackListStorage.getBlacklistMoForSync(any(String.class), any(String.class))).thenReturn(blackListMos);
        when(dpsFacade.getQueryBuilder()).thenReturn(new QueryBuilderMock());
        final Iterator<Object> iterator = new Iterator<Object>() {
            int iterCounter;

            @Override
            public void remove() {}

            @Override
            public ManagedObject next() {
                iterCounter++;
                return managedObject;
            }

            @Override
            public boolean hasNext() {
                return iterCounter < mosAddedToDps;
            }
        };
        when(dpsFacade.executeQuery(Matchers.any(Query.class))).thenReturn(iterator);
        when(dpsFacade.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(any(String.class))).thenReturn(managedObject);
        when(liveBucket.deletePo(any(PersistenceObject.class))).thenReturn(1);
        when(managedObject.getFdn()).thenReturn(BLACK_FDN_IN_DPS);
        when(mockCppCiMo.getFdn()).thenReturn(FdnUtil.getCppCiFdn(PARENT_MO_NAME));
        when(mockCppCiMo.getAttribute(RESTART_TIMESTAMP_ATTR_NAME)).thenReturn(null);
        when(liveBucket.findMoByFdn(FdnUtil.getCppCiFdn(PARENT_MO_NAME))).thenReturn(mockCppCiMo);
        when(dpsWriteConfigurationStorage.getTopDpsWriteBatchSize()).thenReturn(DEFAULT_DPS_WRITE_BATCH_SIZE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeDpsOperation() {
        final int totalElements = topologyWriteDpsOperator.persistTopology(mockEvent, PARENT_MO_NAME, Boolean.FALSE);
        assertEquals(eventPayload.size(), totalElements);
        verify(topologyWriteTxDpsOperatorSpy).invokeWriteMosToDps(any(Map.class), any(List.class), any(String.class), anyBoolean());
        verify(blackListStorage).getBlacklistMoForSync(TEST_MO_VERSION, TEST_MO_NAME);
        verify(topologyWriteTxDpsOperatorSpy).retrieveDeltaTopology(any(String.class), any(Map.class), any(Set.class), any(List.class));
    }

    @Test
    public void testInvokeDpsOperation_EventIsNull() {
        verifyException(topologyWriteDpsOperator, NullPointerException.class).persistTopology(null, PARENT_MO_NAME, Boolean.FALSE);
    }

    @Test
    public void testInvokeDpsOperation_EventIsNull_ForLargeNode() {
        verifyException(topologyWriteDpsOperator, NullPointerException.class).persistTopology(null, PARENT_MO_NAME, Boolean.FALSE);
    }

    @Test
    public void testShmInventoryMosNotReadDuringReSync() {
        final Collection<ManagedObject> childrenWithShmMo = new ArrayList<>();
        when(mockShmMo.getFdn()).thenReturn(SHM_FDN);
        childrenWithShmMo.add(mockShmMo);

        when(mockRootMo.getChildren()).thenReturn(childrenWithShmMo);
        topologyWriteDpsOperator.persistTopology(mockEvent, PARENT_MO_NAME, Boolean.FALSE);
        verify(mockShmMo, Mockito.never()).getChildren();
    }

    @Test
    public void testMoThatIsBlackListedWillBeRemoved() {
        eventPayload.put(BLACK_FDN_IN_DPS, new ModelInfo(TEST_MO_SCHEMA, TEST_MO_NAMESPACE, TEST_MO_NAME, TEST_MO_VERSION));
        topologyWriteDpsOperator.persistTopology(mockEvent, PARENT_MO_NAME, Boolean.FALSE);
        // Verifies that the blacklisted MOs are removed from the payload
        Assert.assertFalse(eventPayload.containsKey(BLACK_FDN_IN_DPS));
        // Verifies that the blacklisted MO was deleted from DPS
        verify(liveBucket).deletePo(any(PersistenceObject.class));
    }

    @Test
    public void testInvokeDpsOperationInBatches() {
        eventPayload.clear();
        final String payload = TEST_MO_FDN + ",Equipment=testEq";
        for (int i = 0; i < DEFAULT_DPS_WRITE_BATCH_SIZE + 100; i++) {
            eventPayload.put(payload + i, new ModelInfo(TEST_MO_SCHEMA, TEST_MO_NAMESPACE, TEST_MO_NAME, TEST_MO_VERSION));
        }
        final int numOfMosPersisted = topologyWriteDpsOperator.persistTopology(mockEvent, PARENT_MO_NAME, Boolean.FALSE);

        assertEquals(numOfMosPersisted, DEFAULT_DPS_WRITE_BATCH_SIZE + 100);
        verify(topologyWriteTxDpsOperatorSpy, times(2)).invokeWriteMosToDps(anyMapOf(String.class, ModelInfo.class),
                anyListOf(String.class), any(String.class), anyBoolean());
        verify(blackListStorage).getBlacklistMoForSync(TEST_MO_VERSION, TEST_MO_NAME);
        verify(topologyWriteTxDpsOperatorSpy).retrieveDeltaTopology(any(String.class), anyMapOf(String.class, ModelInfo.class),
                anySetOf(String.class), anyListOf(String.class));
    }

    @Test
    public void dpsDeleteMOsWithBatchProcessing() {
        //Setup
        eventPayload.clear();
        mosAddedToDps = DEFAULT_DPS_WRITE_BATCH_SIZE * 2 + 1000;

        //Execute
        topologyWriteDpsOperator.persistTopology(mockEvent, PARENT_MO_NAME, Boolean.TRUE);

        //Assertion
        verify(liveBucket, times(mosAddedToDps)).findMoByFdn(any(String.class));
        verify(liveBucket, times(mosAddedToDps)).deletePo(any(PersistenceObject.class));

        verify(dpsFacade, times(3)).getLiveBucket();
        verify(topologyWriteTxDpsOperatorSpy, times(3)).deleteMosFromDps(anyListOf(String.class));
    }

    @Test
    public void dpsDeleteMOsWhenPayloadSizeLessThanBatchSize() {
        //Setup
        eventPayload.clear();
        mosAddedToDps = 1000;

        //Execute
        topologyWriteDpsOperator.persistTopology(mockEvent, PARENT_MO_NAME, Boolean.TRUE);

        //Assertion
        verify(dpsFacade).getLiveBucket();
        verify(liveBucket, times(mosAddedToDps)).findMoByFdn(any(String.class));
        verify(liveBucket, times(mosAddedToDps)).deletePo(any(PersistenceObject.class));

        verify(topologyWriteTxDpsOperatorSpy).deleteMosFromDps(anyListOf(String.class));
    }

    @Test
    public void dpsDeleteMOsWhenPayloadSizeAndBatchSizeAreEqual() {
        //Setup
        eventPayload.clear();
        mosAddedToDps = 3000;

        //Execute
        topologyWriteDpsOperator.persistTopology(mockEvent, PARENT_MO_NAME, Boolean.TRUE);

        //Assertion
        verify(dpsFacade).getLiveBucket();
        verify(liveBucket, times(mosAddedToDps)).findMoByFdn(any(String.class));
        verify(liveBucket, times(mosAddedToDps)).deletePo(any(PersistenceObject.class));

        verify(topologyWriteTxDpsOperatorSpy).deleteMosFromDps(anyListOf(String.class));
    }
}
