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

package com.ericsson.nms.mediation.component.dps.handlers.algorithms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.defaultanswers.ReturnsEmptyValues;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.enm.mediation.handler.common.event.CmEventSender;
import com.ericsson.nms.mediation.component.dps.operators.model.IntegrityModelOperator;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.object.builder.ManagedObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.object.builder.MibRootBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.ericsson.oss.mediation.network.api.notifications.NotificationType;

@RunWith(MockitoJUnitRunner.class)
public class DeltaSyncCreateUpdateAlgorithmTest {
    private static final String FDN_UPDATE_DPS = "ManagedElement=1,mo1=1";
    private static final String FDN_UPDATE_NOT_DPS = "ManagedElement=1,mo1=1,mo2=1";
    private static final String FDN_CREATE = "ManagedElement=1,mo1=1,mo2=1,mo3=1";
    private static final String FDN_RBSCONFIG_CREATE = "ManagedElement=1,ENodeBFunction=1,RbsConfiguration=1";
    private static final String FDN_OTHER_BRANCH = "ManagedElement=1,mo4=1";

    private static final String NAMESPACE_1 = "CPP_NODE_MODEL";
    private static final String NAMESPACE_2 = "ERBS_NODE_MODEL";
    private static final String VERSION_1 = "1.1.1";
    private static final String VERSION_2 = "1.2.1";
    private static final String ME_CONTEXT = "MeContext=NODE";

    private static final String ENODEB_FUNCTION_MO_TYPE = "ENodeBFunction";

    @Mock
    private IntegrityModelOperator integrityModelOperatorMock;

    @Mock
    private DataBucket liveBucketMock;
    @Mock
    private ManagedObject updateInDpsMoMock;
    @Mock
    private ManagedObject updateNotInDpsMoMock;
    @Mock
    private ManagedObject createMoMock;
    @Mock
    private ManagedObject createEnodebFunctionMoMock;
    @Mock
    private ManagedObject createInOtherBranchMoMock;
    @Mock
    private ManagedObject nodeRootMoMock;
    @Mock
    private CmEventSender mockCmEventSender;

    private MibRootBuilder mibRootBuilderMock;
    private ManagedObjectBuilder managedObjectBuilderMock1;
    private ManagedObjectBuilder managedObjectBuilderMock2;

    private final Map<String, Object> attributes = new HashMap<>();
    private final List<NodeNotification> nodeNotifications = new ArrayList<>();

    @InjectMocks
    private DeltaSyncAlgorithm deltaSyncAlgorithm;

    @Before
    public void setUp() throws Exception {
        createNotifications();

        when(integrityModelOperatorMock.isParentChildRelationshipAllowed(any(ModelInfo.class), any(ModelInfo.class))).thenReturn(true);

        mibRootBuilderMock = Mockito.mock(MibRootBuilder.class, new AnswerWithSelf(MibRootBuilder.class));
        managedObjectBuilderMock1 = Mockito.mock(ManagedObjectBuilder.class, new AnswerWithSelf(ManagedObjectBuilder.class));
        managedObjectBuilderMock2 = Mockito.mock(ManagedObjectBuilder.class, new AnswerWithSelf(ManagedObjectBuilder.class));
        Mockito.doReturn(mibRootBuilderMock).when(liveBucketMock).getMibRootBuilder();
        Mockito.when(liveBucketMock.getManagedObjectBuilder()).thenReturn(managedObjectBuilderMock1).thenReturn(managedObjectBuilderMock2);

        when(createMoMock.getFdn()).thenReturn(FDN_RBSCONFIG_CREATE);
    }

    @Test
    public void testProcessChanges() {
        stubMockInvocations();
        deltaSyncAlgorithm.processCreateUpdateChanges(nodeNotifications, liveBucketMock, ME_CONTEXT);
        verifyMocks();
    }

    @Test
    public void testGetValuesChangedOnTheNode() {
        final Map<String, Object> currentMoAttributeValuesFromDps = new HashMap<>();
        currentMoAttributeValuesFromDps.put("userLabel", "notChanged");
        currentMoAttributeValuesFromDps.put("operationalState", "LOCKED");

        final NodeNotification nodeNotification = createOneNodeNotification();

        deltaSyncAlgorithm.getChangedAttributes(nodeNotification, currentMoAttributeValuesFromDps);

        assertThat(nodeNotification.getUpdateAttributes().get("operationalState").equals("UNLOCKED"));
    }

