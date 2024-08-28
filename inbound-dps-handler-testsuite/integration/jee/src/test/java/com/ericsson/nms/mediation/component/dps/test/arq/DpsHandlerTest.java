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

package com.ericsson.nms.mediation.component.dps.test.arq;

import static org.junit.Assert.assertEquals;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.PENDING_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.CLIENT_TYPE_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.GENERATION_COUNTER_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.IS_FIRST_SYNC_FLAG;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.NODE_TYPE_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_MODEL_IDENTITY_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_PREFIX_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.PO_ID_HEADER_NAME;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.blacklist.BlacklistConfigurationStorage;
import com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants;
import com.ericsson.enm.mediation.handler.common.dps.operators.MoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.event.MediationEventSender;
import com.ericsson.nms.mediation.component.dps.handlers.AttributeDpsHandler;
import com.ericsson.nms.mediation.component.dps.handlers.ControllerHandler;
import com.ericsson.nms.mediation.component.dps.handlers.DeltaDpsHandler;
import com.ericsson.nms.mediation.component.dps.handlers.NodeInfoDpsHandler;
import com.ericsson.nms.mediation.component.dps.handlers.TopologyDpsHandler;
import com.ericsson.nms.mediation.component.dps.handlers.payload.DeltaSyncDpsPayload;
import com.ericsson.nms.mediation.component.dps.test.arq.deployment.Artifact;
import com.ericsson.nms.mediation.component.dps.test.arq.mock.DpsHelper;
import com.ericsson.nms.mediation.component.dps.test.arq.mock.dps.DataPersistenceServiceStub;
import com.ericsson.nms.mediation.component.dps.test.arq.mock.dps.DataUpgradeHandlerMock;
import com.ericsson.nms.mediation.component.dps.test.arq.util.TestUtil;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.config.Configuration;
import com.ericsson.oss.itpf.common.event.ControlEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.EventSubscriber;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.ericsson.oss.mediation.network.api.notifications.NotificationType;

public abstract class DpsHandlerTest {
    protected static final String BEANS_XML_FILE_NAME = "beans.xml";
    protected static final String SFWK_CONFIGURATION_FILE_NAME = "ServiceFrameworkConfiguration.properties";
    protected static final String ME_CONTEXT_LTE001 = "MeContext=LTE001";
    protected static final String CM_FUNCTION_LTE001 = "NetworkElement=LTE001,CmFunction=1";
    protected static final String MANAGED_ELEMENT_LTE001 = "MeContext=LTE001,ManagedElement=1";
    protected static final String USER_LABEL_ATTRIBUTE = "userLabel";
    protected static final String NE_TYPE_ATTRIBUTE_VALUE = "ERBS";
    protected static final String NE_TYPE_ATTRIBUTE_NAME = "neType";
    protected static final String USER_LABEL_ATTRIBUTE_VALUE = "test_user_label";
    protected static final long NODE_GEN_COUNT_VALUE = 1307L;

    private static final Logger log = LoggerFactory.getLogger(DpsHandlerTest.class);
    private static final Long PO_ID_HEADER_VALUE = 12345L;

    @EJB
    protected DpsHelper dpsHelper;
    @Inject
    protected ControllerHandler controllerHandler;
    @Inject
    protected TopologyDpsHandler topologySyncHandler;
    @Inject
    protected AttributeDpsHandler attributeSyncHandler;
    @Inject
    protected DeltaDpsHandler deltaSyncHandler;
    @Inject
    protected NodeInfoDpsHandler nodeInfoDpsHandler;
    @Inject
    protected BlacklistConfigurationStorage blacklistStorage;
    @Inject
    private MoDpsOperator moDpsOperator;

