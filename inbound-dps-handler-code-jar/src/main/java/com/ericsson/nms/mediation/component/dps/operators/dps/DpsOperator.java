/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.operators.dps;

import java.util.Date;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.DpsFacade;
import com.ericsson.nms.mediation.component.dps.common.NeTypeSetter;

/**
 * This class has a responsibility to read from or write to DPS.
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class DpsOperator {
    @Inject
    private DpsFacade dpsFacade;
    @Inject
    private CppCiMoDpsOperator cppCiMoDpsOperator;
    @Inject
    private NeTypeSetter neTypeSetter;

    /**
     * Open a new transaction to DPS and write neType attributes under ManagedElement MO.
     *
     * @param ossPrefix
     *            ossPrefix of the node
     * @param neType
     *            neType of the node
     **/
    public void setNeTypeAttribute(final String ossPrefix, final String neType) {
        neTypeSetter.checkAndSetNeTypeAttribute(dpsFacade.getLiveBucket(), ossPrefix, neType);
    }

    /**
     * Open a new transaction to DPS and update Generation Counter.
     *
     * @param cppCiFdn
     *            MeContext FDN of the node for which generation counter needs to be updated.
     * @param generationCounter
     *            The generation counter value to set.
     **/
    public void updateGc(final String cppCiFdn, final Long generationCounter) {
        cppCiMoDpsOperator.setGenerationCounter(cppCiFdn, generationCounter);
    }

    /**
     * Open a new transaction to DPS and write attributes.
     *
     * @param cppCiFdn
     *            MeContext FDN of the node for which generation counter needs to be updated.
     * @param restartTimestamp
     *            The timestamp for the node's last restart.
     **/
    public void setRestartTimestamp(final String cppCiFdn, final Date restartTimestamp) {
        cppCiMoDpsOperator.setRestartTimestamp(cppCiFdn, restartTimestamp);
    }
}