    private NodeNotification createOneNodeNotification() {
        final NodeNotification nodeNotification = new NodeNotification();

        final Map<String, String> parentParams = new HashMap<>();
        final Map<String, Object> attributeValues = new HashMap<>();
        attributeValues.put("userLabel", "notChanged");
        attributeValues.put("operationalState", "UNLOCKED");

        parentParams.put("parentName", ENODEB_FUNCTION_MO_TYPE);
        parentParams.put("parentNameSpace", ENODEB_FUNCTION_MO_TYPE);
        parentParams.put("parentVersion", VERSION_2);

        nodeNotification.setAction(NotificationType.UPDATE);
        nodeNotification.setGenerationCounter(2L);
        nodeNotification.setFdn(FDN_UPDATE_DPS);
        nodeNotification.setNameSpace(NAMESPACE_1);
        nodeNotification.setName(FdnUtil.extractMoType(FDN_UPDATE_DPS));
        nodeNotification.setVersion(VERSION_1);
        nodeNotification.setUpdateAttributes(attributeValues);
        nodeNotification.setParentSupported(parentParams);

        return nodeNotification;
    }

    private void stubMockInvocations() {
        Mockito.doReturn(updateInDpsMoMock).when(liveBucketMock).findMoByFdn(FdnUtil.prependMeContext(ME_CONTEXT, FDN_UPDATE_DPS));

        Mockito.doReturn(updateNotInDpsMoMock).when(managedObjectBuilderMock1).create();
        Mockito.doReturn(null).when(liveBucketMock).findMoByFdn(FdnUtil.prependMeContext(ME_CONTEXT, FDN_UPDATE_NOT_DPS));
        Mockito.doReturn(NAMESPACE_1).when(updateInDpsMoMock).getNamespace();
        Mockito.doReturn(createEnodebFunctionMoMock).when(liveBucketMock).findMoByFdn("MeContext=NODE,ManagedElement=1,ENodeBFunction=1");
        Mockito.doReturn(createMoMock).when(managedObjectBuilderMock2).create();
        Mockito.doReturn("parentMo").when(createEnodebFunctionMoMock).getFdn();

        Mockito.doReturn(NAMESPACE_1).when(updateNotInDpsMoMock).getNamespace();
        Mockito.doReturn(ENODEB_FUNCTION_MO_TYPE).when(updateNotInDpsMoMock).getName();
        Mockito.doReturn(ENODEB_FUNCTION_MO_TYPE).when(updateNotInDpsMoMock).getType();

        Mockito.doReturn(VERSION_2).when(updateNotInDpsMoMock).getVersion();
        Mockito.doReturn(NAMESPACE_1).when(updateInDpsMoMock).getNamespace();
        Mockito.doReturn(ENODEB_FUNCTION_MO_TYPE).when(updateInDpsMoMock).getName();
        Mockito.doReturn(ENODEB_FUNCTION_MO_TYPE).when(updateInDpsMoMock).getType();
        Mockito.doReturn(VERSION_2).when(updateInDpsMoMock).getVersion();

        Mockito.doReturn(NAMESPACE_1).when(createEnodebFunctionMoMock).getNamespace();
        Mockito.doReturn(ENODEB_FUNCTION_MO_TYPE).when(createEnodebFunctionMoMock).getName();
        Mockito.doReturn(ENODEB_FUNCTION_MO_TYPE).when(createEnodebFunctionMoMock).getType();
        Mockito.doReturn(VERSION_2).when(createEnodebFunctionMoMock).getVersion();

        Mockito.doReturn(NAMESPACE_1).when(nodeRootMoMock).getNamespace();
        Mockito.doReturn(ENODEB_FUNCTION_MO_TYPE).when(nodeRootMoMock).getName();
        Mockito.doReturn(ENODEB_FUNCTION_MO_TYPE).when(nodeRootMoMock).getType();
        Mockito.doReturn(VERSION_2).when(nodeRootMoMock).getVersion();

        Mockito.doReturn(nodeRootMoMock).when(liveBucketMock).findMoByFdn(ME_CONTEXT + ",ManagedElement=1");
        Mockito.doReturn(NAMESPACE_1).when(nodeRootMoMock).getNamespace();
        Mockito.doReturn(createInOtherBranchMoMock).when(mibRootBuilderMock).create();

        Mockito.doNothing().when(mockCmEventSender).sendEvent(nodeNotifications.get(0));
    }

    @SuppressWarnings("unchecked")
    private void verifyMocks() {
        Mockito.verify(updateInDpsMoMock).setAttributes(attributes);
        Mockito.verify(updateNotInDpsMoMock).setAttributes(attributes);
        Mockito.verify(createMoMock, Mockito.times(2)).setAttributes(attributes);

        Mockito.verify(createEnodebFunctionMoMock, Mockito.times(3)).getNamespace();
        Mockito.verify(createEnodebFunctionMoMock, Mockito.times(2)).getVersion();
        Mockito.verify(createInOtherBranchMoMock, Mockito.never()).setAttributes(Matchers.anyMap());

        Mockito.verify(liveBucketMock).findMoByFdn(FdnUtil.prependMeContext(ME_CONTEXT, FDN_UPDATE_DPS));
        Mockito.verify(liveBucketMock).findMoByFdn(FdnUtil.prependMeContext(ME_CONTEXT, FDN_UPDATE_NOT_DPS));
        Mockito.verify(liveBucketMock, Mockito.times(7)).findMoByFdn(Matchers.anyString());
        Mockito.verify(liveBucketMock, Mockito.times(3)).getManagedObjectBuilder();
        Mockito.verify(liveBucketMock).getMibRootBuilder();

        Mockito.verify(managedObjectBuilderMock1).parent(updateInDpsMoMock);
        Mockito.verify(managedObjectBuilderMock2).parent(updateNotInDpsMoMock);
        Mockito.verify(mibRootBuilderMock).parent(nodeRootMoMock);
    }

