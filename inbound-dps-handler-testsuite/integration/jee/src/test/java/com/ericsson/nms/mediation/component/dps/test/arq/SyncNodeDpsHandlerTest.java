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

package com.ericsson.nms.mediation.component.dps.test.arq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ATTRIBUTE_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.PENDING_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.TOPOLOGY_SYNC_STATUS_ATTR_VALUE;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.MoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.test.arq.deployment.Artifact;
import com.ericsson.nms.mediation.component.dps.test.arq.mock.DpsHelper;
import com.ericsson.nms.mediation.component.dps.test.arq.mock.moci.MockMociConnection;
import com.ericsson.nms.mediation.component.dps.test.arq.util.ParameterConfiguratorUtil;
import com.ericsson.nms.mediation.component.dps.test.arq.util.TestUtil;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.mediation.core.events.MediationClientType;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;

@RunWith(Arquillian.class)
public class SyncNodeDpsHandlerTest extends DpsHandlerTest {

    private static final Logger log = LoggerFactory.getLogger(SyncNodeDpsHandlerTest.class);

    private static final String MO_OBTAINED_FROM_TOPOLOGY_SYNC = "ManagedElement=1,ENodeBFunction=1";
    private static final String MANAGED_ELEMENT_LTE001 = "MeContext=LTE001,ManagedElement=1";
    private static final String CM_FUNCTION_LTE001 = "NetworkElement=LTE001,CmFunction=1";
    private static final String ERBS_16_NODE_VERSION = "7.1.220";
    private static final Date RESTART_TIME_STAMP = new Date();
    private static final String BLACK_LIST_FILE = "src/test/resources/blacklist.csv";
    private static final String OSS_MODEL_IDENTITY = "1116-673-956";
    private static final String OSS_PREFIX = "MeContext=LTE001";
    private static final String NODE_NAME = "LTE001";
    private static final String NODE_TYPE = "ERBS";
    private static final int MO_INSTANCE_COUNT = 5000;

    @Inject
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperator;
    @Inject
    private MoDpsOperator moDpsOperator;

    @Deployment(name = "PIB", managed = true, testable = false, order = 1)
    public static Archive<?> createPibDeployment() {
        return ShrinkWrap.createFromZipFile(EnterpriseArchive.class, Artifact.resolveArtifactWithoutDependencies(Artifact.COM_ERICSSON_OSS_ITPF_PIB));
    }

    @Deployment(name = "MOCK_DPS", managed = true, testable = false, order = 2)
    public static Archive<?> createMockDpsDeployment() {
        return createStubbedDpsDeployment();
    }

    @Deployment(name = "MockEar", managed = true, testable = true, order = 3)
    public static EnterpriseArchive createDpsHandlerDeployment() {
        final File archiveFile = Artifact.resolveArtifactWithoutDependencies(Artifact.COM_ERICSSON_NMS_MEDIATION_DPS_HANDLER_CODE_EAR);
        if (archiveFile == null) {
            throw new IllegalStateException("Unable to resolve artifact " + Artifact.COM_ERICSSON_NMS_MEDIATION_DPS_HANDLER_CODE_EAR);
        }
        final EnterpriseArchive ear = ShrinkWrap.createFromZipFile(EnterpriseArchive.class, archiveFile);
        ear.addAsModule(createTestArchive("tests"));
        Artifact.createCustomApplicationXmlFile(ear, "tests");
        return ear;
    }

    private static Archive<?> createTestArchive(final String webModuleName) {
        final WebArchive testWar = ShrinkWrap.create(WebArchive.class, webModuleName + ".war");
        testWar.addClasses(SyncNodeDpsHandlerTest.class, DpsHandlerTest.class);
        testWar.addClass(DpsHelper.class);
        testWar.addClass(TestUtil.class);
        testWar.addClass(ParameterConfiguratorUtil.class);
        testWar.addClass(MockMociConnection.class);
        testWar.addAsLibraries(Artifact.resolveArtifactWithDependencies(Artifact.MOCKITO));
        testWar.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        testWar.setManifest("MANIFEST.MF");
        return testWar;
    }

