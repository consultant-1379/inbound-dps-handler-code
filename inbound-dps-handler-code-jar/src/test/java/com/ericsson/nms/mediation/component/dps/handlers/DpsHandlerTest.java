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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.instrumentation.DpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.utility.InstrumentationUtil;
import com.ericsson.nms.mediation.component.dps.utility.LoggingUtil;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

/**
 * The DpsHandlerTest class.
 * <p> Mock methods from multiple class using PowerMock
 * InstrumentationUtil.class is prepared for this test</p>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ InstrumentationUtil.class })
public class DpsHandlerTest {
    private static final String NODE_NAME = "VooDoo";
    private static final String CM_FUNCTION_FDN = FdnUtil.appendCmFunction(FdnUtil.getNetworkElementFdn(NODE_NAME));
    private static final String HANDLER_NAME = "PLAIN DPS HANDLER";
    private static final String HANDLER_NAME_RECORDING_FORMAT = "SYNC_NODE.PLAIN_DPS_HANDLER";

    private static final long START_TIME = 10L;
    private static final long TIME_TAKEN = 1234L;

    private static final String INVOCATION_LOG_LINE = LoggingUtil.constructHandlerInvocationLogLine(HANDLER_NAME, CM_FUNCTION_FDN);
    private static final String TIME_TAKEN_LOG_LINE = LoggingUtil.constructTimeTakenLogLine(HANDLER_NAME, CM_FUNCTION_FDN, TIME_TAKEN);

    @Mock
    private EventHandlerContext contextMock;
    @Mock
    private ComponentEvent eventMock;
    @Mock
    private Exception mockException;
    @Mock
    private SystemRecorder systemRecorderMock;
    @Mock
    private DpsHandlerInstrumentation instrumentationMock;
    @Mock
    private CmFunctionMoDpsOperator failedSyncResultDpsOperatorMock;

    @InjectMocks
    private final DpsHandler dpsHandler = new PlainDpsHandler();

    @Before
    public void setUp() throws Exception {
        dpsHandler.init(contextMock);
    }

    @Test
    public void testDestroy() {
        dpsHandler.destroy();
    }

    @Test
    public void testRecordInvocationDetails() throws Exception {
        dpsHandler.recordInvocationDetails(CM_FUNCTION_FDN, eventMock);
        verify(systemRecorderMock).recordEvent(HANDLER_NAME_RECORDING_FORMAT, EventLevel.DETAILED, MiscellaneousConstants.EVENT_SOURCE,
                MiscellaneousConstants.EVENT_RESOURCE, INVOCATION_LOG_LINE);
    }

    @Test
    public void testRecordTimeTaken() throws Exception {
        mockStatic(InstrumentationUtil.class);
        when(InstrumentationUtil.calculateTimeTaken(START_TIME)).thenReturn(TIME_TAKEN);

        dpsHandler.recordTimeTaken(CM_FUNCTION_FDN, START_TIME);
        verify(systemRecorderMock).recordEvent(HANDLER_NAME_RECORDING_FORMAT, EventLevel.DETAILED, MiscellaneousConstants.EVENT_SOURCE,
                MiscellaneousConstants.EVENT_RESOURCE, TIME_TAKEN_LOG_LINE);
    }

    private class PlainDpsHandler extends DpsHandler {
        @Override
        public ComponentEvent onEvent(final ComponentEvent inputEvent) {
            return null;
        }

        @Override
        protected void initHandler(final EventHandlerContext eventHandlerContext) {}
    }

}
