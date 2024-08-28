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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.DELTA_SYNC_CHANGES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.handlers.payload.DeltaSyncDpsPayload;
import com.ericsson.nms.mediation.component.dps.instrumentation.DpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.operators.dps.FailedSyncResultDpsOperator;
import com.ericsson.nms.mediation.component.dps.utility.InstrumentationUtil;
import com.ericsson.nms.mediation.component.dps.utility.LoggingUtil;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.ericsson.oss.mediation.network.api.notifications.NotificationType;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ LoggingUtil.class, InstrumentationUtil.class })
public class DeltaMergerHandlerTest {
    private static final String HANDLER_NAME = "DELTA SYNC MERGER HANDLER";
    private static final String CM_FUNCTION_FDN = "NetworkElement=Erbs01,CmFunction=1";
    private static final String TEST_FDN_1 = "NetworkElement=Erbs01,ManagedElement=1,ENodeBFunction=1,AnrFunction=1";
    private static final String TEST_FDN_2 = "NetworkElement=Erbs01,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=LTE07ERBS00001-5,"
            + "Cdma20001xRttBandRelation=1";
    private static final String TEST_FDN_1_CHILD = "NetworkElement=Erbs01,ManagedElement=1,ENodeBFunction=1,AnrFunction=1,AnrFunctionGeran=1";
    private static final String EXCEPTION_MESSAGE = "Test exception!";
    private static final Long GENERATION_COUNTER_FDN_1 = 10L;
    private static final Long GENERATION_COUNTER_FDN_2 = 20L;
    private static final Long GENERATION_COUNTER_FDN_3 = 15L;

    private static final String INVOCATION_LOG_LINE = "Starting " + HANDLER_NAME + "...";
    private static final long TIME_TAKEN = 60000L;
    private static final String TIME_TAKEN_LOG_LINE = HANDLER_NAME + " took " + TIME_TAKEN + " ms.";

    private Map<String, Map<String, Object>> payload;
    private Map<String, Object> attributesFdn1;
    private Map<String, Object> attributesFdn2;
    private Collection<NodeNotification> deltaSyncChanges;

    private NodeNotification nodeChangeOne;
    private NodeNotification nodeChangeTwo;
    private NodeNotification nodeChangeThree;

    @Mock
    private ComponentEvent eventMock;
    @Mock
    private EventHandlerContext contextMock;
    @Mock
    private SystemRecorder systemRecorderMock;
    @Mock
    private DpsHandlerInstrumentation instrumentationMock;
    @Mock
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperatorMock;
    @Mock
    private FailedSyncResultDpsOperator failedSyncResultDpsOperatorMock;

    @InjectMocks
    private DeltaMergerHandler deltaMergerHandler;

    @Before
    public void setUp() throws Exception {
        // set up delta sync changes
        deltaSyncChanges = new ArrayList<>();

        // set up payload
        setupPayload();

        // set up headers
        final Map<String, Object> deltaSyncMergerHandlerHeaders = new HashMap<>();
        deltaSyncMergerHandlerHeaders.put(DeltaMergerHandler.class.getCanonicalName(), new HashMap<>());
        deltaSyncMergerHandlerHeaders.put(MiscellaneousConstants.INVOKING_FDN_HEADER_NAME, CM_FUNCTION_FDN);
        deltaSyncMergerHandlerHeaders.put(DELTA_SYNC_CHANGES, deltaSyncChanges);
        when(eventMock.getHeaders()).thenReturn(deltaSyncMergerHandlerHeaders);

        // static mocks
        mockStatic(LoggingUtil.class);
        when(LoggingUtil.constructHandlerInvocationLogLine(HANDLER_NAME, CM_FUNCTION_FDN)).thenReturn(INVOCATION_LOG_LINE);
        when(LoggingUtil.constructTimeTakenLogLine(HANDLER_NAME, CM_FUNCTION_FDN, TIME_TAKEN)).thenReturn(TIME_TAKEN_LOG_LINE);
        mockStatic(InstrumentationUtil.class);
        when(InstrumentationUtil.calculateTimeTaken(anyLong())).thenReturn(TIME_TAKEN);
    }

    @Test
    public void testInitHandler() {
        deltaMergerHandler.initHandler(contextMock);
    }

    @Test
    public void testOnEvent_CreateType() {
        setUpChanges(NotificationType.CREATE);

        final ComponentEvent result = deltaMergerHandler.onEvent(eventMock);
        final DeltaSyncDpsPayload resultPayload = (DeltaSyncDpsPayload) result.getPayload();
        assertEquals(GENERATION_COUNTER_FDN_2, resultPayload.getGenerationCounter());
        assertTrue(resultPayload.getDeleteChanges().isEmpty());
        assertEquals(3, resultPayload.getCreateAndUpdateChanges().size());

        // Checking order
        final Collection<NodeNotification> createUpdates = resultPayload.getCreateAndUpdateChanges();
        final Object[] createUpdatesArray = createUpdates.toArray();
        assertEquals(TEST_FDN_1, ((NodeNotification) createUpdatesArray[0]).getFdn());
        assertEquals(TEST_FDN_1_CHILD, ((NodeNotification) createUpdatesArray[1]).getFdn());
        assertEquals(TEST_FDN_2, ((NodeNotification) createUpdatesArray[2]).getFdn());
    }

