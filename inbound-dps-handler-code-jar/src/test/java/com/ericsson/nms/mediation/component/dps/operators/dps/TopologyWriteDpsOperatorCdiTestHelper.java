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

package com.ericsson.nms.mediation.component.dps.operators.dps;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.object.builder.ManagedObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;

/**
 * Helper class for cdi tests for the topology write phase (utility methods).
 */
public class TopologyWriteDpsOperatorCdiTestHelper {

    protected static final String MO_MODEL_VERSION_NEW = "3.12.0";
    protected static final String MO_MODEL_VERSION_OLD = "3.1.72";
    protected static final String CONNECTIVITY_INFO_MO_NAME = "ConnectivityInfo";
    protected static final String EQUIPMENT_MO_NAME = "Equipment";
    protected static final String ANTENNA_UNIT_GROUP_MO_NAME = "AntennaUnitGroup";
    protected static final String ENODE_B_FUNCTION_MO_NAME = "ENodeBFunction";
    protected static final String RBS_CONFIGURATION_MO_NAME = "RbsConfiguration";
    protected static final String CHILD_OF_RBS_CONFIGURATION_MO_NAME = "ChildOfRbsConfiguration";
    protected static final String CABINET_MO_NAME = "Cabinet";
    protected static final String ERBS_NODE_MODEL_NAMESPACE = "ERBS_NODE_MODEL";
    protected static final String CPP_NODE_MODEL_NAMESPACE = "CPP_NODE_MODEL";
    protected static final String DPS_PRIMARYTYPE_SCHEMA_TYPE = "dps_primarytype";

    protected static final String MANAGED_ELEMENT_MO_TYPE = "ManagedElement";
    protected static final String MANAGED_ELEMENT_RDN = "ManagedElement=1";
    protected static final String ENODE_B_FUNCTION_RDN = "ENodeBFunction=1";
    protected static final String CONNECTIVITY_INFO_RDN = "ConnectivityInfo=1";
    protected static final String EQUIPEMENT_RDN = "Equipment=testEq";
    protected static final String ANTENNA_UNIT_GROUP_RDN = "AntennaUnitGroup=1";
    protected static final String CABINET_RDN = "Cabinet=1";
    protected static final String ME_CONTEXT_FDN = "MeContext=Erbs01";
    protected static final String RBS_CONFIGURATION_RDN = "RbsConfiguration=1";
    protected static final String CHILD_OF_RBS_CONFIGURATION_RDN = "ChildOfRbsConfiguration=1";

    protected static final String MANAGED_ELEMENT_FDN = ME_CONTEXT_FDN + "," + MANAGED_ELEMENT_RDN;
    protected static final String ENODE_B_FUNCTION_NODE_FDN = MANAGED_ELEMENT_FDN + "," + ENODE_B_FUNCTION_RDN;
    protected static final String CONNECTIVITY_INFO_NODE_FDN = MANAGED_ELEMENT_FDN + "," + CONNECTIVITY_INFO_RDN;
    protected static final String EQUIPMENT_NODE_FDN = MANAGED_ELEMENT_FDN + "," + EQUIPEMENT_RDN;
    protected static final String ANTENNA_UNIT_GROUP_NODE_FDN = EQUIPMENT_NODE_FDN + "," + ANTENNA_UNIT_GROUP_RDN;
    protected static final String CABINET_NODE_FDN = EQUIPMENT_NODE_FDN + "," + CABINET_RDN;
    protected static final String RBS_CONFIGURATION_FDN = ENODE_B_FUNCTION_NODE_FDN + "," + RBS_CONFIGURATION_RDN;
    protected static final String CHILD_OF_RBS_CONFIGURATION_FDN = RBS_CONFIGURATION_FDN + "," + CHILD_OF_RBS_CONFIGURATION_RDN;

    @Inject
    private DataPersistenceService dps;

    public ManagedObject findByFdn(final String fdn) {
        return dps.getLiveBucket().findMoByFdn(fdn);
    }

    public long countChildren(final String fdn) {
        return dps.getLiveBucket().getQueryExecutor().executeCount(dps.getQueryBuilder().createContainmentQuery(fdn));
    }

    public ManagedObject populateDpsTopHierarqui() {
        final ManagedObject rootMo = createRootMo("Erbs01");
        final ManagedObject managedElement = createMo("ManagedElement", "1", rootMo);
        final ManagedObject networkElement = dps.getLiveBucket().getMibRootBuilder().namespace(ERBS_NODE_MODEL_NAMESPACE)
                .type("NetworkElement").version(MO_MODEL_VERSION_NEW).name("Erbs01").create();
        final ManagedObject connInfo = createMo("CppConnectivityInformation", "1", networkElement);
        return rootMo;
    }

    public ManagedObject populateDps() {
        return populateDps(new HashSet<String>(), false);
    }

