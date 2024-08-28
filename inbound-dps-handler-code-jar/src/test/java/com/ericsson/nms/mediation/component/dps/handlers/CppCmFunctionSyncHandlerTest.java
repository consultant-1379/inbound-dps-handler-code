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

package com.ericsson.nms.mediation.component.dps.handlers;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ME_CONTEXT_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.NETWORK_ELEMENT_MO_TYPE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.CLIENT_TYPE_HEADER_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.INVOKING_FDN_HEADER_NAME;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.operators.CmSupervisionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.NetworkElementMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.event.MediationEventSender;
import com.ericsson.nms.mediation.component.dps.instrumentation.DpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.operators.ControllerMonitorOperator;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.core.events.MediationClientType;
import com.ericsson.oss.mediation.events.SoftwareSyncStarterEvent200;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;

@RunWith(MockitoJUnitRunner.class)
public class CppCmFunctionSyncHandlerTest {
    private static final String NODE_NAME = "FooBar";
    private static final String NETWORK_ELEMENT_FDN = NETWORK_ELEMENT_MO_TYPE + "=" + NODE_NAME;
    private static final String ME_CONTEXT_FDN = ME_CONTEXT_MO_TYPE + "=" + NODE_NAME;
    private static final String CM_FUNCTION_FDN = FdnUtil.appendCmFunction(NETWORK_ELEMENT_FDN);
    private static final String CM_SUPERVISION_FDN = FdnUtil.appendCmSupervision(NETWORK_ELEMENT_FDN);
    private static final ComponentEvent INPUT_EVENT = new MediationComponentEvent(new HashMap<>(), null);

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EventHandlerContext eventHandlerContext;
    @Mock
    private SystemRecorder systemRecorderMock;
    @Mock
    private DpsHandlerInstrumentation instrumentationMock;
    @Mock
    private MediationEventSender mediationEventSenderMock;
    @Mock
    private NetworkElementMoDpsOperator networkElementMoDpsOperator;
    @Mock
    private CmSupervisionMoDpsOperator cmSupervisionMoDpsOperatorMock;
    @Mock
    private ControllerMonitorOperator controllerMonitorOperatorMock;
    @Mock
    private FailedSyncResultDpsOperator failedSyncResultDpsOperatorMock;
    @Captor
    private ArgumentCaptor<MediationTaskRequest> mtrCaptor;
    @InjectMocks
    private CppCmFunctionSyncHandler cppCmFunctionSyncHandler;

    @Before
    public void setUp() throws Exception {
        // configuration and context mocks
        final Map<String, Object> contextHeaders = new HashMap<>();
        contextHeaders.put(INVOKING_FDN_HEADER_NAME, CM_FUNCTION_FDN);
        contextHeaders.put(CLIENT_TYPE_HEADER_NAME, MediationClientType.PERSISTENCE.toString());
        when(eventHandlerContext.getEventHandlerConfiguration().getAllProperties()).thenReturn(contextHeaders);
        cppCmFunctionSyncHandler.init(eventHandlerContext);

        when(networkElementMoDpsOperator.getOssPrefix(NETWORK_ELEMENT_FDN)).thenReturn(ME_CONTEXT_FDN);
        when(cmSupervisionMoDpsOperatorMock.getActive(CM_SUPERVISION_FDN)).thenReturn(true);
    }

    @Test
    public void invalidSupervisionState() {
        when(cmSupervisionMoDpsOperatorMock.getActive(CM_SUPERVISION_FDN)).thenReturn(false);
        // executes the test, InboundDpsHandlerException should be thrown as supervision is inactive
        cppCmFunctionSyncHandler.onEvent(INPUT_EVENT);
        // the controller should handle the occurred exception then interrupt the flow execution
        verify(failedSyncResultDpsOperatorMock).handleErrorAndRethrowExceptionNoResync(
                eq(CM_FUNCTION_FDN), eq(ME_CONTEXT_FDN), anyObject(), eq(cppCmFunctionSyncHandler.handlerName));
        verifyZeroInteractions(controllerMonitorOperatorMock);
    }

    @Test
    public void softwareSyncCanProceed() {
        when(controllerMonitorOperatorMock.verifySyncStatus(anyObject(),
                eq(cppCmFunctionSyncHandler.handlerName), eq(true))).thenReturn(true);
        // executes the test
        cppCmFunctionSyncHandler.onEvent(INPUT_EVENT);
        // the controller should be able to initiate a SW sync
        verify(mediationEventSenderMock).send(mtrCaptor.capture());
        assertTrue(mtrCaptor.getValue() instanceof SoftwareSyncStarterEvent200);
    }

    @Test
    public void softwareSyncCannotProceed() {
        when(controllerMonitorOperatorMock.verifySyncStatus(anyObject(),
                eq(cppCmFunctionSyncHandler.handlerName), eq(true))).thenReturn(false);
        // executes the test
        cppCmFunctionSyncHandler.onEvent(INPUT_EVENT);
        // the controller should NOT be able to initiate a SW sync
        verifyZeroInteractions(mediationEventSenderMock);
    }
}
