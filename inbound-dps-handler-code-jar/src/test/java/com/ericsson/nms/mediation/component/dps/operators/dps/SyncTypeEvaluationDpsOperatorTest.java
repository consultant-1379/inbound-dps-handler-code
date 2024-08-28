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

package com.ericsson.nms.mediation.component.dps.operators.dps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ME_CONTEXT_MO_TYPE;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.RESET_GENERATION_COUNTER_ATTR_VALUE;
import static com.ericsson.nms.mediation.component.dps.operators.dps.SyncTypeEvaluationDpsOperator.COMMAND_NAME;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_RESOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_SOURCE;

import java.util.Date;

import javax.naming.InitialContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.common.SyncType;
import com.ericsson.nms.mediation.component.dps.handlers.context.SyncInvocationContext;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.network.api.MociCMConnectionProvider;
import com.ericsson.oss.mediation.network.api.exception.MociConnectionProviderException;
import com.ericsson.oss.mediation.network.api.util.ConnectionConfig;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SyncTypeEvaluationDpsOperator.class })
public class SyncTypeEvaluationDpsOperatorTest {
    // --- Constants --- //
    private static final String NODE_NAME = "VooDoo";
    private static final String ME_CONTEXT_FDN = ME_CONTEXT_MO_TYPE + "=" + NODE_NAME;
    private static final String CPP_CI_FDN = FdnUtil.getCppCiFdn(ME_CONTEXT_FDN);
    private static final Date RESTART_TIMESTAMP_ATTR_VALUE = new Date();
    private static final String IP_ADDRESS_ATTR_VALUE = "200.100.55.21";
    private static final String FULL_SYNC_MESSAGE = "Full sync was evaluated for the node %s, by pre-checks values [GC: %s, DPS Restart Timestamp: "
            + "%s, Node Restart Timestamp: %s].";
    private static final String EXCEPTION_MESSAGE = "Test exception!";
    private static final ConnectionConfig CONNECTION_DATA = new ConnectionConfig(CPP_CI_FDN, IP_ADDRESS_ATTR_VALUE);
    // --- Mocks and fields --- //
    @Mock
    private CppCiMoDpsOperator cppCiMoDpsOperatorMock;
    @Mock
    private SyncInvocationContext ctxMock;
    @Mock
    private SystemRecorder systemRecorder;
    @InjectMocks
    private SyncTypeEvaluationDpsOperator syncTypeEvalOperator;
    private MociCMConnectionProvider nodeReaderMock;

    // --- Test setup --- //
    @Before
    public void setUp() throws Exception {
        whenNew(ConnectionConfig.class).withAnyArguments().thenReturn(CONNECTION_DATA);
        when(cppCiMoDpsOperatorMock.getRestartTimestamp(CPP_CI_FDN)).thenReturn(RESTART_TIMESTAMP_ATTR_VALUE);
        when(cppCiMoDpsOperatorMock.getIpAddress(CPP_CI_FDN)).thenReturn(IP_ADDRESS_ATTR_VALUE);
        nodeReaderMock = mock(MociCMConnectionProvider.class);
        final InitialContext initialContextMock = mock(InitialContext.class);
        whenNew(InitialContext.class).withAnyArguments().thenReturn(initialContextMock);
        when(initialContextMock.lookup(MociCMConnectionProvider.VERSION_INDEPENDENT_JNDI_NAME)).thenReturn(nodeReaderMock);
        when(ctxMock.isCmFunctionSyncAction()).thenReturn(false);
        when(ctxMock.isOssModelIdentityUpdated()).thenReturn(false);
        when(ctxMock.isEventBasedClient()).thenReturn(true);
        when(ctxMock.getOssPrefix()).thenReturn(ME_CONTEXT_FDN);
        when(ctxMock.isMimSwitchPerformed()).thenReturn(false);
    }
    // --- Tests --- //

    @Test
    public void testEvaluateSyncType_isCmFunctionSyncAction() {
        when(ctxMock.isCmFunctionSyncAction()).thenReturn(true);

        final SyncType syncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);

