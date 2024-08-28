/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.operators.dps;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.RESET_GENERATION_COUNTER_ATTR_VALUE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_RESOURCE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.EVENT_SOURCE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.operators.CmFunctionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.CmSupervisionMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.event.MediationEventSender;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.exception.SupervisionNotActiveException;
import com.ericsson.nms.mediation.component.dps.instrumentation.DpsHandlerInstrumentation;
import com.ericsson.nms.mediation.component.dps.utility.LoggingUtil;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.cm.events.SubscriptionValidationEvent;
import com.ericsson.oss.mediation.cm.events.SyncNotificationStarterEvent;

/**
 * Responsible for processing the result of a failed sync.
 */
@ApplicationScoped
public class FailedSyncResultDpsOperator {
    public static final boolean PRINT_STACK = true;
    public static final boolean SUPPRESS_STACK = false;

    protected static final int MAX_SYNC_RETRIES = 1;
    private static final String RECORDING_ERROR_SUFFIX = "_ERROR";
    private static final Logger logger = LoggerFactory.getLogger(FailedSyncResultDpsOperator.class);

    @Inject
    private SystemRecorder systemRecorder;
    @Inject
    private DpsHandlerInstrumentation instrumentation;
    @Inject
    private MediationEventSender mediationEventSender;
    @Modeled
    @Inject
    private EventSender<SubscriptionValidationEvent> subscriptionValidationEventSender;
    @Inject
    private CmFunctionMoDpsOperator cmFunctionMoDpsOperator;
    @Inject
    private CmSupervisionMoDpsOperator cmSupervisionMoDpsOperator;
    @Inject
    private CppCiMoDpsOperator cppCiMoDpsOperator;

    /**
     * Updates the DPS with sync failure information and records the error. The sync will be retried if requested and the retry limit
     * has not been exceeded. Re-sync will not be attempted in the case of a SupervisionNotActiveException.
     *
     * @param cmFunctionFdn
     *            FDN of the node for which an exception occurred
     * @param ossPrefix
     *            Oss prefix of the node for which an exception occurred
     * @param exception
     *            exception raised during the handler execution
     * @param handlerName
     *            Name of the handler where the error occurred
     * @param printStack
     *            flag to control if stack trace is printed or not
     * @param attemptResync
     *            flag to control whether a re-sync should be attempted
     */
    public void handleErrorAndRethrowException(final String cmFunctionFdn, final String ossPrefix, final Exception exception,
            final String handlerName, final boolean printStack, final boolean attemptResync) {
        resetGenerationCounter(cmFunctionFdn);

        cmFunctionMoDpsOperator.updateAttrsUponFailedSync(cmFunctionFdn, ossPrefix);

        if (attemptResync && !shouldAbortSync(exception)) {
            final int failedSyncsCount = cmFunctionMoDpsOperator.getFailedSyncsCount(cmFunctionFdn);

            if (failedSyncsCount <= MAX_SYNC_RETRIES) {
                logger.info("Node {} attempting to resubscribe after failure...", cmFunctionFdn);
                resubscribe(cmFunctionFdn);
                logger.info("Node {} attempting re-sync after failure...", cmFunctionFdn);
                resyncNode(cmFunctionFdn);
            } else {
                logger.info("Node {} will not attempt re-sync after failure. Maximum allowed sync retries({}) exceeded.", cmFunctionFdn,
                        MAX_SYNC_RETRIES);
            }
        }

        instrumentation.increaseDpsNumberOfFailedSyncs();
        recordError(cmFunctionFdn, exception, handlerName, printStack);

        throw new InboundDpsHandlerException(exception.getMessage(), exception);
    }

    /**
     * Handle exceptions occurred during execution of sync handlers, attempting re-sync if the retry limit has not been reached.<br>
     * The {@code printStack} argument controls whether the full stack trace is printed in the error log.
     *
     * @param cmFunctionFdn
     *            FDN of the node for which an exception occurred
     * @param ossPrefix
     *            Oss prefix of the node for which an exception occurred
     * @param exception
     *            exception raised during the handler execution
     * @param handlerName
     *            Name of the handler where the error occurred
     * @param printStack
     *            flag to control if stack trace is printed or not
     */
    public void handleErrorAndRethrowException(final String cmFunctionFdn, final String ossPrefix, final Exception exception,
            final String handlerName, final boolean printStack) {
        handleErrorAndRethrowException(cmFunctionFdn, ossPrefix, exception, handlerName, printStack, true);
    }