    private void createNotifications() {
        final NodeNotification updateInDps = new NodeNotification();
        final NodeNotification updateNotInDps = new NodeNotification();
        final NodeNotification create = new NodeNotification();
        final NodeNotification createRbsConfig = new NodeNotification();
        final NodeNotification createInOtherTreeBranch = new NodeNotification();
        final Map<String, String> parentParams = new HashMap<>();
        attributes.put("userLabel", "userLabel_on_node");

        parentParams.put("parentName", ENODEB_FUNCTION_MO_TYPE);
        parentParams.put("parentNameSpace", ENODEB_FUNCTION_MO_TYPE);
        parentParams.put("parentVersion", VERSION_2);

        updateInDps.setAction(NotificationType.UPDATE);
        updateInDps.setGenerationCounter(2L);
        updateInDps.setFdn(FDN_UPDATE_DPS);
        updateInDps.setNameSpace(NAMESPACE_1);
        updateInDps.setName(FdnUtil.extractMoType(FDN_UPDATE_DPS));
        updateInDps.setVersion(VERSION_1);
        updateInDps.setUpdateAttributes(attributes);
        updateInDps.setParentSupported(parentParams);

        updateNotInDps.setAction(NotificationType.UPDATE);
        updateNotInDps.setGenerationCounter(4L);
        updateNotInDps.setFdn(FDN_UPDATE_NOT_DPS);
        updateNotInDps.setNameSpace(NAMESPACE_1);
        updateNotInDps.setName(FdnUtil.extractMoType(FDN_UPDATE_NOT_DPS));
        updateNotInDps.setVersion(VERSION_1);
        updateNotInDps.setUpdateAttributes(attributes);
        updateNotInDps.setParentSupported(parentParams);

        create.setAction(NotificationType.CREATE);
        create.setGenerationCounter(3L);
        create.setFdn(FDN_CREATE);
        create.setNameSpace(NAMESPACE_1);
        create.setName(FdnUtil.extractMoType(FDN_CREATE));
        create.setVersion(VERSION_1);
        create.setUpdateAttributes(attributes);
        create.setParentSupported(parentParams);

        createRbsConfig.setAction(NotificationType.CREATE);
        createRbsConfig.setGenerationCounter(3L);
        createRbsConfig.setFdn(FDN_RBSCONFIG_CREATE);
        createRbsConfig.setNameSpace(NAMESPACE_1);
        createRbsConfig.setName(FdnUtil.extractMoType(FDN_RBSCONFIG_CREATE));
        createRbsConfig.setVersion(VERSION_1);
        createRbsConfig.setUpdateAttributes(attributes);
        createRbsConfig.setParentSupported(parentParams);

        createInOtherTreeBranch.setAction(NotificationType.CREATE);
        createInOtherTreeBranch.setGenerationCounter(7L);
        createInOtherTreeBranch.setFdn(FDN_OTHER_BRANCH);
        createInOtherTreeBranch.setNameSpace(NAMESPACE_2);
        createInOtherTreeBranch.setName(FdnUtil.extractMoType(FDN_OTHER_BRANCH));
        createInOtherTreeBranch.setVersion(VERSION_2);
        createInOtherTreeBranch.setUpdateAttributes(null);
        createInOtherTreeBranch.setParentSupported(parentParams);

        nodeNotifications.add(updateInDps);
        nodeNotifications.add(updateNotInDps);
        nodeNotifications.add(create);
        nodeNotifications.add(createRbsConfig);
        nodeNotifications.add(createInOtherTreeBranch);
    }

    /**
     * Answer implementation to mock effectively builder pattern invocations.
     */
    private static final class AnswerWithSelf implements Answer<Object> {
        private final Answer<Object> delegate = new ReturnsEmptyValues();
        private final Class<?> clazz;

        private AnswerWithSelf(final Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Object answer(final InvocationOnMock invocation) throws Throwable {
            final Class<?> returnType = invocation.getMethod().getReturnType();

            if (returnType.isAssignableFrom(clazz)) {
                return invocation.getMock();
            } else {
                return delegate.answer(invocation);
            }
        }
    }

}
