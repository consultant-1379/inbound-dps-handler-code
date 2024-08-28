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

package com.ericsson.nms.mediation.component.dps.event;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;

/**
 * Mediation event sender wrapper. Responsible for sending events to Event-Based Mediation Client.
 */
@ApplicationScoped
public class MediationEventSender {
    private static final Logger logger = LoggerFactory.getLogger(MediationEventSender.class);

    @Inject
    @Modeled
    private EventSender<MediationTaskRequest> eventSender;

    public <E extends MediationTaskRequest> void send(final E event) {
        final String eventClassName = event.getClass().getSimpleName();
        eventSender.send(event);
        logger.debug("Sent '{}' with node address: '{}'", eventClassName, event.getNodeAddress());
    }

}
