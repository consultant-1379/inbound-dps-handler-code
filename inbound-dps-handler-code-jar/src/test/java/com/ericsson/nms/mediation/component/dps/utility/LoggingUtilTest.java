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

package com.ericsson.nms.mediation.component.dps.utility;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.SYNC_STATUS_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ MediationComponentEvent.class })
public class LoggingUtilTest {
    private static final String HANDLER_NAME = "TEST HANDLER";
    private static final String ME_CONTEXT_FDN = "MeContext=TestNode01";
    private static final long TIME_TAKEN = 10L;
    private static final String EXCEPTION_ERROR = "This is a test Exception";
    private static final int TOTAL_MOS = 4777;
    private static final int TOTAL_ATTRIBUTES = 2444555;
    private static final int MOS_IN_DPS_PRIOR_TO_SYNC = 0;
    private static final int MOS_CREATED_IN_DPS = 10;
    private static final int MOS_DELETED_IN_DPS = 0;

    private static final String EVENT_VERSION = "1.1.1";
    private static final String EVENT_NAMESPACE = "name.space";
    private static final String EVENT_NAME = "name";

    private static final String EXAMPLE_UPPER_CAMEL_TEXT = "MyExampleString";
    private static final String EXAMPLE_UPPER_CAMEL_TEXT_FROM_INTERCEPTOR = "MyExampleString$ PROXY$ $$  WELD SUBCLASS";
    private static final String EXPECTED_UPPER_CAMEL_TEXT_CONVERTED = "MY EXAMPLE STRING";

    private static final String EXPECTED_HANDLER_NAME_RECORDING_FORMAT = "SYNC_NODE.TEST_HANDLER";
    private static final String EXPECTED_INVOCATION_LOG_LINE = "Starting " + HANDLER_NAME + " ('" + ME_CONTEXT_FDN + "')...";
    private static final String EXPECTED_TIMETAKEN_LOG_LINE = HANDLER_NAME + " ('" + ME_CONTEXT_FDN + "') took [" + TIME_TAKEN + "] ms to execute.";
    private static final String EXPECTED_ERROR_LOG_LINE = "Error in: " + HANDLER_NAME + " ('" + ME_CONTEXT_FDN + "'). Reset "
            + SYNC_STATUS_ATTR_NAME + " to '" + UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE + "'. Exception message: "
            + EXCEPTION_ERROR;

    private static final String EXPECTED_SYNC_METRICS_LOG_LINE = "Invoked Topology Write DPS operation for FDN [" + ME_CONTEXT_FDN
            + "] - (Total MOs: " + TOTAL_MOS + ", " + MOS_IN_DPS_PRIOR_TO_SYNC + " MOs read prior to sync (took " + TIME_TAKEN
            + " ms), MOs created: " + MOS_CREATED_IN_DPS
            + " (took " + TIME_TAKEN + " ms), MOs deleted: " + MOS_DELETED_IN_DPS + " (took " + TIME_TAKEN + " ms))";
    private static final String EXPECTED_ATTRIBUTES_SYNC_METRICS_LOG_LINE = "Attribute Write DPS operation for FDN ["
            + ME_CONTEXT_FDN + "] - A total of [" + TOTAL_ATTRIBUTES + "] attributes synced in DPS";
    private static final String CM_FUNCTION_FDN = "MeContext=TestNode01,CmFunction=1";
    private static final int CREATE_UPDATE_SIZE = 10;
    private static final int DELETE_SIZE = 20;
    private static final long DELTA_GC_SIZE = 2;
    private static final long CREATE_UPDATE_TIME = 33;
    private static final long DELETE_TIME = 3;
    private static final String EXPECTED_DELTA_SYNC_METRICS_LOG_LINE =
            "Invoked Delta Sync DPS operation for FDN [" + CM_FUNCTION_FDN + "] (Create/Update changes: [" + CREATE_UPDATE_SIZE
                    + "], Delete changes: [" + DELETE_SIZE + "], Create/Update time: [" + CREATE_UPDATE_TIME + "], Delete time: [" + DELETE_TIME
                    + "], deltaGC: [" + DELTA_GC_SIZE + "]).";

    @Mock
    private MediationComponentEvent mockEvent;

    @Test
    public void testConvertToRecordingFormat() {
        final String handlerNameRecordingFormat = LoggingUtil.convertToRecordingFormat(HANDLER_NAME);
        assertEquals(EXPECTED_HANDLER_NAME_RECORDING_FORMAT, handlerNameRecordingFormat);
    }

    @Test
    public void testConvertToUpperCaseWithSpaces() {
        final String convertedText = LoggingUtil.convertToUpperCaseWithSpaces(EXAMPLE_UPPER_CAMEL_TEXT);
        assertEquals(EXPECTED_UPPER_CAMEL_TEXT_CONVERTED, convertedText);
    }

    @Test
    public void testConvertToUpperCaseWithSpacesFromInterceptors() {
        final String convertedText = LoggingUtil.convertToUpperCaseWithSpaces(EXAMPLE_UPPER_CAMEL_TEXT_FROM_INTERCEPTOR);
        assertEquals(EXPECTED_UPPER_CAMEL_TEXT_CONVERTED, convertedText);
    }

