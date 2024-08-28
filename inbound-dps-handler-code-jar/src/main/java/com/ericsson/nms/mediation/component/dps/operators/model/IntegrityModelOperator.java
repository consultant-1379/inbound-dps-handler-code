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

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.primarytype.HierarchicalPrimaryTypeSpecification;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.itpf.modeling.modelservice.ModelService;
import com.ericsson.oss.itpf.modeling.modelservice.exception.UnknownModelException;
import com.ericsson.oss.itpf.modeling.modelservice.typed.TypedModelAccess;
import com.google.common.base.Stopwatch;

/**
 * Responsible for performing model validity/integrity checks using {@link ModelService}.
 */
@ApplicationScoped
public class IntegrityModelOperator {

    private static final Logger logger = LoggerFactory.getLogger(IntegrityModelOperator.class);

    @Inject
    private ModelService modelService;

    public boolean isParentChildRelationshipAllowed(final ModelInfo parentModelInfo, final ModelInfo childModelInfo) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        boolean relationshipIsAllowed = true;

        try {
            final TypedModelAccess typedModelAccess = modelService.getTypedAccess();
            final HierarchicalPrimaryTypeSpecification parentTypeSpecification =
                    typedModelAccess.getEModelSpecification(parentModelInfo, HierarchicalPrimaryTypeSpecification.class);
            final HierarchicalPrimaryTypeSpecification childTypeSpecification =
                    typedModelAccess.getEModelSpecification(childModelInfo, HierarchicalPrimaryTypeSpecification.class);
            if (!parentTypeSpecification.canHaveChildOfType(childTypeSpecification)) {
                logger.warn("Parent-Child relationship not defined for parent: '{}' and child: '{}'.", childTypeSpecification,
                        parentTypeSpecification);
                relationshipIsAllowed = false;
            }
        } catch (final UnknownModelException exception) {
            logger.warn("Unknown model encountered when trying to resolve relationship between parent: '{}' and child: '{}'.", parentModelInfo,
                    childModelInfo, exception.getMessage());
            relationshipIsAllowed = false;
        }

        logger.trace("Time taken to look up models for '{} / {}' was [{}] ms.", parentModelInfo, childModelInfo,
                stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return relationshipIsAllowed;
    }

}