    @Test
    @InSequence(1)
    @OperateOnDeployment("MockEar")
    public void testControllerHandlerWithoutEventType() throws Exception {
        testControllerHandler("DPS_BASED", RESTART_TIME_STAMP, false);
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment("MockEar")
    public void testControllerHandlerTriggerFullSync() throws Exception {
        testControllerHandler(MediationClientType.EVENT_BASED.toString(), new Date(), false);
    }

    @Test
    @InSequence(3)
    @OperateOnDeployment("MockEar")
    public void testControllerHandlerTriggerDeltaSync() throws Exception {
        testControllerHandler(MediationClientType.EVENT_BASED.toString(), RESTART_TIME_STAMP, true);
    }

    @Test
    @InSequence(4)
    @OperateOnDeployment("MockEar")
    public void testTopologySyncHandler() throws Exception {
        log.debug("--- Testing Topology Sync Handler ---");

        cmFunctionMoDpsOperator.setSyncStatus(CM_FUNCTION_LTE001, OSS_PREFIX, TOPOLOGY_SYNC_STATUS_ATTR_VALUE, null);

        final Map<String, Object> headers = new HashMap<>();
        headers.put("fdn", CM_FUNCTION_LTE001);
        topologySyncHandler.init(newEventHandlerContext(headers));
        blacklistStorage.listenForBlacklistChange(BLACK_LIST_FILE);
        log.debug("Finished setting blacklist");
        topologySyncHandler.onEvent(createComponentEventForTopology());
        assertNotNull(dpsHelper.getMo(MANAGED_ELEMENT_LTE001));
    }

    @Test
    @InSequence(5)
    @OperateOnDeployment("MockEar")
    public void testAttributeSyncHandler() throws Exception {
        testAttributeHandler();
    }

    @Test
    @InSequence(6)
    @OperateOnDeployment("MockEar")
    public void testDeltaSyncDpsHandler() throws Exception {
        log.debug("--- Testing Delta Sync Handler ---");

        final String cmNodeHeartbeatSupervisionFdn = FdnUtil.convertToCmNodeHeartbeatSupervision(CM_FUNCTION_LTE001);
        moDpsOperator.setAttr(cmNodeHeartbeatSupervisionFdn, "active", true);

        final Map<String, Object> headers = new HashMap<>();
        headers.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, CM_FUNCTION_LTE001);
        headers.put(MiscellaneousConstants.DELTA_SYNC_START_TIME_HEADER_NAME, System.currentTimeMillis());
        deltaSyncHandler.init(newEventHandlerContext(headers));
        deltaSyncHandler.onEvent(createComponentEventForDeltaSync());
        assertEquals(SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE, dpsHelper.getNodeSyncStatus(CM_FUNCTION_LTE001));
        assertEquals(USER_LABEL_ATTRIBUTE_VALUE + "DS", dpsHelper.getAttribute(MANAGED_ELEMENT_LTE001, USER_LABEL_ATTRIBUTE));
    }

    @Test
    @InSequence(7)
    @OperateOnDeployment("MockEar")
    public void testNodeInfoDpsHandler() throws Exception {
        dpsHelper.setNetworkElementType(NODE_NAME, NODE_TYPE);
        testNodeInfoDpsHandlerWithMoInstanceCount();
    }

    @Test
    @InSequence(8)
    @OperateOnDeployment("MockEar")
    public void testReducedTopBatchSizeAndAttrWriteMaxTime() throws Exception {
        log.debug("--- Testing Reduced TopBatchSize And AttrWriteMaxTime ---");
        final String configParameterName = "topology_dps_write_batch_size";
        cmFunctionMoDpsOperator.setSyncStatus(CM_FUNCTION_LTE001, OSS_PREFIX, TOPOLOGY_SYNC_STATUS_ATTR_VALUE, null);

        final Map<String, Object> headers = new HashMap<>();
        headers.put("fdn", CM_FUNCTION_LTE001);
        topologySyncHandler.init(newEventHandlerContext(headers));
        ParameterConfiguratorUtil.addConfigParameter(configParameterName, Integer.toString(10));
        topologySyncHandler.onEvent(createComponentEventForTopology());
        assertEquals(ATTRIBUTE_SYNC_STATUS_ATTR_VALUE, dpsHelper.getNodeSyncStatus(CM_FUNCTION_LTE001));
    }

    private void testNodeInfoDpsHandlerWithMoInstanceCount() {
        log.debug("--- Testing Node Info Dps Handler with mo instance count: [{}] ---", MO_INSTANCE_COUNT);

        cmFunctionMoDpsOperator.setSyncStatus(CM_FUNCTION_LTE001, OSS_PREFIX, PENDING_SYNC_STATUS_ATTR_VALUE, null);

        final Map<String, Object> headers = buildHeaders(MO_INSTANCE_COUNT);
        nodeInfoDpsHandler.init(newEventHandlerContext(headers));
        final ComponentEvent componentResultEvent = nodeInfoDpsHandler.onEvent(createComponentEvent(headers));
        assertTrue(componentResultEvent.getHeaders().containsKey(MiscellaneousConstants.LARGE_NODE_FLAG));
        assertFalse((boolean) componentResultEvent.getHeaders().get(MiscellaneousConstants.LARGE_NODE_FLAG));
    }

    private MediationComponentEvent createComponentEventForTopology() {
        log.info("Creating Component Event for Topology Sync Handler");
        final Map<String, Object> payload = new LinkedHashMap<>();
        final Map<String, Object> eventHeaders = createComponentEventBaseForTopology(NODE_TYPE, OSS_MODEL_IDENTITY);

        // build the payload
        payload.put(FdnUtil.appendRdn(OSS_PREFIX, "ManagedElement=1"),
                new ModelInfo("dps_primarytype", "ERBS_NODE_MODEL", "ManagedElement", ERBS_16_NODE_VERSION));
        payload.put(FdnUtil.appendRdn(OSS_PREFIX, MO_OBTAINED_FROM_TOPOLOGY_SYNC),
                new ModelInfo("dps_primarytype", "ERBS_NODE_MODEL", "ENodeBFunction", ERBS_16_NODE_VERSION));
        return new MediationComponentEvent(eventHeaders, payload);
    }

}
