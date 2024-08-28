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

package com.ericsson.nms.mediation.component.dps.handlers.algorithms.stubs;

import static org.mockito.Mockito.when;

import com.ericsson.oss.itpf.datalayer.dps.object.builder.ManagedObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;

public class ManagedObjectBuilderStub extends MoBuilderStub<ManagedObjectBuilder> implements ManagedObjectBuilder {
    @Override
    protected void setMoVersionAndNamespace(final ManagedObject mo) {
        final String namespace = parent.getNamespace();
        final String version = parent.getVersion();
        when(mo.getNamespace()).thenReturn(namespace);
        when(mo.getVersion()).thenReturn(version);
    }

    @Override
    public ManagedObjectBuilder target(final PersistenceObject arg) {
        return null;
    }
}
