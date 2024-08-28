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

import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.NODE_TYPE_HEADER_NAME;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.GENERATION_COUNTER_HEADER_NAME;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.RESTART_NODE_DATE_HEADER_NAME;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.handlers.algorithms.AttributeSyncAlgorithm;
import com.ericsson.nms.mediation.component.dps.operators.DpsRetryExceptionHandler;
import com.ericsson.nms.mediation.component.dps.operators.RetryOperator;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;

/**
 * Persists node attributes in DPS. Node attributes are initially extracted using Sync Node MOCI handler.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AttributeWriteDpsOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeWriteDpsOperator.class);
    private static final long DEFAULT_DPS_TX_SIZE_LIMIT = 1_000_000L;

    @Inject
    private RetryOperator retryOperator;
    @Inject
    private AttributeSyncAlgorithm attributeSyncAlgorithm;

    @SuppressWarnings("unchecked")
    public int handleAttributesPersistence(final ComponentEvent event, final String rootFdn, final boolean isFirstSync) {
        LOGGER.debug("Invoking Attribute Write DPS operation for FDN [{}]", rootFdn);

        final String cppCiFdn = FdnUtil.getCppCiFdn(rootFdn);

        //Update generation in CppConnectivityInfo MO
        final long generationCounter = (long) event.getHeaders().get(GENERATION_COUNTER_HEADER_NAME);
        retryOperator.updateGc(cppCiFdn, generationCounter);

        //Update neType in ManagedElement MO
        final String neType = (String) event.getHeaders().get(NODE_TYPE_HEADER_NAME);
        retryOperator.setNeTypeAttribute(rootFdn, neType);

        //Set the restart timestamp
        final Date restartTimestamp = (Date) event.getHeaders().get(RESTART_NODE_DATE_HEADER_NAME);
        retryOperator.setRestartTimestamp(cppCiFdn, restartTimestamp);

        //Update MO attributes
        final Map<String, Map<String, Object>> attributesMap = (Map<String, Map<String, Object>>) event.getPayload();

        final int persistedAttributeCount =  persistAttributes(rootFdn, attributesMap, isFirstSync);

        LOGGER.debug("A total of {} attributes have been persisted for {}.", persistedAttributeCount, rootFdn);
        return persistedAttributeCount;
    }

    private int persistAttributes(final String rootFdn, Map<String, Map<String, Object>> mainAttributesMap, final boolean isFirstSync) {
        int retriesCount = 0;
        int totalPersistedAttributes = 0;
        long attributesBatchSize = calcAttributesBatchSize(MiscellaneousConstants.DEFAULT_ATTRIBUTES_BATCH_SIZE_FACTOR);
        while (!mainAttributesMap.isEmpty()) {
            try {
                final Map<String, Map<String, Object>> mutableAttributesMap = new HashMap<>(mainAttributesMap);
                totalPersistedAttributes += persistAttributesBatch(rootFdn, mutableAttributesMap, attributesBatchSize, isFirstSync);
                mainAttributesMap = mutableAttributesMap;
            } catch (final RuntimeException e) {
                retriesCount++;
                if (retriesCount > DpsRetryExceptionHandler.MAX_NUM_RETRIES) {
                    throw new InboundDpsHandlerException("Failed to persist the attributes after "
                            + DpsRetryExceptionHandler.MAX_NUM_RETRIES + " retries.");
                }
                DpsRetryExceptionHandler.handleRetry(e, retriesCount);
                attributesBatchSize = calcAttributesBatchSize(MiscellaneousConstants.REDUCED_ATTRIBUTES_BATCH_SIZE_FACTOR);
            }
        }
        return totalPersistedAttributes;
    }

    private int persistAttributesBatch(final String rootFdn, final Map<String, Map<String, Object>> mutableAttributesMap,
                                       final long attributesBatchSize, final boolean isFirstSync) {
        final int persistedAttributes = attributeSyncAlgorithm.writeAttributes(rootFdn, mutableAttributesMap, attributesBatchSize, isFirstSync);
        if (!mutableAttributesMap.isEmpty()) {
            if (persistedAttributes == 0) {
                throw new InboundDpsHandlerException("Unable to persist attributes for FDN: " + rootFdn);
            }
            LOGGER.info("Persisted {} attributes. Continuing to persist the attributes for the remaining {} MOs.",
                    persistedAttributes, mutableAttributesMap.size());
        }
        return persistedAttributes;
    }

    private static long calcAttributesBatchSize(final double sizeFactor) {
        final long dpsTxSizeLimit = Long.getLong("dps.tx.size.limit", DEFAULT_DPS_TX_SIZE_LIMIT);
        final long attributesBatchSize = (long) (dpsTxSizeLimit * sizeFactor);
        LOGGER.debug("The attributes batch size has been calculated to {}.", attributesBatchSize);
        return attributesBatchSize;
    }
}