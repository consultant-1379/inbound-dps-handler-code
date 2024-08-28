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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ME_CONTEXT_MO_TYPE;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.DpsFacade;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.handlers.algorithms.DeltaSyncAlgorithm;
import com.ericsson.nms.mediation.component.dps.handlers.payload.DeltaSyncDpsPayload;
import com.ericsson.nms.mediation.component.dps.operators.RetryOperator;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;

@RunWith(MockitoJUnitRunner.class)
public class DeltaSyncDpsOperatorTest {
    private static final String NODE_NAME = "VooDoo";
    private static final String ME_CONTEXT_FDN = ME_CONTEXT_MO_TYPE + "=" + NODE_NAME;
    private static final String CPP_CI_FDN = FdnUtil.getCppCiFdn(ME_CONTEXT_FDN);
    private static final Long GENERATION_COUNTER = 100L;
    private static final String NODE_TYPE = "nodeType";
    private static final String ERBS = "ERBS";

    @Mock
    private ComponentEvent eventMock;
    @Mock
    private DeltaSyncAlgorithm deltaSyncAlgorithmMock;
    @Mock
    private CppCiMoDpsOperator cppCiMoDpsOperatorMock;
    @Mock
    private DpsFacade dpsFacadeMock;
    @Mock
    private DataBucket liveBucketMock;
    @Mock
    private SystemRecorder systemRecorder;
    @Mock
    private DpsOperator dpsOperator;
    @Mock
    private RetryOperator dpsOperatorMock;

    @InjectMocks
    private DeltaSyncDpsOperator deltaSyncDpsOperator;

    @Before
    public void setUp() throws Exception {
        final Map<String, Object> eventHeaders = new HashMap<>();
        eventHeaders.put(MiscellaneousConstants.GENERATION_COUNTER_HEADER_NAME, GENERATION_COUNTER);
        eventHeaders.put(NODE_TYPE, ERBS);
        when(dpsFacadeMock.getLiveBucket()).thenReturn(liveBucketMock);
        when(eventMock.getHeaders()).thenReturn(eventHeaders);
    }

    @Test
    public void testPersistDeltaSyncChanges() {
        final DeltaSyncDpsPayload payload = createPayload();
        final int totalChangesPersisted = deltaSyncDpsOperator.persistDeltaSyncChanges(eventMock, ME_CONTEXT_FDN);
        final int totalPayloadChanges = payload.getCreateAndUpdateChanges().size() + payload.getDeleteChanges().size();

        Assert.assertEquals(totalPayloadChanges, totalChangesPersisted);
        verify(deltaSyncAlgorithmMock).processCreateUpdateChanges(payload.getCreateAndUpdateChanges(), liveBucketMock, ME_CONTEXT_FDN);
        verify(deltaSyncAlgorithmMock).processDeleteChanges(payload.getDeleteChanges(), liveBucketMock, ME_CONTEXT_FDN);
        verify(cppCiMoDpsOperatorMock).setGenerationCounter(CPP_CI_FDN, GENERATION_COUNTER);
        verify(dpsOperatorMock).setNeTypeAttribute(ME_CONTEXT_FDN, ERBS);
    }

    @Test
    public void testPersistDeltaSyncChanges_GenerationCounterIsZero() {
        final DeltaSyncDpsPayload payload = createPayload();
        payload.setGenerationCounter(0L);

        Assert.assertEquals(payload.getCreateAndUpdateChanges().size() + payload.getDeleteChanges().size(),
                deltaSyncDpsOperator.persistDeltaSyncChanges(eventMock, ME_CONTEXT_FDN));
        verify(cppCiMoDpsOperatorMock, Mockito.never()).setGenerationCounter(CPP_CI_FDN, GENERATION_COUNTER);
    }

    private DeltaSyncDpsPayload createPayload() {
        final Collection<NodeNotification> createupdates = new LinkedList<>();
        createupdates.add(new NodeNotification());
        final Collection<NodeNotification> deletes = new LinkedList<>();
        deletes.add(new NodeNotification());

        final DeltaSyncDpsPayload payload = new DeltaSyncDpsPayload(createupdates, deletes, GENERATION_COUNTER);
        when(eventMock.getPayload()).thenReturn(payload);

        return payload;
    }

}
