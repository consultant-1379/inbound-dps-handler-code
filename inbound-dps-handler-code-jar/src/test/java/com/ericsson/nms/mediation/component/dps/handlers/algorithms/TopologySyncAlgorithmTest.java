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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.googlecode.catchexception.CatchException.verifyException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.util.DpsFacade;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.enm.mediation.handler.common.event.CmEventSender;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.handlers.algorithms.stubs.ManagedObjectBuilderStub;
import com.ericsson.nms.mediation.component.dps.handlers.algorithms.stubs.MibRootBuilderStub;
import com.ericsson.nms.mediation.component.dps.operators.model.IntegrityModelOperator;
import com.ericsson.nms.mediation.component.dps.utility.ModelServiceAccessInstrumentationUtil;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.exception.general.AlreadyDefinedException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.ModelConstraintViolationException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;

@RunWith(MockitoJUnitRunner.class)
public class TopologySyncAlgorithmTest {

    private static final String EXCEPTION_TEST_TEXT = "Exception Text";
    private static final String MODEL_CONTRAINT_EXCEPTION_ENTITY_NAME = "EntityName";
    private static final String MODEL_CONTRAINT_EXCEPTION_ENTITY_VERSION = "Version";
    private static final String MODEL_CONTRAINT_EXCEPTION_ENTITY_TYPE = "Type";
    private static final String MODEL_CONTRAINT_EXCEPTION_ENTITY_NAMESPACE = "Namespace";
    private static final String MO_MODEL_VERSION_NEW = "3.12.0";
    private static final String MO_MODEL_VERSION_OLD = "3.1.72";
    private static final String CONNECTIVITY_INFO_MO_NAME = "ConnectivityInfo";
    private static final String EQUIPMENT_MO_NAME = "Equipment";
    private static final String ANTENNA_UNIT_GROUP_MO_NAME = "AntennaUnitGroup";
    private static final String ENODE_B_FUNCTION_MO_NAME = "ENodeBFunction";
    private static final String RBS_CONFIGURATION_MO_NAME = "RbsConfiguration";
    private static final String CHILD_OF_RBS_CONFIGURATION_MO_NAME = "ChildOfRbsConfiguration";
    private static final String CABINET_MO_NAME = "Cabinet";
    private static final String ERBS_NODE_MODEL_NAMESPACE = "ERBS_NODE_MODEL";
    private static final String CPP_NODE_MODEL_NAMESPACE = "CPP_NODE_MODEL";
    private static final String DPS_PRIMARYTYPE_SCHEMA_TYPE = "dps_primarytype";

    private static final String MANAGED_ELEMENT_MO_TYPE = "ManagedElement";
    private static final String CONNECTIVITY_INFO_MO_TYPE = CONNECTIVITY_INFO_MO_NAME;
    private static final String MANAGED_ELEMENT_RDN = "ManagedElement=1";
    private static final String ENODE_B_FUNCTION_RDN = "ENodeBFunction=1";
    private static final String CONNECTIVITY_INFO_RDN = "ConnectivityInfo=1";
    private static final String EQUIPEMENT_RDN = "Equipment=testEq";
    private static final String ANTENNA_UNIT_GROUP_RDN = "AntennaUnitGroup=1";
    private static final String CABINET_RDN = "Cabinet=1";
    private static final String ME_CONTEXT_FDN = "MeContext=Erbs01";
    private static final String RBS_CONFIGURATION_RDN = "RbsConfiguration=1";
    private static final String CHILD_OF_RBS_CONFIGURATION_RDN = "ChildOfRbsConfiguration=1";

    private static final String MANAGED_ELEMENT_FDN = ME_CONTEXT_FDN + "," + MANAGED_ELEMENT_RDN;
    private static final String ENODE_B_FUNCTION_NODE_FDN = MANAGED_ELEMENT_FDN + "," + ENODE_B_FUNCTION_RDN;
    private static final String CONNECTIVITY_INFO_NODE_FDN = MANAGED_ELEMENT_FDN + "," + CONNECTIVITY_INFO_RDN;
    private static final String EQUIPMENT_NODE_FDN = MANAGED_ELEMENT_FDN + "," + EQUIPEMENT_RDN;
    private static final String ANTENNA_UNIT_GROUP_NODE_FDN = EQUIPMENT_NODE_FDN + "," + ANTENNA_UNIT_GROUP_RDN;
    private static final String CABINET_NODE_FDN = EQUIPMENT_NODE_FDN + "," + CABINET_RDN;
    private static final String RBS_CONFIGURATION_FDN = ENODE_B_FUNCTION_NODE_FDN + "," + RBS_CONFIGURATION_RDN;
    private static final String CHILD_OF_RBS_CONFIGURATION_FDN = RBS_CONFIGURATION_FDN + ","
            + CHILD_OF_RBS_CONFIGURATION_RDN;
    private static final String EXCEPTION_MESSAGE = "Test exception!";

