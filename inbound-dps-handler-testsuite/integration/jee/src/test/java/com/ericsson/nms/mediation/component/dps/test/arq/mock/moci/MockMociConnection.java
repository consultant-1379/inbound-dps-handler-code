/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.test.arq.mock.moci;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.network.api.MociCMConnectionProvider;
import com.ericsson.oss.mediation.network.api.exception.MociConnectionProviderException;
import com.ericsson.oss.mediation.network.api.util.ConnectionConfig;
import com.ericsson.oss.mediation.network.api.util.ManagedObjectMetaInfo;
import com.ericsson.oss.mediation.network.api.util.MibChangeInfo;
import com.ericsson.oss.mediation.network.api.util.NodeAttributeModelDTO;
import com.ericsson.oss.mediation.network.api.util.NodeMetaInfo;

@Singleton
@Startup
@Local(MociCMConnectionProvider.class)
@EJB(name = MociCMConnectionProvider.VERSION_INDEPENDENT_JNDI_NAME, beanInterface = MociCMConnectionProvider.class)
public class MockMociConnection implements MociCMConnectionProvider {
    private static final Logger logger = LoggerFactory.getLogger(MockMociConnection.class);

    @Override
    public Date getRestartNodeDate(final ConnectionConfig paramConnectionConfig) throws MociConnectionProviderException {
        logger.warn("restart node date requested");
        return new Date();
    }

    @Override
    public NodeMetaInfo getNodeInfo(final ConnectionConfig paramConnectionConfig) throws MociConnectionProviderException {
        return null;
    }

    @Override
    public boolean setDnPrefix(final ConnectionConfig paramConnectionConfig, final String paramString) throws MociConnectionProviderException {
        return false;
    }

    @Override
    public Collection<ManagedObjectMetaInfo> getFdns(final ConnectionConfig paramConnectionConfig) throws MociConnectionProviderException {
        return null;
    }

    @Override
    public long getGenerationCounter(final ConnectionConfig paramConnectionConfig) throws MociConnectionProviderException {
        return 0;
    }

    @Override
    public Object action(final ConnectionConfig paramConnectionConfig, final String paramString1, final String paramString2,
            final Map<String, NodeAttributeModelDTO> paramMap) throws MociConnectionProviderException {
        return null;
    }

    @Override
    public void modify(final ConnectionConfig paramConnectionConfig, final String paramString, final Map<String, NodeAttributeModelDTO> paramMap)
            throws MociConnectionProviderException {
    }

    @Override
    public void createMO(final ConnectionConfig paramConnectionConfig, final String paramString1, final String paramString2,
            final Map<String, NodeAttributeModelDTO> paramMap) throws MociConnectionProviderException {
    }

    @Override
    public void deleteMO(final ConnectionConfig paramConnectionConfig, final String paramString) throws MociConnectionProviderException {
    }

    @Override
    public Collection<MibChangeInfo> getMibChanges(final ConnectionConfig paramConnectionConfig, final int paramInt)
            throws MociConnectionProviderException {
        return null;
    }

    @Override
    public Map<String, Object> getMOAttributes(final ConnectionConfig connectionData, final String fdn, final Collection<String> moAttributes)
            throws MociConnectionProviderException {
        return null;
    }

    @Override
    public Map<String, Map<String, Object>> getMOAttributes(final ConnectionConfig connectionData, final Map<String, String[]> attrs)
            throws MociConnectionProviderException {
        return null;
    }

    @Override
    public boolean ping(final ConnectionConfig arg) throws MociConnectionProviderException {
        return false;
    }

    @Override
    public int getMoInstanceCount(final ConnectionConfig connectionConfig) throws MociConnectionProviderException {
        throw new MociConnectionProviderException("METHOD NOT SUPPORTED BY TEST MOCK");
    }
}