    /**
     * Handle exceptions occurred during execution of sync handlers, printing the stack trace and attempting re-sync
     * if the retry limit has not been reached.
     *
     * @param cmFunctionFdn
     *            FDN of the node for which an exception occurred
     * @param ossPrefix
     *            Oss prefix of the node for which an exception occurred
     * @param exception
     *            exception raised during the handler execution
     * @param handlerName
     *            Name of the handler where the error occurred
     */
    public void handleErrorAndRethrowException(final String cmFunctionFdn, final String ossPrefix, final Exception exception,
            final String handlerName) {
        handleErrorAndRethrowException(cmFunctionFdn, ossPrefix, exception, handlerName, PRINT_STACK, true);
    }

    /**
     * Handle exceptions occurred during execution of sync handlers, printing the stack trace and not attempting re-sync.
     *
     * @param cmFunctionFdn
     *            FDN of the node for which an exception occurred
     * @param ossPrefix
     *            Oss prefix of the node for which an exception occurred
     * @param exception
     *            exception raised during the handler execution
     * @param handlerName
     *            Name of the handler where the error occurred
     */
    public void handleErrorAndRethrowExceptionNoResync(final String cmFunctionFdn, final String ossPrefix, final Exception exception,
            final String handlerName) {
        handleErrorAndRethrowException(cmFunctionFdn, ossPrefix, exception, handlerName, PRINT_STACK, false);
    }

    public void checkHeartbeatSupervision(final String cmFunctionFdn) {
        final String cmNodeHeartbeatSupervisionFdn = FdnUtil.convertToCmNodeHeartbeatSupervision(cmFunctionFdn);
        boolean activeStatus = false;
        // TODO add a retry operator here to retry DPS operation if Optimistic Lock exception occurs
        try {
            activeStatus = cmSupervisionMoDpsOperator.getActive(cmNodeHeartbeatSupervisionFdn);
        } catch (final Exception e) {
            logger.error("Failed to get attribute 'active' for '{}'", cmNodeHeartbeatSupervisionFdn);
            throw e;
        }
        if (!activeStatus) {
            throw new SupervisionNotActiveException("Supervision is off, cannot Synchronize.");
        }
    }

    private void resetGenerationCounter(final String cmFunctionFdn) {
        final String cppCiFdn = FdnUtil.appendCppCi(FdnUtil.extractParentFdn(cmFunctionFdn));
        cppCiMoDpsOperator.setGenerationCounter(cppCiFdn, RESET_GENERATION_COUNTER_ATTR_VALUE);
    }

    private void resyncNode(final String cmFunctionFdn) {
        mediationEventSender.send(new SyncNotificationStarterEvent(cmFunctionFdn));
    }

    private void resubscribe(final String cmFunctionFdn) {
        final SubscriptionValidationEvent subscriptionEvent = new SubscriptionValidationEvent();
        final String cmNodeHeartbeatSupervisionFdn = FdnUtil.convertToCmNodeHeartbeatSupervision(cmFunctionFdn);
        subscriptionEvent.setId(cmNodeHeartbeatSupervisionFdn);
        subscriptionEvent.setSubscriptionValid(false);
        logger.info("Sending event to resubscribe for Node {}", cmNodeHeartbeatSupervisionFdn);
        subscriptionValidationEventSender.send(subscriptionEvent);
    }

    private void recordError(final String nodeFdn, final Exception exception, final String handlerName, final boolean printStack) {
        final String errorLogLine = LoggingUtil.constructErrorLogLine(handlerName, nodeFdn, exception.getMessage());
        if (printStack) {
            logger.error(errorLogLine, exception);
        }
        systemRecorder.recordError(LoggingUtil.convertToRecordingFormat(handlerName) + RECORDING_ERROR_SUFFIX, ErrorSeverity.ALERT, EVENT_SOURCE,
                EVENT_RESOURCE, errorLogLine);
    }

    private boolean shouldAbortSync(final Exception e) {
        return e instanceof SupervisionNotActiveException;
    }
}
