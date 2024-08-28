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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ME_CONTEXT_MO_TYPE;
import static com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants.OSS_PREFIX_HEADER_NAME;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.GENERATION_COUNTER_HEADER_NAME;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.NODE_NAMESPACE_KEY;
import static com.ericsson.oss.mediation.cm.constants.CommonConstants.NODE_VERSION_KEY;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.MoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.NetworkElementMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.utility.constants.MiscellaneousConstants;

@RunWith(MockitoJUnitRunner.class)
public class DeltaNodeInfoReadDpsOperatorTest {
    private static final String NODE_NAME = "VooDoo";
    private static final String ME_CONTEXT_FDN = ME_CONTEXT_MO_TYPE + "=" + NODE_NAME;
    private static final String MANAGED_ELEMENT_FDN = FdnUtil.appendManagedElement(ME_CONTEXT_FDN);
    private static final String NETWORK_ELEMENT_FDN = FdnUtil.getNetworkElementFdn(NODE_NAME);
    private static final String CM_FUNCTION_FDN = FdnUtil.appendCmFunction(NETWORK_ELEMENT_FDN);
    private static final String CPP_CI_FDN = FdnUtil.getCppCiFdn(ME_CONTEXT_FDN);

    private static final long GENERATION_COUNTER_ATTR_VALUE = 654L;
    private static final String MO_VERSION = "1.2.345";
    private static final String MO_NAMESPACE = "ERBS_NODE_MODEL";
    private static final String OSS_MODEL_IDENTITY_PARAM = "ossModelIdendity";
    private static final String NODE_TYPE_VALUE = "MyNodeType";
    private static final String OSS_MODEL_IDENTITY_VALUE = "MyModelIdentity";

    @Mock
    private MoDpsOperator generalMoDpsOperatorMock;
    @Mock
    private NetworkElementMoDpsOperator networkElementMoDpsOperatorMock;
    @Mock
    private CppCiMoDpsOperator cppCiMoDpsOperatorMock;

    @InjectMocks
    private DeltaNodeInfoReadDpsOperator deltaNodeInfoReadDpsOperator;

    @Test
    public void testCreateHeaders() {
        final Map<String, Object> expectedHeaders = new HashMap<>();
        expectedHeaders.put(GENERATION_COUNTER_HEADER_NAME, GENERATION_COUNTER_ATTR_VALUE);
        expectedHeaders.put(OSS_PREFIX_HEADER_NAME, ME_CONTEXT_FDN);
        expectedHeaders.put(MiscellaneousConstants.NODE_TYPE_HEADER_NAME, NODE_TYPE_VALUE);
        expectedHeaders.put(OSS_MODEL_IDENTITY_PARAM, OSS_MODEL_IDENTITY_VALUE);

        when(networkElementMoDpsOperatorMock.getOssPrefix(NETWORK_ELEMENT_FDN)).thenReturn(ME_CONTEXT_FDN);
        when(networkElementMoDpsOperatorMock.getNeType(NETWORK_ELEMENT_FDN)).thenReturn(NODE_TYPE_VALUE);
        when(networkElementMoDpsOperatorMock.getOssModelIdentity(NETWORK_ELEMENT_FDN)).thenReturn(OSS_MODEL_IDENTITY_VALUE);
        when(cppCiMoDpsOperatorMock.getGenerationCounter(CPP_CI_FDN)).thenReturn(GENERATION_COUNTER_ATTR_VALUE);

        final Map<String, Object> headers = deltaNodeInfoReadDpsOperator.createHeaders(CM_FUNCTION_FDN);
        verify(networkElementMoDpsOperatorMock).getOssPrefix(NETWORK_ELEMENT_FDN);
        verify(cppCiMoDpsOperatorMock).getGenerationCounter(CPP_CI_FDN);
        assertEquals(expectedHeaders, headers);
    }

    @Test
    public void testCreatePayload() {
        final Map<String, String> expectedPayload = new HashMap<>();
        expectedPayload.put(NODE_NAMESPACE_KEY, MO_NAMESPACE);
        expectedPayload.put(NODE_VERSION_KEY, MO_VERSION);

        when(generalMoDpsOperatorMock.getNamespace(MANAGED_ELEMENT_FDN)).thenReturn(MO_NAMESPACE);
        when(generalMoDpsOperatorMock.getVersion(MANAGED_ELEMENT_FDN)).thenReturn(MO_VERSION);

        final Object payload = deltaNodeInfoReadDpsOperator.createPayload(ME_CONTEXT_FDN);
        verify(generalMoDpsOperatorMock).getNamespace(MANAGED_ELEMENT_FDN);
        verify(generalMoDpsOperatorMock).getVersion(MANAGED_ELEMENT_FDN);
        assertEquals(expectedPayload, payload);
    }

}
