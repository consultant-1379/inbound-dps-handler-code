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

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.enm.mediation.handler.common.dps.util.DpsFacade;
import com.ericsson.nms.mediation.component.dps.handlers.algorithms.AttributeSyncAlgorithm;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AttributeWriteTxDpsOperatorTest.class })
@Ignore // class has no tests
public class AttributeWriteTxDpsOperatorTest {
    private static final String FDN = "MeContext=Erbs01,ManagedElement=1,Equipment=testEq";
    private static final String TEST_MO_NAME = "test_name";
    private static final String TEST_MO_NAMESPACE = "test_namespace";
    private static final String TEST_MO_SCHEMA = "dps_primarytype";
    private static final String TEST_MO_VERSION = "1.0.0";

    private final int attributeCount = 200;

    private Map<String, Map<String, Object>> attributeMap;

    @Mock
    private AttributeSyncAlgorithm attributeSyncAlgorithmMock;
    @Mock
    private DpsFacade dpsFacade;
    @Mock
    private ManagedObject mangedObjectMock;
    @Mock
    private DataBucket dataBucketMock;
    @InjectMocks
    private DpsOperator attributeWriteTxDpsOperator;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        attributeMap = new HashMap<>();
        whenNew(AttributeSyncAlgorithm.class).withNoArguments().thenReturn(attributeSyncAlgorithmMock);
        createAttributeMap();
        when(dpsFacade.getLiveBucket()).thenReturn(dataBucketMock);
        when(attributeSyncAlgorithmMock.writeAttributes(anyString(), Matchers.anyMap(), anyLong(), Boolean.FALSE))
                .thenReturn(attributeCount);
        when(dataBucketMock.findMoByFdn(FDN)).thenReturn(mangedObjectMock);
        when(mangedObjectMock.getFdn()).thenReturn(FDN);
    }

    private void createAttributeMap() {
        final Map<String, Object> payload = new HashMap<>();
        for (int i = 0; i < attributeCount; i++) {
            payload.put(FDN + " test=" + i, new ModelInfo(TEST_MO_SCHEMA, TEST_MO_NAMESPACE, TEST_MO_NAME, TEST_MO_VERSION));
        }
        attributeMap.put(FDN, payload);
    }
}