    @Mock
    private DpsFacade dpsFacade;
    @Mock
    private ModelInfo modelInfo;
    @Mock
    private IntegrityModelOperator integrityModelOperatorMock;
    @Mock
    private DataBucket mockDataBucket;
    @Mock
    private CmEventSender cmEventSender;
    @Mock
    private ModelServiceAccessInstrumentationUtil modelServiceAccessInstrumentationUtil;

    private ManagedObjectBuilderStub moBuilderStub;
    private MibRootBuilderStub mibRootBuilderStub;
    private ManagedObject rootMoStub;

    private Map<String, ModelInfo> resultTopologyMapping;
    private List<String> resultPreSyncTopology;

    private List<String> blackList;

    @InjectMocks
    private TopologySyncAlgorithm topologySyncAlgorithm;

    @Before
    public void setUp() throws Exception {
        resultTopologyMapping = new TreeMap<>();
        resultTopologyMapping.put(MANAGED_ELEMENT_FDN, new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, CPP_NODE_MODEL_NAMESPACE,
                MANAGED_ELEMENT_MO_TYPE, MO_MODEL_VERSION_NEW));
        resultTopologyMapping.put(CONNECTIVITY_INFO_NODE_FDN, new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE,
                ERBS_NODE_MODEL_NAMESPACE, CONNECTIVITY_INFO_MO_NAME, MO_MODEL_VERSION_OLD));
        resultTopologyMapping.put(EQUIPMENT_NODE_FDN, new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, ERBS_NODE_MODEL_NAMESPACE,
                EQUIPMENT_MO_NAME, MO_MODEL_VERSION_OLD));
        resultTopologyMapping.put(ENODE_B_FUNCTION_NODE_FDN, new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE,
                ERBS_NODE_MODEL_NAMESPACE, ENODE_B_FUNCTION_MO_NAME, MO_MODEL_VERSION_OLD));
        resultTopologyMapping.put(ANTENNA_UNIT_GROUP_NODE_FDN, new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE,
                ERBS_NODE_MODEL_NAMESPACE, ANTENNA_UNIT_GROUP_MO_NAME, MO_MODEL_VERSION_OLD));
        resultTopologyMapping.put(CABINET_NODE_FDN, new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, ERBS_NODE_MODEL_NAMESPACE,
                CABINET_MO_NAME, MO_MODEL_VERSION_OLD));
        resultTopologyMapping.put(RBS_CONFIGURATION_FDN, new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE,
                ERBS_NODE_MODEL_NAMESPACE, RBS_CONFIGURATION_MO_NAME, MO_MODEL_VERSION_OLD));
        resultTopologyMapping.put(CHILD_OF_RBS_CONFIGURATION_FDN, new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE,
                ERBS_NODE_MODEL_NAMESPACE, CHILD_OF_RBS_CONFIGURATION_MO_NAME, MO_MODEL_VERSION_OLD));
        resultPreSyncTopology = new ArrayList<>();
        resultPreSyncTopology.add(MANAGED_ELEMENT_FDN);
        resultPreSyncTopology.add(ANTENNA_UNIT_GROUP_NODE_FDN);

        blackList = new LinkedList<>();
        blackList.add("Cabinet");

        rootMoStub = createMockMeContext();

        when(integrityModelOperatorMock.isParentChildRelationshipAllowed(any(ModelInfo.class), any(ModelInfo.class)))
                .thenReturn(true);

        mibRootBuilderStub = new MibRootBuilderStub();
        when(mockDataBucket.getMibRootBuilder()).thenReturn(mibRootBuilderStub);
        moBuilderStub = new ManagedObjectBuilderStub();
        when(mockDataBucket.getManagedObjectBuilder()).thenReturn(moBuilderStub);
        when(mockDataBucket.findMoByFdn(Matchers.anyString())).thenReturn(rootMoStub);
        when(dpsFacade.getLiveBucket()).thenReturn(mockDataBucket);
    }

    @Test
    public void teststoreTopologyInDps() {
        topologySyncAlgorithm.storeTopologyInDps(resultTopologyMapping, new ArrayList<>(resultTopologyMapping.keySet()), ME_CONTEXT_FDN,
                Boolean.FALSE);
        assertEquals("mo builder count ", 4, moBuilderStub.getCreationCount());
        assertEquals("mib builder count ", 4, mibRootBuilderStub.getCreationCount());
        verify(cmEventSender).sendBatchEvents(any());
    }

    @Test
    public void teststoreTopologyInDpsNoNotifications() {
        topologySyncAlgorithm.storeTopologyInDps(resultTopologyMapping, new ArrayList<>(resultTopologyMapping.keySet()), ME_CONTEXT_FDN,
                Boolean.TRUE);
        assertEquals("mo builder count ", 4, moBuilderStub.getCreationCount());
        assertEquals("mib builder count ", 4, mibRootBuilderStub.getCreationCount());
        verify(cmEventSender, never()).sendBatchEvents(any());
    }

    @Test
    public void testParentAndChildMoNamespacesEqual() {
        assertFalse(topologySyncAlgorithm.areParentAndChildMoNamespacesEqual(null, new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, "namespace",
                "firstName", MO_MODEL_VERSION_NEW)));
        assertTrue(topologySyncAlgorithm.areParentAndChildMoNamespacesEqual(new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, "namespace", "firstName",
                MO_MODEL_VERSION_NEW), new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, "namespace", "secondName", MO_MODEL_VERSION_OLD)));
        assertFalse(topologySyncAlgorithm
                .areParentAndChildMoNamespacesEqual(new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, "firstNamespace", "firstName", MO_MODEL_VERSION_NEW),
                        new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, "secondNamespace", "secondName", MO_MODEL_VERSION_OLD)));
    }

    @Test
    public void testCreateChildForParent_CatchesException() {
        resultTopologyMapping.put(CONNECTIVITY_INFO_NODE_FDN, null);
        verifyException(topologySyncAlgorithm, InboundDpsHandlerException.class).storeTopologyInDps(resultTopologyMapping,
                new ArrayList<>(resultTopologyMapping.keySet()), ME_CONTEXT_FDN, Boolean.FALSE);
    }

    @Test
    public void testCreateChild_CatchesException() {
        when(mockDataBucket.findMoByFdn(Matchers.anyString())).thenReturn(null);
        verifyException(topologySyncAlgorithm, InboundDpsHandlerException.class).storeTopologyInDps(resultTopologyMapping,
                new ArrayList<>(resultTopologyMapping.keySet()), ME_CONTEXT_FDN, Boolean.FALSE);
    }

    @Test
    public void testCreateChildForParent_CatchesAlreadyDefinedException() {
        final ManagedObject childMo = mock(ManagedObject.class);
        final ModelInfo meModelInfo = new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, CPP_NODE_MODEL_NAMESPACE,
                MANAGED_ELEMENT_MO_TYPE, MO_MODEL_VERSION_NEW);
        prepareParentAndChildMosForExceptionTests(rootMoStub, childMo, MANAGED_ELEMENT_MO_TYPE, MANAGED_ELEMENT_FDN,
                MANAGED_ELEMENT_FDN, meModelInfo);

        when(mockDataBucket.getMibRootBuilder()).thenThrow(new DummyAlreadyDefinedException(EXCEPTION_MESSAGE));

        final int numberOfPersistedMos = topologySyncAlgorithm.storeTopologyInDps(resultTopologyMapping,
                new ArrayList<>(resultTopologyMapping.keySet()), ME_CONTEXT_FDN, Boolean.FALSE);
        assertEquals(1, numberOfPersistedMos);
        verify(rootMoStub).getChild(MANAGED_ELEMENT_RDN);
    }

    @Test
    public void testCreateChildForParent_CatchesModelConstraintViolationException() {
        final ManagedObject childMo = mock(ManagedObject.class);
        final String ciMappingFdn = ME_CONTEXT_FDN + "," + CONNECTIVITY_INFO_RDN;
        final ModelInfo ciModelInfo = new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, ERBS_NODE_MODEL_NAMESPACE,
                CONNECTIVITY_INFO_MO_NAME, MO_MODEL_VERSION_OLD);

        prepareParentAndChildMosForExceptionTests(rootMoStub, childMo, CONNECTIVITY_INFO_MO_TYPE,
                CONNECTIVITY_INFO_NODE_FDN, ciMappingFdn, ciModelInfo);

        when(mockDataBucket.getMibRootBuilder()).thenThrow(
                new ModelConstraintViolationException(MODEL_CONTRAINT_EXCEPTION_ENTITY_NAMESPACE,
                        MODEL_CONTRAINT_EXCEPTION_ENTITY_TYPE, MODEL_CONTRAINT_EXCEPTION_ENTITY_VERSION,
                        EXCEPTION_MESSAGE, MODEL_CONTRAINT_EXCEPTION_ENTITY_NAME, EXCEPTION_TEST_TEXT));
        final int numberOfPersistedMos = topologySyncAlgorithm.storeTopologyInDps(resultTopologyMapping,
                new ArrayList<>(resultTopologyMapping.keySet()), ME_CONTEXT_FDN, Boolean.FALSE);
        assertEquals(1, numberOfPersistedMos);
        verify(rootMoStub).getChild(CONNECTIVITY_INFO_RDN);
    }

    @Test
    public void testCreateChildForParentForLargeNode_CatchesModelConstraintViolationException() {
        final ManagedObject childMo = mock(ManagedObject.class);
        final String ciMappingFdn = ME_CONTEXT_FDN + "," + CONNECTIVITY_INFO_RDN;
        final ModelInfo ciModelInfo = new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, ERBS_NODE_MODEL_NAMESPACE,
                CONNECTIVITY_INFO_MO_NAME, MO_MODEL_VERSION_OLD);

        prepareParentAndChildMosForExceptionTests(rootMoStub, childMo, CONNECTIVITY_INFO_MO_TYPE,
                CONNECTIVITY_INFO_NODE_FDN, ciMappingFdn, ciModelInfo);

        when(mockDataBucket.getMibRootBuilder()).thenThrow(
                new ModelConstraintViolationException(MODEL_CONTRAINT_EXCEPTION_ENTITY_NAMESPACE,
                        MODEL_CONTRAINT_EXCEPTION_ENTITY_TYPE, MODEL_CONTRAINT_EXCEPTION_ENTITY_VERSION,
                        EXCEPTION_MESSAGE, MODEL_CONTRAINT_EXCEPTION_ENTITY_NAME, EXCEPTION_TEST_TEXT));

        final int numberOfPersistedMos = topologySyncAlgorithm.storeTopologyInDps(resultTopologyMapping,
                new ArrayList<>(resultTopologyMapping.keySet()), ME_CONTEXT_FDN, Boolean.FALSE);
        assertEquals(1, numberOfPersistedMos);
        verify(rootMoStub).getChild(CONNECTIVITY_INFO_RDN);
    }

    private void prepareParentAndChildMosForExceptionTests(final ManagedObject meCxtMo, final ManagedObject childMo,
            final String childType, final String childFdn, final String childMappingDn,
            final ModelInfo childModelInfo) {
        when(childMo.getFdn()).thenReturn(childFdn);
        when(childMo.getType()).thenReturn(childType);
        final String childRdn = FdnUtil.extractRdn(childMo.getFdn(), childMo.getType());
        when(meCxtMo.getChild(childRdn)).thenReturn(childMo);
        prepareMappingForExceptionCatchingTests(childMappingDn, childModelInfo);
    }

    private void prepareMappingForExceptionCatchingTests(final String mappingKey, final ModelInfo modelInfo) {
        resultTopologyMapping.clear();
        resultTopologyMapping.put(mappingKey, modelInfo);
    }

    private ManagedObject createMockMeContext() {
        final ManagedObject rootMo = mock(ManagedObject.class);
        when(rootMo.getFdn()).thenReturn(ME_CONTEXT_FDN);
        return rootMo;
    }

    private final class DummyAlreadyDefinedException extends AlreadyDefinedException {
        private static final long serialVersionUID = -3774440858454948772L;

        private DummyAlreadyDefinedException(final String message) {
            super(message);
        }
    }
}
