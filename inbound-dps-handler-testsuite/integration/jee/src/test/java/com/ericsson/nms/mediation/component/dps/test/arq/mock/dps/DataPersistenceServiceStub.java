/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.test.arq.mock.dps;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ACTIVE_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.CM_FUNCTION_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.CM_NODE_HEARTBEAT_SUPERVISION_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.CPP_CONN_INFO_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.CPP_MED_NAMESPACE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.FAILED_SYNCS_COUNT_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.GENERATION_COUNTER_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.IP_ADDRESS_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ME_CONTEXT_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.MO_ID1;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.NETWORK_ELEMENT_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.NE_TYPE_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.OSS_MODEL_IDENTITY_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.OSS_NE_CM_DEF_NAMESPACE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.OSS_NE_DEF_NAMESPACE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.OSS_PREFIX_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.SYNC_STATUS_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.availability.DpsAvailabilityCallback;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps;

@Singleton
@Startup
@LocalBean
public class DataPersistenceServiceStub {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPersistenceServiceStub.class);

    private static final String NODEID = "LTE001";

    private RuntimeConfigurableDps stubbedDps;

    @PostConstruct
    public void initialize() {
        stubbedDps = new RuntimeConfigurableDps();
        // initialize the DB for testing
        initStubbedDb();
        try {
            // register DPS in JNDI
            registerDpsInJndi();
        } catch (final NamingException e) {
            throw new IllegalStateException("Failure registering Stubbed DPS", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            LOGGER.info("Unbinding stubbed DataPersistenceService in JNDI");
            new InitialContext().unbind(DataPersistenceService.JNDI_LOOKUP_NAME);
        } catch (final NamingException e) {
            throw new IllegalStateException("Failure unregistering Stubbed DPS", e);
        }
    }

    public RuntimeConfigurableDps getStubbedDps() {
        return stubbedDps;
    }

    private void registerDpsInJndi() throws NamingException {
        final DataPersistenceService dps = spy(stubbedDps.build());
        LOGGER.info("Binding stubbed DataPersistenceService in JNDI: {}", dps);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                // immediately notify upon registration
                DpsAvailabilityCallback.class.cast(invocation.getArguments()[0]).onServiceAvailable();
                return null;
            }
        }).when(dps).registerDpsAvailabilityCallback(any(DpsAvailabilityCallback.class));
        new InitialContext().bind(DataPersistenceService.JNDI_LOOKUP_NAME, dps);
    }

    private void initStubbedDb() {
        // MeContext
        final ManagedObject meContextMo = stubbedDps.addManagedObject().namespace("OSS_TOP").version("3.0.0")
                .type(ME_CONTEXT_MO_TYPE).name(NODEID)
                .build();
        // NetworkElement
        final ManagedObject neMo = stubbedDps.addManagedObject().namespace(OSS_NE_DEF_NAMESPACE).version("2.0.0")
                .type(NETWORK_ELEMENT_MO_TYPE).name(NODEID)
                .addAttribute(NE_TYPE_ATTR_NAME, "ERBS")
                .addAttribute(OSS_PREFIX_ATTR_NAME, meContextMo.getFdn())
                .addAttribute(OSS_MODEL_IDENTITY_ATTR_NAME, "1116-673-956")
                .build();
        // CmFunction
        stubbedDps.addManagedObject().namespace(OSS_NE_CM_DEF_NAMESPACE).version("1.0.1")
                .type(CM_FUNCTION_MO_TYPE).name(MO_ID1)
                .parent(neMo)
                .addAttribute(NE_TYPE_ATTR_NAME, "ERBS")
                .addAttribute(SYNC_STATUS_ATTR_NAME, UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE)
                .addAttribute(FAILED_SYNCS_COUNT_ATTR_NAME, 0)
                .build();
        // CmNodeHeartbeatSupervision
        stubbedDps.addManagedObject().namespace(OSS_NE_CM_DEF_NAMESPACE).version("1.0.1")
                .type(CM_NODE_HEARTBEAT_SUPERVISION_MO_TYPE).name(MO_ID1)
                .parent(neMo)
                .addAttribute(ACTIVE_ATTR_NAME, true)
                .build();
        // CppConnectivityInformation
        stubbedDps.addManagedObject().namespace(CPP_MED_NAMESPACE).version("1.0.0")
                .type(CPP_CONN_INFO_MO_TYPE).name(MO_ID1)
                .parent(neMo)
                .addAttribute(IP_ADDRESS_ATTR_NAME, "127.0.0.1")
                .addAttribute(GENERATION_COUNTER_ATTR_NAME, 1L)
                .build();

        // Cdma2000Network
        stubbedDps.addManagedObject().namespace("ERBS_NODE_MODEL").version("3.12.0")
                .withFdn(meContextMo.getFdn() + ",ManagedElement=1,ENodeBFunction=1,Cdma2000Network=1")
                .generateTree()
                .build();
    }
}
