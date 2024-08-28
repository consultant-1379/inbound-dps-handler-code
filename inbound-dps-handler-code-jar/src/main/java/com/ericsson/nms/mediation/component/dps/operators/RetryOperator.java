/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.operators;

import java.util.Date;

import javax.inject.Inject;
import javax.interceptor.Interceptors;

import com.ericsson.nms.mediation.component.dps.operators.dps.DpsOperator;

/**
 * A wrapper class used for performing all operations on DPS which can retry. This wrapper is necessary as every new retry has to happen in a new
 * transaction. It is very important to ensure that all the methods which are called from this class must start in a NEW_TRANSACTION.
 */
@Interceptors({ DpsRetryInterceptor.class })
public class RetryOperator {

    @Inject
    private DpsOperator dpsOperator;

    /**
     * Tries to persist neType attribute under ManagedElement MO in DPS.
     *
     * @param ossPrefix
     *            ossPrefix of the node
     * @param neType
     *            neType of the node
     */
    public void setNeTypeAttribute(final String ossPrefix, final String neType) {
        dpsOperator.setNeTypeAttribute(ossPrefix, neType);
    }

    /**
     * Tries to persist the GC to DPS. For some specific exceptions, it attempts to retry the operation.
     *
     * @param cppCiFdn
     *            MeContext FDN of the node for which generation counter needs to be updated
     * @param generationCounter
     *            The generation counter value to set
     */
    public void updateGc(final String cppCiFdn, final Long generationCounter) {
        dpsOperator.updateGc(cppCiFdn, generationCounter);
    }

    /**
     * Tries to persist the restart timestamp to DPS. For some specific exceptions, it attempts to retry the operation.
     *
     * @param cppCiFdn
     *            MeContext FDN of the node for which generation counter needs to be updated
     * @param restartTimestamp
     *            The timestamp for node restart
     */
    public void setRestartTimestamp(final String cppCiFdn, final Date restartTimestamp) {
        dpsOperator.setRestartTimestamp(cppCiFdn, restartTimestamp);
    }

}
