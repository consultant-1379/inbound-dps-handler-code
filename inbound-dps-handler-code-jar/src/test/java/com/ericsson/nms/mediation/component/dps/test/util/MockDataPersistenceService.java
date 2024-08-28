/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.nms.mediation.component.dps.test.util;

import java.util.Collection;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataBucketProperty;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.NonLiveDataBucket;
import com.ericsson.oss.itpf.datalayer.dps.availability.DpsAvailabilityCallback;
import com.ericsson.oss.itpf.datalayer.dps.database.DatabaseStorageSpaceInfo;
import com.ericsson.oss.itpf.datalayer.dps.database.DpsTransactionConnectionInfo;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.spi.ChangeRecorder;
import com.ericsson.oss.itpf.datalayer.dps.stub.StubbedDataPersistenceService;

public class MockDataPersistenceService implements DataPersistenceService {
    private final DataPersistenceService stubbedDps = new StubbedDataPersistenceService();

    @Override public void setWriteAccess(final boolean b) {
        stubbedDps.setWriteAccess(b);
    }

    @Override public void setTransactionAccessType(final boolean b) {
        stubbedDps.setTransactionAccessType(b);
    }

    @Override public DataBucket getLiveBucket() {
        return stubbedDps.getLiveBucket();
    }

    @Override public DataBucket getLiveBucketWithChangeRecorder(final ChangeRecorder changeRecorder) {
        return stubbedDps.getLiveBucketWithChangeRecorder(changeRecorder);
    }

    @Override public <T extends DataBucket> T getDataBucket(final String s, final String... strings) {
        return stubbedDps.getDataBucket(s, strings);
    }

    @Override public NonLiveDataBucket createDataBucket(final String s, final String s1, final String... strings) {
        return stubbedDps.createDataBucket(s, s1, strings);
    }

    @Override public long deleteDataBucket(final String s, final boolean b) {
        return stubbedDps.deleteDataBucket(s, b);
    }

    @Override public Collection<String> getAllDataBucketNames() {
        return stubbedDps.getAllDataBucketNames();
    }

    @Override public QueryBuilder getQueryBuilder() {
        return stubbedDps.getQueryBuilder();
    }

    @Override public NonLiveDataBucket setDataBucketProperty(final String s, final DataBucketProperty dataBucketProperty) {
        return stubbedDps.setDataBucketProperty(s, dataBucketProperty);
    }

    @Override public NonLiveDataBucket removeDataBucketProperty(final String s, final DataBucketProperty dataBucketProperty) {
        return stubbedDps.removeDataBucketProperty(s, dataBucketProperty);
    }

    @Override public DatabaseStorageSpaceInfo getDatabaseStorageInformation() {
        return stubbedDps.getDatabaseStorageInformation();
    }

    @Override public void registerDpsAvailabilityCallback(final DpsAvailabilityCallback dpsAvailabilityCallback) {
        return;
    }

    @Override public void deregisterDpsAvailabilityCallback(final String s) {
        return;
    }

    @Override
    public void setTransactionConnectionInfo(final DpsTransactionConnectionInfo transactionConnectionInfo) {
        throw new UnsupportedOperationException("setTransactionConnectionInfo not supported");
    }
}
