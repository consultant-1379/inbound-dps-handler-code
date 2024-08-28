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

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import static com.ericsson.enm.mediation.handler.common.dps.constants.MoConstants.ME_CONTEXT_MO_TYPE;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.enm.mediation.handler.common.dps.operators.CppCiMoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.operators.MoDpsOperator;
import com.ericsson.enm.mediation.handler.common.dps.util.FdnUtil;
import com.ericsson.nms.mediation.component.dps.exception.InboundDpsHandlerException;
import com.ericsson.nms.mediation.component.dps.handlers.algorithms.AttributeSyncAlgorithm;
import com.ericsson.nms.mediation.component.dps.operators.RetryOperator;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AttributeWriteDpsOperator.class })
public class AttributeWriteDpsOperatorTest {
    private static final String NODE_NAME = "VooDoo";
    private static final String ME_CONTEXT_FDN = ME_CONTEXT_MO_TYPE + "=" + NODE_NAME;
    private static final String CPP_CI_FDN = FdnUtil.getCppCiFdn(ME_CONTEXT_FDN);
    private static final String FDN = "MeContext=Erbs01,ManagedElement=1,Equipment=testEq";
    private static final String EMPTY_ME_CONTEXT_FDN = "";

    private static final String GENERATION_COUNTER_HEADER_NAME = "generationCounter";
    private static final Long GENERATION_COUNTER_VALUE = 12345L;
    private static final String RESTART_NODE_DATE_HEADER_NAME = "restartTimestamp";
    private static final String RESTART_NODE_DATE_VALUE = "Sat, 22 Nov 2014 21:21:43 GMT";

    private static final String NODE_TYPE = "nodeType";
    private static final String ERBS = "ERBS";
    private static final int TOTAL_ATTRS = 10;

    private Map<String, Object> eventHeaders;

    private Date restartDate;
    private final SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");

    @Mock
    private ComponentEvent eventMock;
    @Mock
    private ManagedObject rootMoMock;
    @Mock
    private MoDpsOperator generalMoDpsOperatorMock;
    @Mock
    private RetryOperator dpsOperatorMock;
    @Mock
    private AttributeSyncAlgorithm attributeSyncAlgorithmMock;
    @Mock
    private DpsOperator attributeWriteTxDpsOperatorMock;
    @Mock
    private CppCiMoDpsOperator cppCiMoDpsOperatorMock;

    @InjectMocks
    private AttributeWriteDpsOperator attributeWriteDpsOperator;

    @Before
    public void setUp() throws Exception {
        restartDate = formatter.parse(RESTART_NODE_DATE_VALUE);

        // constructor mocks
        whenNew(AttributeSyncAlgorithm.class).withNoArguments().thenReturn(attributeSyncAlgorithmMock);
        whenNew(DpsOperator.class).withNoArguments().thenReturn(attributeWriteTxDpsOperatorMock);

        // mockEvent
        when(eventMock.getPayload()).thenReturn(new HashMap<String, Object>());
        eventHeaders = new HashMap<>();
        eventHeaders.put(GENERATION_COUNTER_HEADER_NAME, GENERATION_COUNTER_VALUE);
        eventHeaders.put(RESTART_NODE_DATE_HEADER_NAME, restartDate);
        eventHeaders.put(NODE_TYPE, ERBS);
        when(eventMock.getHeaders()).thenReturn(eventHeaders);

        // other mocks
        when(generalMoDpsOperatorMock.getMo(ME_CONTEXT_FDN)).thenReturn(rootMoMock);
    }

    @Test
    public void testDoNotPersistAttributesWhenPayloadIsEmpty() throws Exception {
        final Map<String, Object> payload = new HashMap<>();
        when(eventMock.getPayload()).thenReturn(payload);
        attributeWriteDpsOperator.handleAttributesPersistence(eventMock, ME_CONTEXT_FDN, Boolean.FALSE);
        verify(dpsOperatorMock).updateGc(CPP_CI_FDN, GENERATION_COUNTER_VALUE);
        verify(dpsOperatorMock).setRestartTimestamp(CPP_CI_FDN, restartDate);
        verify(dpsOperatorMock).setNeTypeAttribute(ME_CONTEXT_FDN, ERBS);
        verify(attributeSyncAlgorithmMock, never()).writeAttributes(anyString(), anyMap(), anyLong(), anyBoolean());
    }

    @Test
    public void testPersistAttributes() {
        final Map<String, Map<String, Object>> payload = new HashMap<>();
        final Map<String, Object> data = new HashMap<>();
        data.put("userLabel", TOTAL_ATTRS);
        for (int i = 0; i < TOTAL_ATTRS; i++) {
            payload.put(FDN + " test=" + i, data);
        }
        when(eventMock.getPayload()).thenReturn(payload);
        when(attributeSyncAlgorithmMock.writeAttributes(eq(ME_CONTEXT_FDN), eq(payload), anyLong(), eq(Boolean.FALSE))).thenReturn(1)
                .thenAnswer(new Answer() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        ((Map<String, Map<String, Object>>) invocation.getArguments()[1]).clear();
                        return null;
                    }
                });
        attributeWriteDpsOperator.handleAttributesPersistence(eventMock, ME_CONTEXT_FDN, Boolean.FALSE);
        verify(attributeSyncAlgorithmMock, times(2)).writeAttributes(anyString(), anyMap(), anyLong(), anyBoolean());
    }

    @Test(expected = InboundDpsHandlerException.class)
    public void testPersistAttributesExceptionScenario() throws Exception {
        final Map<String, Object> payload = new HashMap<>();
        final Map<String, Object> data = new HashMap<>();
        data.put("userLabel", TOTAL_ATTRS);
        for (int i = 0; i < TOTAL_ATTRS; i++) {
            payload.put(FDN + " test=" + i, data);
        }
        when(eventMock.getPayload()).thenReturn(payload);
        when(attributeSyncAlgorithmMock.writeAttributes(anyString(), anyMap(), anyLong(), anyBoolean())).thenReturn(0);
        attributeWriteDpsOperator.handleAttributesPersistence(eventMock, ME_CONTEXT_FDN, Boolean.FALSE);
    }

    @Test
    public void testSetNeTypeAttributeWithInvalidFdn() throws Exception {
        attributeWriteDpsOperator.handleAttributesPersistence(eventMock, EMPTY_ME_CONTEXT_FDN, Boolean.FALSE);
        verify(dpsOperatorMock, never()).setNeTypeAttribute(ME_CONTEXT_FDN, ERBS);
    }
}
