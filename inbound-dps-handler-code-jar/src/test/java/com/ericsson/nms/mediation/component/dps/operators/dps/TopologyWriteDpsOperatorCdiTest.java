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

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.verify;

import static com.ericsson.nms.mediation.component.dps.operators.dps.TopologyWriteDpsOperatorCdiTestHelper.CABINET_MO_NAME;
import static com.ericsson.nms.mediation.component.dps.operators.dps.TopologyWriteDpsOperatorCdiTestHelper.CABINET_NODE_FDN;
import static com.ericsson.nms.mediation.component.dps.operators.dps.TopologyWriteDpsOperatorCdiTestHelper.CHILD_OF_RBS_CONFIGURATION_FDN;
import static com.ericsson.nms.mediation.component.dps.operators.dps.TopologyWriteDpsOperatorCdiTestHelper.RBS_CONFIGURATION_FDN;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_RESOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_SOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.NODE_TYPE_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_MODEL_IDENTITY_HEADER_NAME;
import static com.ericsson.oss.itpf.sdk.recording.EventLevel.DETAILED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.ericsson.cds.cdi.support.rule.CdiInjectorRule;
import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest;
import com.ericsson.enm.mediation.handler.common.blacklist.BlacklistConfigurationStorage;
import com.ericsson.enm.mediation.handler.common.blacklist.BlacklistMo;
import com.ericsson.nms.mediation.component.dps.operators.model.IntegrityModelOperator;
import com.ericsson.nms.mediation.component.dps.test.util.MockDataPersistenceService;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Tests for the TopologyWriteDpsOperator class.
 */
public class TopologyWriteDpsOperatorCdiTest {

    private static Map<String, Object> eventHeader;

    @Rule
    public CdiInjectorRule rule = new CdiInjectorRule(this);

    @ObjectUnderTest
    private TopologyWriteDpsOperator topologyWriteDpsOperator;

    @ImplementationInstance
    private DataPersistenceService dps = new MockDataPersistenceService();

    @ImplementationInstance
    private IntegrityModelOperator integrityModelOperator = new IntegrityModelOperator() {
        @Override
        public boolean isParentChildRelationshipAllowed(final ModelInfo parentModelInfo, final ModelInfo childModelInfo) {
            return moNameWhichParentRelationshipNotAllowed == null ? true : childModelInfo.toUrn().contains(moNameWhichParentRelationshipNotAllowed);
        }
    };

    @Inject
    private TopologyWriteDpsOperatorCdiTestHelper helper;

    @Inject
    private BlacklistConfigurationStorage blacklistStorage;

    @Inject
    private SystemRecorder systemRecorder;

    private String moNameWhichParentRelationshipNotAllowed;

    @BeforeClass
    public static void setup() {
        eventHeader = new HashMap<>();
        eventHeader.put(NODE_TYPE_HEADER_NAME, "ERBS");
        eventHeader.put(OSS_MODEL_IDENTITY_HEADER_NAME, "v1234");
    }

    @Test
    public void shouldUpdateDpsWithExtraDataInPayload() {
        final ManagedObject rootMo = helper.populateDpsTopHierarqui();
        assertThat(helper.countChildren(rootMo.getFdn())).isEqualTo(1);

        final Map<String, ModelInfo> payload = helper.createFullPayload();
        final ComponentEvent event = new MediationComponentEvent(eventHeader, payload);

        topologyWriteDpsOperator.persistTopology(event, rootMo.getFdn(), Boolean.FALSE);
        assertThat(helper.countChildren(rootMo.getFdn())).isEqualTo(8);
    }

    @Test
    public void shouldNotUpdateDpsWhenDataInPayloadIsInDpsAlready() {
        final ManagedObject rootMo = helper.populateDps();

        final Map<String, ModelInfo> payload = helper.createFullPayload();
        final ComponentEvent event = new MediationComponentEvent(eventHeader, payload);

        topologyWriteDpsOperator.persistTopology(event, rootMo.getFdn(), Boolean.FALSE);

        verify(systemRecorder).recordEvent(anyString(), eq(DETAILED), eq(EVENT_SOURCE), eq(EVENT_RESOURCE),
                matches(".*(MOs created: 1).*(MOs deleted: 0).*"));
    }

