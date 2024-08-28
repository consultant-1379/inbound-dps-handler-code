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

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.*;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.utility.InstrumentationUtil;
import com.ericsson.nms.mediation.component.dps.utility.LoggingUtil;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.TypedEventInputHandler;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

/**
 * Parent of DPS handlers.
 */
public abstract class DpsHandler implements TypedEventInputHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    protected SystemRecorder systemRecorder;
    @Inject
    protected DpsHandlerInstrumentation instrumentation;
    @Inject
    protected CmFunctionMoDpsOperator cmFunctionMoDpsOperator;

    protected String handlerName;

    /**
     * Performs handler initialization steps. Implement this method in descendants of this class instead of overriding <code>init()</code>.
     * @param eventHandlerContext
     *            non-null event context instance
     */
    protected abstract void initHandler(final EventHandlerContext eventHandlerContext);

    /**
     * Template for child's init(). Concrete handler implementation has to implement <code>initHandler()</code> only.
     *
     * @see <code>initHandler()</code>
     */
    @Override
    public void init(final EventHandlerContext eventHandlerContext) {
        handlerName = LoggingUtil.convertToUpperCaseWithSpaces(getClass().getSimpleName());
        logger.trace("Initializing '{}'...", handlerName);

        initHandler(eventHandlerContext);

        logger.trace("Initialized '{}'.", handlerName);
    }

    @Override
    public void destroy() {
        logger.trace("'{}' instance destroyed.", handlerName);
    }

    protected void recordOverallSyncStatistics(final String cmFunctionFdn, final Long startTimeForFdn, final long startTime) {
        final Long overallSyncTimeTaken = InstrumentationUtil.calculateTimeTaken(startTimeForFdn);

        instrumentation.addToAverageDpsAttributeDataTimesTaken(InstrumentationUtil.calculateTimeTaken(startTime));
        instrumentation.addToAverageOverallSyncTimesTaken(overallSyncTimeTaken);
        instrumentation.increaseDpsCounterForSuccessfulSync();
        instrumentation.addToMaxOverallSyncTimesTaken(overallSyncTimeTaken);

        final String overallSyncTimeTakenLogLine = LoggingUtil.constructTimeTakenLogLine(COMPLETE_SYNC_LABEL, cmFunctionFdn, overallSyncTimeTaken);
        logger.debug(overallSyncTimeTakenLogLine);
        systemRecorder.recordEvent(LoggingUtil.convertToRecordingFormat(handlerName + "_" + COMPLETE_SYNC_LABEL), EventLevel.DETAILED, EVENT_SOURCE,
                EVENT_RESOURCE, overallSyncTimeTakenLogLine);
    }

    /**
     * Logs and records information about the time it took the handler to do its work.
     * Note : DDP is Parsing this data and DDP team need to be notified of changes
     *
     * @param rootMoFdn
     *            Root of the topology tree for example: MeContext=LTE001
     * @param totalAttributes
     *            Total number of attributes on a Network Node represented in DPS
     */
    protected void recordTotalAttributesCount(final String rootMoFdn, final int totalAttributes) {
        final Map<String, Object> eventData = new HashMap<>(4);
        eventData.put("EVENT_LEVEL", EventLevel.DETAILED);
        eventData.put("EVENT_SOURCE", EVENT_SOURCE);
        eventData.put("EVENT_RESOURCE", EVENT_RESOURCE);
        eventData.put("EVENT_INFO", LoggingUtil.constructAttributeSyncMetricsLogLine(rootMoFdn, totalAttributes));

        systemRecorder.recordEventData(LoggingUtil.convertToRecordingFormat(HANDLER_NAME_ATTRIBUTE_SYNC), eventData);
    }

    /**
     * Logs and records the information about handler's start-up and the corresponding input data details.
     *
     * @param nodeFdn
     *            FDN identifying the node (i.e. NetworkElementFdn or any of its children FDNs)
     * @param event
     *            handler's input data
     */
    protected void recordInvocationDetails(final String nodeFdn, final ComponentEvent event) {
        final String invocationLogLine = LoggingUtil.constructHandlerInvocationLogLine(handlerName, nodeFdn);
        logger.debug(invocationLogLine);
        systemRecorder.recordEvent(LoggingUtil.convertToRecordingFormat(handlerName), EventLevel.DETAILED, EVENT_SOURCE, EVENT_RESOURCE,
                invocationLogLine);
        LoggingUtil.logEventDetails(event);
    }

    /**
     * Logs and records information about the time it took the handler to do its work.
     *
     * @param nodeFdn
     *            FDN identifying the node (i.e. NetworkElementFdn or any of its children FDNs)
     * @param startTime
     *            time at which the handler started its work
     */
    protected void recordTimeTaken(final String nodeFdn, final long startTime) {
        final long timeTaken = InstrumentationUtil.calculateTimeTaken(startTime);
        final String timeTakenLogLine = LoggingUtil.constructTimeTakenLogLine(handlerName, nodeFdn, timeTaken);
        logger.debug(timeTakenLogLine);
        systemRecorder.recordEvent(LoggingUtil.convertToRecordingFormat(handlerName), EventLevel.DETAILED, EVENT_SOURCE, EVENT_RESOURCE,
                timeTakenLogLine);
    }

    /**
     * Checks previous handler did not throw an exception. If no exception do nothing, otherwise throw an <code>InboundDpsHandlerException</code>.
     *
     * @param eventPayload
     *            Event payload passed into the handler
     */
    protected void rethrowExceptionIfOneFoundInPayload(final Object eventPayload) {
        if (eventPayload instanceof Exception) {
            throw new InboundDpsHandlerException("Exception has been discovered in the event payload: " + ((Exception) eventPayload).getMessage());
        }
    }
}