    @Test
    public void testOnEvent_UpdateType() {
        setUpChanges(NotificationType.UPDATE);

        final ComponentEvent result = deltaMergerHandler.onEvent(eventMock);
        final DeltaSyncDpsPayload resultPayload = (DeltaSyncDpsPayload) result.getPayload();
        assertEquals(GENERATION_COUNTER_FDN_2, resultPayload.getGenerationCounter());
        assertTrue(resultPayload.getDeleteChanges().isEmpty());
        assertEquals(3, resultPayload.getCreateAndUpdateChanges().size());

        // Checking order
        final Collection<NodeNotification> createUpdates = resultPayload.getCreateAndUpdateChanges();
        final Object[] createUpdatesArray = createUpdates.toArray();
        assertEquals(TEST_FDN_1, ((NodeNotification) createUpdatesArray[0]).getFdn());
        assertEquals(TEST_FDN_1_CHILD, ((NodeNotification) createUpdatesArray[1]).getFdn());
        assertEquals(TEST_FDN_2, ((NodeNotification) createUpdatesArray[2]).getFdn());
    }

    @Test
    public void testOnEvent_DeleteType() {
        setUpChanges(NotificationType.DELETE);
        final ComponentEvent result = deltaMergerHandler.onEvent(eventMock);
        final DeltaSyncDpsPayload resultPayload = (DeltaSyncDpsPayload) result.getPayload();
        assertEquals(GENERATION_COUNTER_FDN_2, resultPayload.getGenerationCounter());
        assertTrue(resultPayload.getCreateAndUpdateChanges().isEmpty());
        assertEquals(3, resultPayload.getDeleteChanges().size());

        // Checking order
        final Collection<NodeNotification> deletes = resultPayload.getDeleteChanges();
        final Object[] deletesArray = deletes.toArray();
        assertEquals(TEST_FDN_2, ((NodeNotification) deletesArray[0]).getFdn());
        assertEquals(TEST_FDN_1_CHILD, ((NodeNotification) deletesArray[1]).getFdn());
        assertEquals(TEST_FDN_1, ((NodeNotification) deletesArray[2]).getFdn());
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testOnEvent_PayloadIsException() {
        when(eventMock.getPayload()).thenReturn(new Exception());
        doThrow(new InboundDpsHandlerException(EXCEPTION_MESSAGE)).when(failedSyncResultDpsOperatorMock).handleErrorAndRethrowException(anyString(),
                anyString(), any(Exception.class), anyString());

        deltaMergerHandler.onEvent(eventMock);
        verify(cmFunctionMoDpsOperatorMock, never()).setSyncStatus(CM_FUNCTION_FDN, SYNCHRONIZED_SYNC_STATUS_ATTR_VALUE);
    }

    @Test
    public void testOnEvent_PayloadIsEmpty() {
        when(eventMock.getPayload()).thenReturn(new HashMap<String, Map<String, Object>>());

        final ComponentEvent result = deltaMergerHandler.onEvent(eventMock);
        final DeltaSyncDpsPayload resultPayload = (DeltaSyncDpsPayload) result.getPayload();
        assertTrue(resultPayload.getCreateAndUpdateChanges().isEmpty());
        assertTrue(resultPayload.getDeleteChanges().isEmpty());
    }

    private void setupPayload() {
        payload = new HashMap<>();
        attributesFdn1 = new HashMap<>();
        attributesFdn1.put("prioTime", 80L);
        attributesFdn1.put("prioHoSuccRate", 50L);
        attributesFdn1.put("hoAllowedEutranPolicy", false);

        attributesFdn2 = new HashMap<>();
        attributesFdn2.put("userLabel", "");
        attributesFdn2.put("cellReselectionPriority4", 4L);
        attributesFdn2.put("cdma2000FreqBandRef", 4L);

        payload.put(TEST_FDN_1, attributesFdn1);
        payload.put(TEST_FDN_2, attributesFdn2);
        when(eventMock.getPayload()).thenReturn(payload);
    }

    private void setUpChanges(final NotificationType changeType) {
        nodeChangeOne = new NodeNotification();
        nodeChangeOne.setAction(changeType);
        nodeChangeOne.setFdn(TEST_FDN_1);
        nodeChangeOne.setGenerationCounter(GENERATION_COUNTER_FDN_1);

        nodeChangeTwo = new NodeNotification();
        nodeChangeTwo.setAction(changeType);
        nodeChangeTwo.setFdn(TEST_FDN_2);
        nodeChangeTwo.setGenerationCounter(GENERATION_COUNTER_FDN_2);

        nodeChangeThree = new NodeNotification();
        nodeChangeThree.setAction(changeType);
        nodeChangeThree.setFdn(TEST_FDN_1_CHILD);
        nodeChangeThree.setGenerationCounter(GENERATION_COUNTER_FDN_3);

        deltaSyncChanges.add(nodeChangeOne);
        deltaSyncChanges.add(nodeChangeTwo);
        deltaSyncChanges.add(nodeChangeThree);
    }
}
