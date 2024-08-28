/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.handlers.algorithms.stubs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;

@SuppressWarnings("unchecked")
public class MoBuilderStub<T> {
    protected ManagedObject parent;

    private String namespace;
    private String name;
    private String type;
    private String version;
    private int creationCount;

    public T type(final String type) {
        this.type = type;
        return (T) this;
    }

    public ManagedObject create() {
        final ManagedObject mo = mock(ManagedObject.class);
        setMoBasicFields(mo);
        setMoVersionAndNamespace(mo);
        creationCount++;
        clearFields();
        return mo;
    }

    protected void setMoBasicFields(final ManagedObject mo) {
        final String fdn = createFdn();
        when(mo.getParent()).thenReturn(parent);
        when(mo.getFdn()).thenReturn(fdn);
        when(mo.getName()).thenReturn(name);
    }

    protected void setMoVersionAndNamespace(final ManagedObject mo) {
        when(mo.getNamespace()).thenReturn(namespace);
        when(mo.getVersion()).thenReturn(version);
    }

    public int getCreationCount() {
        return creationCount;
    }

    private String createFdn() {
        String fdn = "";
        if (parent != null) {
            fdn = parent.getFdn() + ",";
        }
        return fdn + type + "=" + name;
    }

    private void clearFields() {
        name = null;
        namespace = null;
        parent = null;
        type = null;
        version = null;
    }

    public T name(final String name) {
        this.name = name;
        return (T) this;
    }

    public T namespace(final String namespace) {
        this.namespace = namespace;
        return (T) this;
    }

    public T parent(final ManagedObject parent) {
        this.parent = parent;
        return (T) this;
    }

    public T addressInfo(final PersistenceObject addressInfo) {
        throw new UnsupportedOperationException("method not implemented");
    }

    public T version(final String version) {
        this.version = version;
        return (T) this;
    }

    public T addAttributes(final Map<String, Object> attributes) {
        throw new UnsupportedOperationException("method not implemented");
    }

    public T addAttribute(final String attributeName, final Object attributeValue) {
        throw new UnsupportedOperationException("method not implemented");
    }
}
