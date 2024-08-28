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

package com.ericsson.nms.mediation.component.dps.instrumentation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.nms.mediation.component.dps.utility.InstrumentationUtil;
import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Category;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.CollectionType;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Interval;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Units;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Visibility;

/**
 * Records metrics as part of Delta Sync Node including average DPS operation times, Delta Sync times, and Delta Sync details.
 */
@ApplicationScoped
@InstrumentedBean(description = "Records metrics as part of Delta Sync", displayName = "Delta Sync Node Metrics - DPS Operations")
public class DeltaDpsHandlerInstrumentation {
    private AtomicInteger dpsSuccessfulDeltaSync = new AtomicInteger(0);
    private AtomicInteger dpsFailedDeltaSync = new AtomicInteger(0);
    private AtomicInteger dpsDeltaInvocationAttributeSync = new AtomicInteger(0);

    private long averageNumberOfAttrBeingSynced;
    private long dpsPersistAttributesAvgTime;
    private long averageOverallDeltaSyncTimeTaken;
    private long averageOverallNodeInfoDeltaSyncTimeTaken;

    private final List<Long> deltaNumberOfAttrBeingSyncedSamples = new ArrayList<>();
    private final List<Long> deltaPersistAttributesTimeSamples = new ArrayList<>();
    private final List<Long> overallDeltaSyncTimesSamples = new ArrayList<>();
    private final List<Long> overallDeltaSyncNodeInfoTimesSamples = new ArrayList<>();

    @MonitoredAttribute(displayName = "Number of Successful Delta Syncs",
            visibility = Visibility.ALL, units = Units.NONE, category = Category.UTILIZATION,
            interval = Interval.ONE_MIN, collectionType = CollectionType.TRENDSUP)
    public int getDpsSuccessfulDeltaSync() {
        return dpsSuccessfulDeltaSync.get();
    }

    public void setDpsSuccessfulDeltaSync(final AtomicInteger dpsSuccessfulDeltaSync) {
        this.dpsSuccessfulDeltaSync = dpsSuccessfulDeltaSync;
    }

    public void increaseSuccessfulDeltaSync() {
        dpsSuccessfulDeltaSync.incrementAndGet();
    }

    @MonitoredAttribute(displayName = "Number of Failed Delta Syncs", visibility = Visibility.ALL,
            units = Units.NONE, category = Category.UTILIZATION, interval = Interval.ONE_MIN, collectionType = CollectionType.TRENDSUP)
    public int getDpsFailedDeltaSync() {
        return dpsFailedDeltaSync.get();
    }

    public void setDpsFailedDeltaSync(final AtomicInteger dpsFailedDeltaSync) {
        this.dpsFailedDeltaSync = dpsFailedDeltaSync;
    }

    public void increaseFailedDeltaSync() {
        dpsFailedDeltaSync.incrementAndGet();
    }

    @MonitoredAttribute(displayName = "Average Number of Attributes being Synchronized During Delta Sync",
            visibility = Visibility.ALL, units = Units.NONE, category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN, collectionType = CollectionType.DYNAMIC)
    public long getAverageNumberOfAttrBeingSynced() {
        setAverageNumberOfAttrBeingSynced(InstrumentationUtil.calculateAverageAndReset(deltaNumberOfAttrBeingSyncedSamples));
        // an attribute is required for the averageNumberOfAttrBeingSyncd to
        // conform with SDK Instrumentation to expose the attribute using JMX
        return averageNumberOfAttrBeingSynced;
    }

    public void setAverageNumberOfAttrBeingSynced(final long averageNumberOfAttrBeingSynced) {
        this.averageNumberOfAttrBeingSynced = averageNumberOfAttrBeingSynced;
    }

    public void addToNumberOfAttrBeingSyncedSamples(final Long numberOfAttrBeingSynced) {
        deltaNumberOfAttrBeingSyncedSamples.add(numberOfAttrBeingSynced);
    }