    public ManagedObject populateDps(final Set<String> fdnsToExclude) {
        return populateDps(fdnsToExclude, false);
    }

    private ManagedObject populateDps(final Set<String> fdnsToExclude, final boolean excludingChildren) {
        final ManagedObject rootMo = populateDpsTopHierarqui();
        final ManagedObject managedElement = rootMo.getChildren().iterator().next();

        final ManagedObject enbF = createMo("ENodeBFunction", "1", managedElement);
        final ManagedObject rbs = createMo("RbsConfiguration", "1", enbF);
        final ManagedObject equip = createMo("Equipment", "testEq", managedElement);

        if (excludingChildren) {
            return rootMo;
        }

        if (!fdnsToExclude.contains(CHILD_OF_RBS_CONFIGURATION_MO_NAME)) {
            createMo("ChildOfRbsConfiguration", "1", rbs);
        }

        if (!fdnsToExclude.contains(ANTENNA_UNIT_GROUP_MO_NAME)) {
            createMo("AntennaUnitGroup", "1", equip);
        }

        if (!fdnsToExclude.contains(CABINET_MO_NAME)) {
            createMo("Cabinet", "1", equip);
        }
        return rootMo;
    }

    public ManagedObject populateDpsExcludingChildren() {
        return populateDps(new HashSet<String>(), true);
    }

    public Map<String, ModelInfo> createFullPayload() {
        return createPayloadWithout(new HashSet<String>());
    }

    public Map<String, ModelInfo> createPayloadWithout(final Set<String> fdns) {
        final Map<String, ModelInfo> resultTopologyMapping = new TreeMap<>();
        resultTopologyMapping.put(MANAGED_ELEMENT_FDN,
                new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, CPP_NODE_MODEL_NAMESPACE, MANAGED_ELEMENT_MO_TYPE, MO_MODEL_VERSION_NEW));
        resultTopologyMapping.put(CONNECTIVITY_INFO_NODE_FDN,
                new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, ERBS_NODE_MODEL_NAMESPACE, CONNECTIVITY_INFO_MO_NAME, MO_MODEL_VERSION_OLD));
        resultTopologyMapping.put(EQUIPMENT_NODE_FDN,
                new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, ERBS_NODE_MODEL_NAMESPACE, EQUIPMENT_MO_NAME, MO_MODEL_VERSION_OLD));
        resultTopologyMapping.put(ENODE_B_FUNCTION_NODE_FDN,
                new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, ERBS_NODE_MODEL_NAMESPACE, ENODE_B_FUNCTION_MO_NAME, MO_MODEL_VERSION_OLD));
        if (!fdns.contains(ANTENNA_UNIT_GROUP_NODE_FDN)) {
            resultTopologyMapping.put(ANTENNA_UNIT_GROUP_NODE_FDN,
                    new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, ERBS_NODE_MODEL_NAMESPACE, ANTENNA_UNIT_GROUP_MO_NAME, MO_MODEL_VERSION_OLD));
        }

        if (!fdns.contains(CABINET_NODE_FDN)) {
            resultTopologyMapping.put(CABINET_NODE_FDN,
                    new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, ERBS_NODE_MODEL_NAMESPACE, CABINET_MO_NAME, MO_MODEL_VERSION_OLD));
        }
        resultTopologyMapping.put(RBS_CONFIGURATION_FDN,
                new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, ERBS_NODE_MODEL_NAMESPACE, RBS_CONFIGURATION_MO_NAME, MO_MODEL_VERSION_OLD));
        if (!fdns.contains(CHILD_OF_RBS_CONFIGURATION_FDN)) {
            resultTopologyMapping.put(CHILD_OF_RBS_CONFIGURATION_FDN,
                    new ModelInfo(DPS_PRIMARYTYPE_SCHEMA_TYPE, ERBS_NODE_MODEL_NAMESPACE, CHILD_OF_RBS_CONFIGURATION_MO_NAME, MO_MODEL_VERSION_OLD));
        }
        return resultTopologyMapping;
    }

    private ManagedObject createRootMo(final String id) {
        return dps.getLiveBucket().getMibRootBuilder().namespace(ERBS_NODE_MODEL_NAMESPACE).type("MeContext").version(MO_MODEL_VERSION_NEW).name(id)
                .create();
    }

    private ManagedObject createMo(final String type, final String id, final ManagedObject root) {
        return createMo(type, id, root, new HashMap<String, Object>());
    }

    private ManagedObject createMo(final String type, final String id, final ManagedObject root, final Map<String, Object> attributes) {
        final ManagedObjectBuilder builder = dps.getLiveBucket().getManagedObjectBuilder().type(type).name(id).parent(root);
        final Iterator<String> attrKeyIter = attributes.keySet().iterator();
        while (attrKeyIter.hasNext()) {
            final String attrName = attrKeyIter.next();
            builder.addAttribute(attrName, attributes.get(attrName));
        }
        return builder.create();
    }
}
