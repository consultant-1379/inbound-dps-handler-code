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

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.SYNC_STATUS_ATTR_NAME;
import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.google.common.base.CaseFormat;

/**
 * Provides logging functionality common between all or some of the handlers.
 */
public final class LoggingUtil {

    private static final String RECORDING_FORMAT_PREFIX = "SYNC_NODE.";

    private static final Logger logger = LoggerFactory.getLogger(LoggingUtil.class);

    private LoggingUtil() {}

    /**
     * Converts passed text fragment to the format that Service Framework Recording API expects.
     * <p><b>Example Output Format:</b> "SYNC_NODE.CONVERTED_HANDLER_NAME"</p>
     *
     * @param text
     *            text fragment to convert
     * @return converted text fragment according to the above format
     *         converted text fragment according to the above format
     */
    public static String convertToRecordingFormat(final String text) {
        return RECORDING_FORMAT_PREFIX + text.replace(" ", "_").toUpperCase();
    }

    /**
     * Converts upper-camel-case string to the upper-case string where words are separated with spaces.
     * <p> <b>Example:</b> {@literal "MyExampleString" -> "MY EXAMPLE STRING"} </p>
     *
     * @param upperCamelCaseText
     *            string to be converted
     * @return string converted to upper-case where words are separated with spaces
     *         string converted to upper-case where words are separated with spaces
     */
    public static String convertToUpperCaseWithSpaces(final String upperCamelCaseText) {
        String localUpperCamelCaseText = upperCamelCaseText;
        if (upperCamelCaseText.contains("$")) {
            localUpperCamelCaseText = upperCamelCaseText.substring(0, upperCamelCaseText.indexOf('$'));
        }
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, localUpperCamelCaseText).replace('_', ' ');
    }

    /**
     * Logs attribute details for the <code>ComponentEvent</code> passed.
     *
     * @param event
     *            event for which the attributes are to be logged
     * @throws IllegalArgumentException
     *             if <code>event</code> is null
     */
    public static void logEventDetails(final ComponentEvent event) throws IllegalArgumentException {
        if (event == null) {
            throw new IllegalArgumentException("ComponentEvent passed cannot be null.");
        }

        logger.trace("Event version: '{}', namespace: '{}', class: '{}'", event.getVersion(), event.getNamespace(), event.getClass().getSimpleName());

        logger.trace("=====> Event headers ('{}'): <=====", event.getName());
        for (final Entry<String, Object> entry : event.getHeaders().entrySet()) {
            logger.trace("--> '{}': '{}'", entry.getKey(), entry.getValue());
        }
    }

    /**
     * Creates a specific log line indicating an invocation of a handler.
     * <p>
     * <b>Output Format:</b> "Starting <i>handler</i> (<i>FDN</i>)..."
     *
     * @param handlerName
     *            name of the handler to be included in the log line
     * @param fdn
     *            FDN identifying the node for which the log line is being constructed
     * @return constructed log line
     */
    public static String constructHandlerInvocationLogLine(final String handlerName, final String fdn) {
        final StringBuilder logLine = new StringBuilder("Starting ")
                .append(handlerName)
                .append(" ('")
                .append(fdn)
                .append("')...");
        return logLine.toString();
    }

    /**
     * Creates a specific log line for the time taken to execute for a handler or a specific operation (i.e. complete sync).
     * <p>
     * <b>Output Format:</b> "<i>operation</i> (<i>FDN</i>) took <i>x</i> ms to execute."
     *
     * @param operationName
     *            name of the operation for which the total time is calculated (could be simply the name of the handler)
     * @param fdn
     *            FDN identifying the node for which the log line is being constructed
     * @param timeTaken
     *            time to be included in the log line
     * @return constructed log line
     */
    public static String constructTimeTakenLogLine(final String operationName, final String fdn, final long timeTaken) {
        final StringBuilder logLine = new StringBuilder(operationName)
                .append(" ('")
                .append(fdn)
                .append("') took [")
                .append(timeTaken)
                .append("] ms to execute.");
        return logLine.toString();
    }

    /**
     * Creates a specific log line for an error that occurred during execution of a handler, including the information about resetting
     * <code>syncStatus</code>.
     * <p>
     * <b>Output Format:</b> "Error in: <i>handler</i> (<i>FDN</i>). Reset syncStatus to <i>UNSYNCHRONIZED</i>. Exception message: <i>message</i>"
     *
     * @param handlerName
     *            name of the handler to be included in the log line
     * @param fdn
     *            FDN identifying the node for which the log line is being constructed
     * @param exceptionMessage
     *            message for the exception thrown to be included in the log line
     * @return constructed log line
     */
    public static String constructErrorLogLine(final String handlerName, final String fdn,
            final String exceptionMessage) {
        final StringBuilder logLine = new StringBuilder("Error in: ")
                .append(handlerName)
                .append(" ('")
                .append(fdn)
                .append("'). Reset ")
                .append(SYNC_STATUS_ATTR_NAME)
                .append(" to '")
                .append(UNSYNCHRONIZED_SYNC_STATUS_ATTR_VALUE)
                .append("'. Exception message: ")
                .append(exceptionMessage);
        return logLine.toString();
    }

    /**
     * Creates a specific log line for the Topology sync metrics.
     *
     * @param syncMetricsMap
     *            contains rootMoFdn, fdn of the rootMo to be included in the log line totalElements, total number of MOs in network
     *            topology represented in DPS preSyncTopologySize, topology size before sync topologyReadTime, time taken to read the topology
     *            totalMosCreated, number of mos created in DPS during topology sync createTime, time taken to create the mos in DPS
     *            mosToBeDeletedFromDpsSize, number of mos deleted in DPS during topology sync deleteTime, time taken to delete the mos in DPS.
     * @return constructed log line
     */
    public static String constructSyncMetricsLogLine(final Map<String, Object> syncMetricsMap) {
        final StringBuilder logLine = new StringBuilder("Invoked Topology Write DPS operation for FDN [")
                .append(syncMetricsMap.get(MiscellaneousConstants.ROOT_MO))
                .append("] - (Total MOs: ")
                .append(syncMetricsMap.get(MiscellaneousConstants.TOTAL_NUMBER_OF_MOS))
                .append(", ")
                .append(syncMetricsMap.get(MiscellaneousConstants.PRE_TOPOLOGY_SIZE))
                .append(" MOs read prior to sync (took ")
                .append(syncMetricsMap.get(MiscellaneousConstants.TIME_TAKEN_READ_TOPOLOGY))
                .append(" ms), MOs created: ")
                .append(syncMetricsMap.get(MiscellaneousConstants.NUMBER_MOS_CREATED))
                .append(" (took ")
                .append(syncMetricsMap.get(MiscellaneousConstants.TIME_TAKEN_TO_CREATE_MOS))
                .append(" ms), MOs deleted: ")
                .append(syncMetricsMap.get(MiscellaneousConstants.NUMBER_MOS_DELETED))
                .append(" (took ")
                .append(syncMetricsMap.get(MiscellaneousConstants.TIME_TAKEN_TO_DELETE_MOS))
                .append(" ms))");
        return logLine.toString();
    }

    /**
     * Creates a specific log line for the Attribute sync metrics.
     *
     * @param fdn
     *            FDN identifying the node for which the log line is being constructed
     * @param totalAttributes
     *            total number of Attributes in network topology represented in DPS
     * @return constructed log line
     */
    public static String constructAttributeSyncMetricsLogLine(final String fdn, final int totalAttributes) {
        final StringBuilder logLine = new StringBuilder("Attribute Write DPS operation for FDN [")
                .append(fdn)
                .append("] - A total of [")
                .append(totalAttributes)
                .append("] attributes synced in DPS");
        return logLine.toString();
    }

    /**
     * Creates a specific log line for the delta sync metrics.
     *
     * @param deltaSyncMetricsMap
     *            contains cmFunctionFdn, fdn for rootMo createUpdateChangeSize, number of create/update changes
     *            deleteChangeSize, number of delete changes generationCounterDifference, difference between max GC from events and GC in DPS
     * @return constructed log line
     */
    public static String constructDeltaSyncMetricsLogLine(final Map<String, Object> deltaSyncMetricsMap) {
        final StringBuilder logLine = new StringBuilder("Invoked Delta Sync DPS operation for FDN [")
                .append(deltaSyncMetricsMap.get(MiscellaneousConstants.ROOT_MO))
                .append("] (Create/Update changes: [")
                .append(deltaSyncMetricsMap.get(MiscellaneousConstants.NUMBER_MOS_CREATED))
                .append("], Delete changes: [")
                .append(deltaSyncMetricsMap.get(MiscellaneousConstants.NUMBER_MOS_DELETED))
                .append("], Create/Update time: [")
                .append(deltaSyncMetricsMap.get(MiscellaneousConstants.TIME_TAKEN_TO_CREATE_MOS))
                .append("], Delete time: [")
                .append(deltaSyncMetricsMap.get(MiscellaneousConstants.TIME_TAKEN_TO_DELETE_MOS))
                .append("], deltaGC: [")
                .append(deltaSyncMetricsMap.get(MiscellaneousConstants.GENERATION_COUNTER_DIFFERENCE))
                .append("]).");
        return logLine.toString();
    }
}
