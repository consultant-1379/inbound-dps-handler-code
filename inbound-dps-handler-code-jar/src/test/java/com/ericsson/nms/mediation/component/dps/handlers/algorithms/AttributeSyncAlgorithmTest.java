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

package com.ericsson.nms.mediation.component.dps.handlers.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.RESTART_TIMESTAMP_ATTR_NAME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.util.DpsFacade;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.enm.mediation.handler.common.event.CmEventSender;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;

@RunWith(MockitoJUnitRunner.class)
public class AttributeSyncAlgorithmTest {
    private static final String ROOT_FDN = "MeContext=1";
    private static final String CHILD_FDN = "MeContext=1,ManagedElement=1";
    private static final String SHM_FDN = "MeContext=1,Inventory=1";
    private static final String CHILD_ATTR_1_NAME = "child_attr_1";
    private static final String CHILD_ATTR_1_VALUE = "1";
    private static final String CHILD_ATTR_2_NAME = "child_attr_2";
    private static final String CHILD_ATTR_2_VALUE = "2";
    private Map<String, Map<String, Object>> attributeData;
    private final long attributesBatchSize = 10;

    @Mock
    private ManagedObject mockRootMo;
    @Mock
    private ManagedObject mockChildMo;
    @Mock
    private ManagedObject mockCppCiMo;
    @Mock
    private DataBucket liveMockBucket;
    @Mock
    private ManagedObject mockShmChildMo;
    @Mock
    private DpsFacade dpsFacade;
    @Mock
    private QueryBuilder queryBuilder;
    @Mock
    private CmEventSender cmEventSender;
    @Mock
    private Query<ContainmentRestrictionBuilder> query;
    @InjectMocks
    private AttributeSyncAlgorithm attributeSyncAlgorithm;

    @Before
    public void setUp() throws Exception {
        final Map<String, Object> childAttributes = new HashMap<>();
        childAttributes.put(CHILD_ATTR_1_NAME, CHILD_ATTR_1_VALUE);
        childAttributes.put(CHILD_ATTR_2_NAME, CHILD_ATTR_2_VALUE);
        attributeData = new HashMap<>();

        attributeData.put(CHILD_FDN, childAttributes);

        // mockChildMo
        final Collection<Object> children = new ArrayList<>();
        when(mockChildMo.getFdn()).thenReturn(CHILD_FDN);
        when(mockChildMo.getAllAttributes()).thenReturn(childAttributes);
        children.add(mockChildMo);

        // mockRootMo... is always ossPrefix, never has attributes read from the node
        when(mockRootMo.getFdn()).thenReturn(ROOT_FDN);
        when(dpsFacade.getLiveBucket()).thenReturn(liveMockBucket);
        when(mockCppCiMo.getFdn()).thenReturn(FdnUtil.getCppCiFdn(ROOT_FDN));
        when(mockCppCiMo.getAttribute(RESTART_TIMESTAMP_ATTR_NAME)).thenReturn(null);
        when(liveMockBucket.findMoByFdn(FdnUtil.getCppCiFdn(ROOT_FDN))).thenReturn(mockCppCiMo);
        when(dpsFacade.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createContainmentQuery(Matchers.anyString())).thenReturn(query);
        when(dpsFacade.executeQuery(query)).thenReturn(children.iterator());
        when(liveMockBucket.findMoByFdn(ROOT_FDN)).thenReturn(mockRootMo);
    }

    @Test
    public void testWriteAttributes() {
        when(liveMockBucket.findMoByFdn(CHILD_FDN)).thenReturn(mockChildMo);
        final int attributesWritten = attributeSyncAlgorithm.writeAttributes(ROOT_FDN, attributeData, attributesBatchSize, Boolean.TRUE);
        verify(mockRootMo, never()).setAttributes(anyMapOf(String.class, Object.class));
        verify(mockChildMo).setAttributes(anyMapOf(String.class, Object.class));
        assertEquals(0, attributeData.size());
        assertEquals(2, attributesWritten);
        verify(cmEventSender, never()).sendBatchEvents(anyListOf(Serializable.class));
        when(mockCppCiMo.getAttribute(RESTART_TIMESTAMP_ATTR_NAME)).thenReturn(new Date());
        final int attributesWritten2 = attributeSyncAlgorithm.writeAttributes(ROOT_FDN, attributeData, attributesBatchSize, Boolean.FALSE);
        assertEquals(0, attributesWritten2);
        verify(cmEventSender).sendBatchEvents(anyListOf(Serializable.class));
    }

    @Test
    public void testWriteAttributes_emptyAttributeData_nothingPersisted() {
        when(liveMockBucket.findMoByFdn(CHILD_FDN)).thenReturn(mockChildMo);
        final Map<String, Map<String, Object>> emptyData = new HashMap<>();
        final int attributesWritten = attributeSyncAlgorithm.writeAttributes(ROOT_FDN, emptyData, attributesBatchSize, Boolean.TRUE);
        verify(mockChildMo, Mockito.never()).setAttributes(anyMapOf(String.class, Object.class));
        assertEquals(0, attributesWritten);
    }

    @Test
    public void testWriteAttributesIgnoringShmMos() {
        // mockChildMo
        final Map<String, Object> childAttributes = new HashMap<>();

        final Collection<ManagedObject> childrenWithShmMo = new ArrayList<>();
        when(mockShmChildMo.getFdn()).thenReturn(SHM_FDN);
        when(mockChildMo.getAllAttributes()).thenReturn(childAttributes);
        childrenWithShmMo.add(mockShmChildMo);
        when(mockRootMo.getChildren()).thenReturn(childrenWithShmMo);
        when(liveMockBucket.findMoByFdn(Matchers.anyString())).thenReturn(mockRootMo);

        attributeSyncAlgorithm.writeAttributes(ROOT_FDN, attributeData, attributesBatchSize, Boolean.FALSE);
        verify(mockChildMo, Mockito.never()).getChildren();
    }

    @Test
    public void testWriteAttributesWithDataInMapButNoTimeout() {
        attributeData.put("UNKNOWN_FDN", new HashMap<String, Object>());
        final int attributesWritten = attributeSyncAlgorithm.writeAttributes(ROOT_FDN, attributeData, attributesBatchSize, Boolean.FALSE);
        assertEquals(2, attributesWritten);
        assertEquals(0, attributeData.size());
    }

    @Test
    public void testGetNodeNotification() {
        final Map<String, Object> fromDps = new HashMap<>();
        fromDps.put("attr1", "From Dps");
        final Map<String, Object> fromNode = new HashMap<>();
        fromNode.put("attr1", "From Node");
        final NodeNotification change = attributeSyncAlgorithm.buildAttributeChangeNotif(ROOT_FDN, fromDps, fromNode);
        assertNotNull(change);
        assertEquals(change.getUpdateAttributes().get("attr1"), "From Node");
    }
}