    @MonitoredAttribute(displayName = "Average Time Taken to Persist Attributes During Delta Sync",
            visibility = Visibility.ALL, units = Units.MILLISECONDS, category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN, collectionType = CollectionType.DYNAMIC)
    public long getDpsPersistAttributesAvgTime() {
        setDpsPersistAttributesAvgTime(InstrumentationUtil.calculateAverageAndReset(deltaPersistAttributesTimeSamples));
        return dpsPersistAttributesAvgTime;
    }

    public void setDpsPersistAttributesAvgTime(final long dpsPersistAttributesAvgTime) {
        this.dpsPersistAttributesAvgTime = dpsPersistAttributesAvgTime;
    }

    public void addToDpsPersistAttributesAvgTimeSamples(final Long timeTakenToPersistAttr) {
        deltaPersistAttributesTimeSamples.add(timeTakenToPersistAttr);
    }

    @MonitoredAttribute(displayName = "Average time taken to perform Delta Sync on a node",
            visibility = Visibility.ALL, units = Units.MILLISECONDS, category = Category.PERFORMANCE,
            interval = Interval.FIVE_MIN, collectionType = CollectionType.DYNAMIC)
    public long getAverageOverallDeltaSyncTimeTaken() {
        setAverageOverallDeltaSyncTimeTaken(InstrumentationUtil.calculateAverageAndReset(overallDeltaSyncTimesSamples));
        return averageOverallDeltaSyncTimeTaken;
    }

    public void setAverageOverallDeltaSyncTimeTaken(final long averageOverallDeltaSyncTimeTaken) {
        this.averageOverallDeltaSyncTimeTaken = averageOverallDeltaSyncTimeTaken;
    }

    public void addToAverageOverallDeltaSyncTimesTaken(final Long averageOverallDeltaSyncTimeTaken) {
        overallDeltaSyncTimesSamples.add(averageOverallDeltaSyncTimeTaken);
    }

    @MonitoredAttribute(displayName = "Average Time spent retrieving information from the node during Delta Sync",
            visibility = Visibility.ALL, units = Units.MILLISECONDS, category = Category.PERFORMANCE,
            interval = Interval.FIVE_MIN, collectionType = CollectionType.DYNAMIC)
    public long getAverageOverallNodeInfoDeltaSyncTimeTaken() {
        setAverageOverallNodeInfoDeltaSyncTimeTaken(InstrumentationUtil.calculateAverageAndReset(overallDeltaSyncNodeInfoTimesSamples));
        return averageOverallNodeInfoDeltaSyncTimeTaken;
    }

    public void setAverageOverallNodeInfoDeltaSyncTimeTaken(final long averageOverallDeltaSyncNodeInfoTimeTaken) {
        this.averageOverallNodeInfoDeltaSyncTimeTaken = averageOverallDeltaSyncNodeInfoTimeTaken;
    }

    public void addToAverageOverallNodeInfoDeltaSyncTimeTaken(final Long averageOverallDeltaSyncNodeInfoTimeTaken) {
        overallDeltaSyncNodeInfoTimesSamples.add(averageOverallDeltaSyncNodeInfoTimeTaken);
    }

    @MonitoredAttribute(displayName = "Number of DPS Invocations for Delta Sync of a node", visibility = Visibility.ALL,
            units = Units.NONE, category = Category.UTILIZATION, interval = Interval.ONE_MIN, collectionType = CollectionType.TRENDSUP)
    public int getDpsDeltaInvocationAttributeSync() {
        return dpsDeltaInvocationAttributeSync.get();
    }

    public void setDpsDeltaInvocationAttributeSync(final AtomicInteger dpsDeltaInvocationAttributeSync) {
        this.dpsDeltaInvocationAttributeSync = dpsDeltaInvocationAttributeSync;
    }

    public void increaseDpsDeltaInvocationAttributeSync() {
        dpsDeltaInvocationAttributeSync.incrementAndGet();
    }
}
