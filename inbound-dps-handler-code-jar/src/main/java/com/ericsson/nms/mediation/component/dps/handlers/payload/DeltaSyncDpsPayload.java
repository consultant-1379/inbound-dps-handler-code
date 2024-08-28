/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2013
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.handlers.payload;

import java.util.Collection;

import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;

/**
 * Wrapper for <code>DeltaSyncMergerHandler</code>'s output payload which is passed as part of the <code>ComponentEvent</code> to the next step in the
 * flow i.e. <code>DeltaSyncDpsHandler</code>.
 */
public class DeltaSyncDpsPayload {

    private Collection<NodeNotification> createAndUpdateChanges;
    private Collection<NodeNotification> deleteChanges;
    private Long generationCounter;

    public DeltaSyncDpsPayload(final Collection<NodeNotification> createAndUpdateChanges, final Collection<NodeNotification> deleteChanges,
            final Long generationCounter) {
        this.createAndUpdateChanges = createAndUpdateChanges;
        this.deleteChanges = deleteChanges;
        this.generationCounter = generationCounter;
    }

    public Collection<NodeNotification> getCreateAndUpdateChanges() {
        return createAndUpdateChanges;
    }

    public void setCreateAndUpdateChanges(final Collection<NodeNotification> createAndUpdateChanges) {
        this.createAndUpdateChanges = createAndUpdateChanges;
    }

    public Collection<NodeNotification> getDeleteChanges() {
        return deleteChanges;
    }

    public void setDeleteChanges(final Collection<NodeNotification> deleteChanges) {
        this.deleteChanges = deleteChanges;
    }

    public Long getGenerationCounter() {
        return generationCounter;
    }

    public void setGenerationCounter(final Long generationCounter) {
        this.generationCounter = generationCounter;
    }

}
