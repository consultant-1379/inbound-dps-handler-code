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

package com.ericsson.nms.mediation.component.dps.handlers.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.enm.mediation.handler.common.event.CmEventSender;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.ericsson.oss.mediation.network.api.notifications.NotificationType;

@RunWith(MockitoJUnitRunner.class)
public class DeltaSyncDeleteAlgorithmTest {

    private static final String FDN_DPS = "ManagedElement=1,available=1";
    private static final String FDN_NOT_DPS = "ManagedElement=1,notavailable=1";
    private static final String ME_CONTEXT = "MeContext=NODE";

    @Mock
    private DataBucket liveBucketMock;
    @Mock
    private ManagedObject moToDeleteMock;
    @Mock
    private CmEventSender mockCmEventSender;

    private final List<NodeNotification> changes = new ArrayList<NodeNotification>();

    @InjectMocks
    private DeltaSyncAlgorithm deltaSyncAlgorithm;

    @Before
    public void setUp() {
        final NodeNotification deleteInDps = new NodeNotification();
        final NodeNotification deleteNotInDps = new NodeNotification();

        deleteInDps.setAction(NotificationType.DELETE);
        deleteInDps.setGenerationCounter(2L);
        deleteInDps.setFdn(FDN_DPS);
        deleteNotInDps.setAction(NotificationType.DELETE);
        deleteNotInDps.setGenerationCounter(5L);
        deleteNotInDps.setFdn(FDN_NOT_DPS);

        changes.add(deleteInDps);
        changes.add(deleteNotInDps);

        Mockito.doReturn(moToDeleteMock).when(liveBucketMock).findMoByFdn(FdnUtil.prependMeContext(ME_CONTEXT, FDN_DPS));
        Mockito.doReturn(null).when(liveBucketMock).findMoByFdn(FdnUtil.prependMeContext(ME_CONTEXT, FDN_NOT_DPS));
        Mockito.doNothing().when(mockCmEventSender).sendEvent(deleteInDps);
    }

    @Test
    public void testProcessChanges() {
        deltaSyncAlgorithm.processDeleteChanges(changes, liveBucketMock, ME_CONTEXT);

        Mockito.verify(liveBucketMock).deletePo(moToDeleteMock);
        Mockito.verify(liveBucketMock).findMoByFdn(FdnUtil.prependMeContext(ME_CONTEXT, FDN_DPS));
        Mockito.verify(liveBucketMock).findMoByFdn(FdnUtil.prependMeContext(ME_CONTEXT, FDN_NOT_DPS));
    }

}