    public static Archive<?> createStubbedDpsDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "stubbed-dps.jar");
        jar.addAsResource(EmptyAsset.INSTANCE, BEANS_XML_FILE_NAME);
        jar.addAsResource("stubbed-dps/" + SFWK_CONFIGURATION_FILE_NAME, SFWK_CONFIGURATION_FILE_NAME);
        jar.addClass(DataPersistenceServiceStub.class);
        jar.addClass(DataUpgradeHandlerMock.class);
        jar.addClass(MoConstants.class);
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "stubbed-dps.ear");
        ear.addAsManifestResource("MANIFEST.MF");
        ear.addAsModule(jar);
        ear.addAsLibraries(Artifact.resolveArtifactWithDependencies(Artifact.DPS_TEST_SUPPORT));
        ear.addAsLibraries(Artifact.resolveArtifactWithDependencies(Artifact.MOCKITO));
        return ear;
    }

    protected void testControllerHandler(final String eventType, final Date nodeDate, final boolean desiredDeltaSync) throws Exception {
        log.info("--- Testing Controller Handler ---");
        dpsHelper.resetSyncStatusToUnsynchronized(CM_FUNCTION_LTE001);
        final MediationEventSender mockMediationEventSender = Mockito.mock(MediationEventSender.class);

        final Map<String, Object> headers = new HashMap<>();
        headers.put("fdn", CM_FUNCTION_LTE001);
        headers.put(PO_ID_HEADER_NAME, PO_ID_HEADER_VALUE);
        headers.put(CLIENT_TYPE_HEADER_NAME, eventType);
        TestUtil.setFieldValue(ControllerHandler.class, "mediationEventSender", controllerHandler, mockMediationEventSender);
        controllerHandler.init(newEventHandlerContext(headers));
        controllerHandler.onEvent(createComponentEvent(headers));

        assertEquals(PENDING_SYNC_STATUS_ATTR_VALUE, dpsHelper.getNodeSyncStatus(CM_FUNCTION_LTE001));
    }

    protected void testAttributeHandler() throws Exception {
        log.debug("--- Testing Attribute Sync Handler ---");

        final String cmNodeHeartbeatSupervisionFdn = FdnUtil.convertToCmNodeHeartbeatSupervision(CM_FUNCTION_LTE001);
        moDpsOperator.setAttr(cmNodeHeartbeatSupervisionFdn, "active", true);

        final Map<String, Object> headers = new HashMap<>();
        headers.put("fdn", CM_FUNCTION_LTE001);
        headers.put("sync start time", System.currentTimeMillis());
        headers.put(IS_FIRST_SYNC_FLAG, Boolean.FALSE);
        attributeSyncHandler.init(newEventHandlerContext(headers));
        attributeSyncHandler.onEvent(createComponentEventForAttribute());
        assertEquals(SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE, dpsHelper.getNodeSyncStatus(CM_FUNCTION_LTE001));
        // check that neType under ManagedElement MO is set after Full Sync
        assertEquals(NE_TYPE_ATTRIBUTE_VALUE, dpsHelper.getMo(MANAGED_ELEMENT_LTE001).getAttribute(NE_TYPE_ATTRIBUTE_NAME));
    }

    protected MediationComponentEvent createComponentEvent(final Map<String, Object> headers) {
        log.debug("Creating Component Event for Controller Handler");
        final Object payload = new Object();
        return new MediationComponentEvent(headers, payload);
    }

    protected Map<String, Object> buildHeaders(final Integer moInstanceCount) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, CM_FUNCTION_LTE001);
        headers.put(MiscellaneousConstants.DELTA_SYNC_START_TIME_HEADER_NAME, System.currentTimeMillis());
        headers.put(MiscellaneousConstants.MO_INSTANCE_COUNT, moInstanceCount);
        return headers;
    }

    private MediationComponentEvent createComponentEventForAttribute() {
        log.info("Creating Component Event for Attribute Sync Handler");

        final Map<String, Object> eventHeaders = new HashMap<>();
        eventHeaders.put("fdn", CM_FUNCTION_LTE001);
        eventHeaders.put(GENERATION_COUNTER_HEADER_NAME, NODE_GEN_COUNT_VALUE);
        eventHeaders.put("ossPrefix", ME_CONTEXT_LTE001);
        eventHeaders.put("sync start time", System.currentTimeMillis());
        eventHeaders.put("nodeType", NE_TYPE_ATTRIBUTE_VALUE);
        eventHeaders.put(IS_FIRST_SYNC_FLAG, Boolean.FALSE);

        final Map<String, Object> payload = new LinkedHashMap<>();
        final Map<String, Object> recordOne = new HashMap<>();
        recordOne.put(USER_LABEL_ATTRIBUTE, USER_LABEL_ATTRIBUTE_VALUE);
        payload.put("MeContext=LTE001,ManagedElement=1", recordOne);
        return new MediationComponentEvent(eventHeaders, payload);
    }

    protected MediationComponentEvent createComponentEventForDeltaSync() {
        log.info("Creating Component Event for DeltaSyncDPSHandler");
        final Map<String, Object> eventHeaders = new HashMap<>();
        eventHeaders.put("fdn", CM_FUNCTION_LTE001);
        eventHeaders.put("ossPrefix", ME_CONTEXT_LTE001);
        eventHeaders.put("nodeType", NE_TYPE_ATTRIBUTE_VALUE);
        eventHeaders.put(MiscellaneousConstants.DELTA_SYNC_START_TIME_HEADER_NAME, System.currentTimeMillis());
        eventHeaders.put(MiscellaneousConstants.GENERATION_COUNTER_HEADER_NAME, 22L);
        eventHeaders.put("synctype", "delta");
        final NodeNotification update1 = new NodeNotification();
        update1.setFdn("ManagedElement=1");
        update1.setAction(NotificationType.UPDATE);
        update1.setGenerationCounter(10L);
        update1.setName("ManagedElement");
        final NodeNotification update2 = new NodeNotification();
        update2.setFdn("ManagedElement=1,ENodeBFunction=1,Cdma2000Network=1");
        update2.setAction(NotificationType.UPDATE);
        update2.setGenerationCounter(20L);
        update2.setName("Cdma2000Network");
        final Map<String, Object> recordOne = new HashMap<>();
        final Map<String, Object> recordTwo = new HashMap<>();
        recordOne.put(USER_LABEL_ATTRIBUTE, USER_LABEL_ATTRIBUTE_VALUE + "DS");
        update1.setUpdateAttributes(recordOne);
        update2.setUpdateAttributes(recordTwo);
        final List<NodeNotification> updates = new LinkedList<>();
        updates.add(update1);
        updates.add(update2);

        final DeltaSyncDpsPayload payload = new DeltaSyncDpsPayload(updates, new LinkedList<NodeNotification>(), NODE_GEN_COUNT_VALUE + 2);
        return new MediationComponentEvent(eventHeaders, payload);
    }

    protected Map<String, Object> createComponentEventBaseForTopology(final String nodeType, final String ossModelIdentity) {
        log.info("Creating Component Event base for Topology Sync Handler");
        final Map<String, Object> eventHeaders = new HashMap<>();
        eventHeaders.put("fdn", ME_CONTEXT_LTE001);
        eventHeaders.put(GENERATION_COUNTER_HEADER_NAME, NODE_GEN_COUNT_VALUE);
        eventHeaders.put(NODE_TYPE_HEADER_NAME, nodeType);
        eventHeaders.put(OSS_MODEL_IDENTITY_HEADER_NAME, ossModelIdentity);
        eventHeaders.put(OSS_PREFIX_HEADER_NAME, ME_CONTEXT_LTE001);
        return eventHeaders;
    }

    protected EventHandlerContext newEventHandlerContext(final Map<String, Object> properties) {
        return new EventHandlerContext() {
            @Override
            public void sendControlEvent(final ControlEvent arg) {}

            @Override
            public Collection<EventSubscriber> getEventSubscribers() {
                return null;
            }

            @Override
            public Configuration getEventHandlerConfiguration() {
                return new Configuration() {
                    @Override
                    public String getStringProperty(final String arg) {
                        return null;
                    }

                    @Override
                    public Integer getIntProperty(final String arg) {
                        return null;
                    }

                    @Override
                    public Boolean getBooleanProperty(final String arg) {
                        return null;
                    }

                    @Override
                    public Map<String, Object> getAllProperties() {
                        return properties;
                    }
                };
            }

            @Override
            public Object getContextualData(final String arg) {
                return null;
            }
        };
    }

}