        verifyZeroInteractions(cppCiMoDpsOperatorMock);
        verifyZeroInteractions(nodeReaderMock);
        assertSame(SyncType.FULL, syncType);
    }

    @Test
    public void testEvaluateSyncType_ossModelIdentityUpdated() {
        when(ctxMock.isOssModelIdentityUpdated()).thenReturn(true);

        final SyncType syncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);

        verifyZeroInteractions(cppCiMoDpsOperatorMock);
        verifyZeroInteractions(nodeReaderMock);
        assertSame(SyncType.FULL, syncType);
    }

    @Test
    public void testEvaluateSyncType_isMimSwitchPerfomed() {
        when(ctxMock.isMimSwitchPerformed()).thenReturn(true);

        final SyncType syncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);

        verifyZeroInteractions(cppCiMoDpsOperatorMock);
        verifyZeroInteractions(nodeReaderMock);
        assertSame(SyncType.FULL, syncType);
    }

    @Test
    public void testEvaluateSyncType_persistenceClientType() {
        when(ctxMock.isEventBasedClient()).thenReturn(false);

        final SyncType syncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);

        verifyZeroInteractions(cppCiMoDpsOperatorMock);
        verifyZeroInteractions(nodeReaderMock);
        assertSame(SyncType.FULL, syncType);
    }

    @Test
    public void testEvaluateSyncType_generationCounterIsReset() {
        when(cppCiMoDpsOperatorMock.getGenerationCounter(CPP_CI_FDN)).thenReturn(RESET_GENERATION_COUNTER_ATTR_VALUE);

        final SyncType syncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);

        verify(cppCiMoDpsOperatorMock).getGenerationCounter(CPP_CI_FDN);
        verify(cppCiMoDpsOperatorMock, never()).getRestartTimestamp(CPP_CI_FDN);
        verifyZeroInteractions(nodeReaderMock);
        assertSame(SyncType.FULL, syncType);
    }

    @Test
    public void generationCounterIsResetFullSyncReasonMessage() {
        final String expectedMessage = String.format(FULL_SYNC_MESSAGE, ctxMock.getNetworkElementFdn(),
                RESET_GENERATION_COUNTER_ATTR_VALUE, null, null);
        when(cppCiMoDpsOperatorMock.getGenerationCounter(CPP_CI_FDN)).thenReturn(RESET_GENERATION_COUNTER_ATTR_VALUE);
        final SyncType evaluatedSyncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);
        //expected sync type should be FULL
        assertEquals(evaluatedSyncType, SyncType.FULL);
        //verify that required log message is logged, with the Reason value sufficient to scenario
        verify(systemRecorder).recordEvent(COMMAND_NAME, EventLevel.DETAILED, EVENT_SOURCE, EVENT_RESOURCE, expectedMessage);
    }

    @Test
    public void testEvaluateSyncType_restartDateIsNull() {
        when(cppCiMoDpsOperatorMock.getRestartTimestamp(CPP_CI_FDN)).thenReturn(null);

        final SyncType syncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);

        verify(cppCiMoDpsOperatorMock).getGenerationCounter(CPP_CI_FDN);
        verify(cppCiMoDpsOperatorMock).getRestartTimestamp(CPP_CI_FDN);
        verifyZeroInteractions(nodeReaderMock);
        assertSame(SyncType.FULL, syncType);
    }

    @Test
    public void restartDateIsNullFullSyncReasonMessage() {
        final String expectedMessage = String.format(FULL_SYNC_MESSAGE, ctxMock.getNetworkElementFdn(),
                cppCiMoDpsOperatorMock.getGenerationCounter(CPP_CI_FDN), null, null);
        when(cppCiMoDpsOperatorMock.getRestartTimestamp(CPP_CI_FDN)).thenReturn(null);
        final SyncType evaluatedSyncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);
        //expected sync type should be FULL
        assertEquals(evaluatedSyncType, SyncType.FULL);
        //verify that required log message is logged, with the Reason value sufficient to scenario
        verify(systemRecorder).recordEvent(COMMAND_NAME, EventLevel.DETAILED, EVENT_SOURCE, EVENT_RESOURCE, expectedMessage);
    }

    @Test
    public void testEvaluateSyncType_restartDatesMismatch() throws MociConnectionProviderException {
        when(cppCiMoDpsOperatorMock.getRestartTimestamp(CPP_CI_FDN)).thenReturn(new Date());

        final SyncType syncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);

        verify(cppCiMoDpsOperatorMock).getGenerationCounter(CPP_CI_FDN);
        verify(cppCiMoDpsOperatorMock).getRestartTimestamp(CPP_CI_FDN);
        verify(nodeReaderMock).getRestartNodeDate(CONNECTION_DATA);
        assertSame(SyncType.FULL, syncType);
    }

    @Test
    public void restartDatesMismatchFullSyncReasonMessage() throws MociConnectionProviderException {
        final Date nodeRestartDate = new Date();
        final Date dpsRestartDate = new Date(nodeRestartDate.getTime() - 5000);
        final String expectedMessage = String.format(FULL_SYNC_MESSAGE, ctxMock.getNetworkElementFdn(),
                cppCiMoDpsOperatorMock.getGenerationCounter(CPP_CI_FDN), dpsRestartDate, nodeRestartDate);
        when(nodeReaderMock.getRestartNodeDate(CONNECTION_DATA)).thenReturn(nodeRestartDate);
        when(cppCiMoDpsOperatorMock.getRestartTimestamp(CPP_CI_FDN)).thenReturn(dpsRestartDate);
        final SyncType evaluatedSyncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);
        //expected sync type should be FULL
        assertEquals(evaluatedSyncType, SyncType.FULL);
        //verify that required log message is logged, with the Reason value sufficient to scenario
        verify(systemRecorder).recordEvent(COMMAND_NAME, EventLevel.DETAILED, EVENT_SOURCE, EVENT_RESOURCE, expectedMessage);
    }

    @Test
    public void testEvaluateSyncType_deltaSyncTypeResolved() throws MociConnectionProviderException {
        when(nodeReaderMock.getRestartNodeDate(CONNECTION_DATA)).thenReturn(RESTART_TIMESTAMP_ATTR_VALUE);

        final SyncType syncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);

        verify(cppCiMoDpsOperatorMock).getGenerationCounter(CPP_CI_FDN);
        verify(cppCiMoDpsOperatorMock).getRestartTimestamp(CPP_CI_FDN);
        verify(nodeReaderMock).getRestartNodeDate(CONNECTION_DATA);
        assertSame(SyncType.DELTA, syncType);
    }

    @Test
    public void testEvaluateSyncType_catchesException() throws MociConnectionProviderException {
        when(nodeReaderMock.getRestartNodeDate(CONNECTION_DATA)).thenThrow(new MociConnectionProviderException(EXCEPTION_MESSAGE));

        final SyncType syncType = syncTypeEvalOperator.evaluateSyncType(ctxMock);

        assertSame(SyncType.FULL, syncType);
    }
}