    @Test
    public void testLogEventDetails() throws Exception {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("TestAttribute", "test value");

        when(mockEvent.getVersion()).thenReturn(EVENT_VERSION);
        when(mockEvent.getNamespace()).thenReturn(EVENT_NAMESPACE);
        when(mockEvent.getName()).thenReturn(EVENT_NAME);
        when(mockEvent.getHeaders()).thenReturn(headers);

        LoggingUtil.logEventDetails(mockEvent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLogEventDetails_NullException() {
        LoggingUtil.logEventDetails(null);
    }

    @Test
    public void testConstructHandlerInvocationLogLine() {
        final String logLine = LoggingUtil.constructHandlerInvocationLogLine(HANDLER_NAME, ME_CONTEXT_FDN);
        assertEquals(EXPECTED_INVOCATION_LOG_LINE, logLine);
    }

    @Test
    public void testConstructTimeTakenLogLine() {
        final String logLine = LoggingUtil.constructTimeTakenLogLine(HANDLER_NAME, ME_CONTEXT_FDN, TIME_TAKEN);
        assertEquals(EXPECTED_TIMETAKEN_LOG_LINE, logLine);
    }

    @Test
    public void testConstructErrorLogLine() {
        final String logLine = LoggingUtil.constructErrorLogLine(HANDLER_NAME, ME_CONTEXT_FDN, EXCEPTION_ERROR);
        assertEquals(EXPECTED_ERROR_LOG_LINE, logLine);
    }

    @Test
    public void testConstructSyncMetricsLogLine() {
        final Map<String, Object> syncMetricsMap = constructSyncMetrics(ME_CONTEXT_FDN, TOTAL_MOS,
                MOS_IN_DPS_PRIOR_TO_SYNC, TIME_TAKEN, MOS_CREATED_IN_DPS, TIME_TAKEN, MOS_DELETED_IN_DPS, TIME_TAKEN);

        final String logLine =
                LoggingUtil.constructSyncMetricsLogLine(syncMetricsMap);
        assertEquals(EXPECTED_SYNC_METRICS_LOG_LINE, logLine);
    }

    @Test
    public void testConstructAttributeSyncMetricsLogLine() {
        final String logLine = LoggingUtil.constructAttributeSyncMetricsLogLine(ME_CONTEXT_FDN, TOTAL_ATTRIBUTES);
        assertEquals(EXPECTED_ATTRIBUTES_SYNC_METRICS_LOG_LINE, logLine);
    }

    @Test
    public void testConstructDeltaSyncMetricsLogLine() {
        final Map<String, Object> deltaSyncMetricsMap = constructDeltaSyncMetrics(CM_FUNCTION_FDN, CREATE_UPDATE_SIZE,
                CREATE_UPDATE_TIME, DELETE_SIZE, DELETE_TIME, DELTA_GC_SIZE);
        final String logLine =
                LoggingUtil.constructDeltaSyncMetricsLogLine(deltaSyncMetricsMap);
        assertEquals(EXPECTED_DELTA_SYNC_METRICS_LOG_LINE, logLine);
    }

    private Map<String, Object> constructDeltaSyncMetrics(final String cmFunctionFdn, final int createUpdateSize, final long createUpdateTime,
            final int deleteSize,
            final long deleteTime, final long deltaGcSize) {
        final Map<String, Object> deltaSyncMetricsMap = new HashMap<>();
        deltaSyncMetricsMap.put(MiscellaneousConstants.ROOT_MO, cmFunctionFdn);
        deltaSyncMetricsMap.put(MiscellaneousConstants.NUMBER_MOS_CREATED, createUpdateSize);
        deltaSyncMetricsMap.put(MiscellaneousConstants.TIME_TAKEN_TO_CREATE_MOS, createUpdateTime);
        deltaSyncMetricsMap.put(MiscellaneousConstants.NUMBER_MOS_DELETED, deleteSize);
        deltaSyncMetricsMap.put(MiscellaneousConstants.TIME_TAKEN_TO_DELETE_MOS, deleteTime);
        deltaSyncMetricsMap.put(MiscellaneousConstants.GENERATION_COUNTER_DIFFERENCE, deltaGcSize);
        return deltaSyncMetricsMap;
    }

    @SuppressWarnings("parameternumber")
    private Map<String, Object> constructSyncMetrics(final String meContextFdn, final int totalMos, final int mosInDpsPriorToSync,
            final long timeTakenReadTopology, final int mosCreatedInDps,
            final long timeTakenToCreatMosInDps, final int mosDeletedInDps, final long timeTakenToDeleteMos) {
        final Map<String, Object> syncMetricsMap = new HashMap<>();
        syncMetricsMap.put(MiscellaneousConstants.ROOT_MO, meContextFdn);
        syncMetricsMap.put(MiscellaneousConstants.TOTAL_NUMBER_OF_MOS, totalMos);
        syncMetricsMap.put(MiscellaneousConstants.PRE_TOPOLOGY_SIZE, mosInDpsPriorToSync);
        syncMetricsMap.put(MiscellaneousConstants.TIME_TAKEN_READ_TOPOLOGY, timeTakenReadTopology);
        syncMetricsMap.put(MiscellaneousConstants.NUMBER_MOS_CREATED, mosCreatedInDps);
        syncMetricsMap.put(MiscellaneousConstants.TIME_TAKEN_TO_CREATE_MOS, timeTakenToCreatMosInDps);
        syncMetricsMap.put(MiscellaneousConstants.NUMBER_MOS_DELETED, mosDeletedInDps);
        syncMetricsMap.put(MiscellaneousConstants.TIME_TAKEN_TO_DELETE_MOS, timeTakenToDeleteMos);
        return syncMetricsMap;
    }
}
