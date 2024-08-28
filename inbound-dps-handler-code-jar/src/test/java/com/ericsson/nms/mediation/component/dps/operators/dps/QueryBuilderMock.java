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

package com.ericsson.nms.mediation.component.dps.operators.dps;

import com.ericsson.oss.itpf.datalayer.dps.exception.model.NotDefinedInModelException;
import com.ericsson.oss.itpf.datalayer.dps.query.*;

public class QueryBuilderMock implements QueryBuilder {
    @Override
    public Query<TypeContainmentRestrictionBuilder> createTypeQuery(final String namespace, final String type, final String baseMoFdn)
            throws NotDefinedInModelException {
        return null;
    }

    @Override
    public Query<TypeRestrictionBuilder> createTypeQuery(final String namespace, final String type) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public Query<GroupRestrictionBuilder> createGroupQuery() {
        return null;
    }

    @Override
    public Query<ContainmentRestrictionBuilder> createContainmentQuery(final String baseMoFdn) {
        return new Query<ContainmentRestrictionBuilder>() {
            @Override
            public void setRestriction(final Restriction restriction) {
            }

            @Override
            public ContainmentRestrictionBuilder getRestrictionBuilder() {
                return null;
            }

            @Override
            public void addSortingOrder(final ObjectField field, final SortDirection direction) {
            }

            @Override
            public void addSortingOrder(final String attributeName, final SortDirection direction) {
            }

            @Override
            public void limitResultSet(final int numResultsToSkip, final int maxResultSize) {
            }
        };
    }

    @Override
    public Query<ChangeLogRestrictionBuilder> createChangeLogQuery() {
        return null;
    }

}
