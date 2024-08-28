/*------------------------------------------------------------------------------
*******************************************************************************
* COPYRIGHT Ericsson 2017
*
* The copyright to the computer program(s) herein is the property of
* Ericsson Inc. The programs may be used and/or copied only with written
* permission from Ericsson Inc. or in accordance with the terms and
* conditions stipulated in the agreement/contract under which the
* program(s) have been supplied.
*******************************************************************************
*----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.common;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;

@RunWith(MockitoJUnitRunner.class)
public class NeTypeSetterTest {
    private static final String OSS_PREFIX = "MeContext=Foo";
    private static final String MANAGED_ELEMENT_FDN = "MeContext=Foo,ManagedElement=1";
    private static final String NE_TYPE = "ERBS";

    @Mock
    private ManagedObject managedElementMock;
    @Mock
    private DataBucket liveBucket;

    @InjectMocks
    private NeTypeSetter neTypeSetter;

    @Before
    public void setUp() {
        when(liveBucket.findMoByFdn(MANAGED_ELEMENT_FDN)).thenReturn(managedElementMock);
    }

    @Test
    public void testOssPrefixIsEmpty() {
        neTypeSetter.checkAndSetNeTypeAttribute(liveBucket, "", NE_TYPE);
        verifyZeroInteractions(liveBucket);
    }

    @Test
    public void testOssPrefixIsNull() {
        neTypeSetter.checkAndSetNeTypeAttribute(liveBucket, null, NE_TYPE);
        verifyZeroInteractions(liveBucket);
    }

    @Test
    public void testMoNotFoundInDps() {
        when(liveBucket.findMoByFdn(MANAGED_ELEMENT_FDN)).thenReturn(null);
        neTypeSetter.checkAndSetNeTypeAttribute(liveBucket, OSS_PREFIX, NE_TYPE);
        verify(liveBucket).findMoByFdn(MANAGED_ELEMENT_FDN);
        verify(managedElementMock, never()).setAttribute("neType", NE_TYPE);
    }

    @Test
    public void testMoWithNullAttribute() {
        when(managedElementMock.getAttribute("neType")).thenReturn(null);
        neTypeSetter.checkAndSetNeTypeAttribute(liveBucket, OSS_PREFIX, NE_TYPE);
        verify(managedElementMock).setAttribute("neType", NE_TYPE);
    }

    @Test
    public void testMoWithNotNullAttribute() {
        when(managedElementMock.getAttribute("neType")).thenReturn(NE_TYPE);
        neTypeSetter.checkAndSetNeTypeAttribute(liveBucket, OSS_PREFIX, NE_TYPE);
        verify(managedElementMock, never()).setAttribute("neType", NE_TYPE);
    }

}