    @Test
    public void shouldRemoveFromDpsWhenDataNotInThePayload() {
        final ManagedObject rootMo = helper.populateDps();
        assertThat(helper.findByFdn(RBS_CONFIGURATION_FDN).getChildrenSize()).isEqualTo(1);

        final Map<String, ModelInfo> payload = helper.createPayloadWithout(Sets.newHashSet(CHILD_OF_RBS_CONFIGURATION_FDN));
        final ComponentEvent event = new MediationComponentEvent(eventHeader, payload);

        topologyWriteDpsOperator.persistTopology(event, rootMo.getFdn(), Boolean.FALSE);
        assertThat(helper.findByFdn(RBS_CONFIGURATION_FDN).getChildrenSize()).isEqualTo(0);
    }

    @Test
    public void shouldRemoveFromDpsDataNotInThePayloadAndAddPayloadDataNotInDps() {
        final ManagedObject rootMo = helper.populateDps(Sets.newHashSet(CABINET_MO_NAME));
        assertThat(helper.findByFdn(CABINET_NODE_FDN)).isNull();
        assertThat(helper.findByFdn(CHILD_OF_RBS_CONFIGURATION_FDN)).isNotNull();

        final Map<String, ModelInfo> payload = helper.createPayloadWithout(Sets.newHashSet(CHILD_OF_RBS_CONFIGURATION_FDN));
        final ComponentEvent event = new MediationComponentEvent(eventHeader, payload);

        topologyWriteDpsOperator.persistTopology(event, rootMo.getFdn(), Boolean.FALSE);
        assertThat(helper.findByFdn(CABINET_NODE_FDN)).isNotNull();
        assertThat(helper.findByFdn(CHILD_OF_RBS_CONFIGURATION_FDN)).isNull();
    }

    @Test
    public void shouldRecordInstrumentation() {
        final ManagedObject rootMo = helper.populateDps(Sets.newHashSet(CABINET_MO_NAME));
        final Map<String, ModelInfo> payload = helper.createPayloadWithout(Sets.newHashSet(CHILD_OF_RBS_CONFIGURATION_FDN));
        final ComponentEvent event = new MediationComponentEvent(eventHeader, payload);
        topologyWriteDpsOperator.persistTopology(event, rootMo.getFdn(), Boolean.FALSE);
        verify(systemRecorder).recordEvent(anyString(), eq(DETAILED), eq(EVENT_SOURCE), eq(EVENT_RESOURCE),
                matches("(Invoked Topology Write DPS operation for FDN \\[" + rootMo.getFdn()
                        + "\\]).*(Total MOs: 7, 6 MOs read prior to sync).*(MOs created: 2).*(MOs deleted: 1).*"));
    }

    @Test
    public void shouldContinueToSyncWhenParentChildNotAllowed() {
        moNameWhichParentRelationshipNotAllowed = CABINET_MO_NAME;
        try {
            final ManagedObject rootMo = helper.populateDpsExcludingChildren();
            assertThat(helper.countChildren(rootMo.getFdn())).isEqualTo(4);

            final Map<String, ModelInfo> payload = helper.createFullPayload();
            assertThat(payload).hasSize(8);

            final ComponentEvent event = new MediationComponentEvent(eventHeader, payload);
            topologyWriteDpsOperator.persistTopology(event, rootMo.getFdn(), Boolean.FALSE);
            assertThat(helper.countChildren(rootMo.getFdn())).isEqualTo(5);
        } finally {
            moNameWhichParentRelationshipNotAllowed = null;
        }
    }

    @Test
    public void shouldNotSyncBlacklistedMoWhenSyncAttributeIsFalse() {
        syncBlacklisted(false);
    }

    @Test
    public void shouldSyncBlacklistedWhenSyncAttributeIsTrue() {
        syncBlacklisted(true);
    }

    void syncBlacklisted(final boolean syncAttribute) {
        final List<BlacklistMo> list = Lists.newArrayList(new BlacklistMo("Cabinet", "", syncAttribute));
        blacklistStorage.getBlacklistMap().put("ERBS__v1234", list);

        final ManagedObject rootMo = helper.populateDps();
        assertThat(helper.findByFdn(CABINET_NODE_FDN)).isNotNull();

        final Map<String, ModelInfo> payload = helper.createFullPayload();
        final ComponentEvent event = new MediationComponentEvent(eventHeader, payload);

        topologyWriteDpsOperator.persistTopology(event, rootMo.getFdn(), Boolean.FALSE);

        if (syncAttribute) {
            assertThat(helper.findByFdn(CABINET_NODE_FDN)).isNotNull();
        } else {
            assertThat(helper.findByFdn(CABINET_NODE_FDN)).isNull();
        }
    }
}
