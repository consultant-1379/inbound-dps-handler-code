/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.event;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.cm.events.StartSyncEvent;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;

@RunWith(MockitoJUnitRunner.class)
public class MediationEventSenderTest {
    private static final long PO_ID = 123L;

    @Mock
    private EventSender<? super MediationTaskRequest> mockEventSender;

    @InjectMocks
    private MediationEventSender mediationEventSender;

    @Test
    public void testSend() throws Exception {
        mediationEventSender.send(new StartSyncEvent(PO_ID));
        verify(mockEventSender).send(any(StartSyncEvent.class));
    }
}
