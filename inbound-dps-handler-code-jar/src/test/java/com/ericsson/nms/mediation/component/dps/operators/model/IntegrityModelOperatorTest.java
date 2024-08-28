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

package com.ericsson.nms.mediation.component.dps.operators.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.primarytype.HierarchicalPrimaryTypeSpecification;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.itpf.modeling.modelservice.ModelService;
import com.ericsson.oss.itpf.modeling.modelservice.exception.UnknownModelException;
import com.ericsson.oss.itpf.modeling.modelservice.typed.TypedModelAccess;

@RunWith(MockitoJUnitRunner.class)
public class IntegrityModelOperatorTest {

    @Mock
    private ModelService modelServiceMock;
    @Mock
    private ModelInfo parentModelInfoMock;
    @Mock
    private ModelInfo childModelInfoMock;
    @Mock
    private TypedModelAccess typedModelAccessMock;
    @Mock
    private HierarchicalPrimaryTypeSpecification parentSpecMock;
    @Mock
    private HierarchicalPrimaryTypeSpecification childSpecMock;

    @InjectMocks
    private IntegrityModelOperator integrityModelOperator;

    @Before
    public void setUp() {
        when(modelServiceMock.getTypedAccess()).thenReturn(typedModelAccessMock);
        when(typedModelAccessMock.getEModelSpecification(parentModelInfoMock, HierarchicalPrimaryTypeSpecification.class)).thenReturn(parentSpecMock);
        when(typedModelAccessMock.getEModelSpecification(childModelInfoMock, HierarchicalPrimaryTypeSpecification.class)).thenReturn(childSpecMock);
    }

    @Test
    public void testParentChildReletionshipAllowed() {
        when(parentSpecMock.canHaveChildOfType(childSpecMock)).thenReturn(true);
        final boolean relationsipAllowed = integrityModelOperator.isParentChildRelationshipAllowed(parentModelInfoMock, childModelInfoMock);
        assertTrue("Expected that parent - child relationship is allwoed by the model.", relationsipAllowed);
    }

    @Test
    public void testParentChildReletionshipNotAllowed() {
        when(parentSpecMock.canHaveChildOfType(childSpecMock)).thenReturn(false);

        final boolean relationsipAllowed = integrityModelOperator.isParentChildRelationshipAllowed(parentModelInfoMock, childModelInfoMock);
        assertFalse("Expected that parent - child relationship is NOT allwoed by the model.", relationsipAllowed);
    }

    @Test
    public void testisParentChildRelationshipAllowedThrowingException() {
        when(parentSpecMock.canHaveChildOfType(childSpecMock)).thenThrow(new UnknownModelException("ERROR!"));
        final boolean relationsipAllowed = integrityModelOperator.isParentChildRelationshipAllowed(parentModelInfoMock, childModelInfoMock);
        assertFalse("Expected that parent - child relationship is NOT allwoed by the model.", relationsipAllowed);
    }

}
