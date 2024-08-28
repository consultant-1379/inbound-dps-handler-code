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
 * Records metrics as part of Sync Node including average DPS operation times, Node Sync times, and Node Sync details.
 */
@ApplicationScoped
@InstrumentedBean(description = "Records metrics as part of Sync Node including average DPS operation times,"
        + "Node Sync times, and Node Sync details", displayName = "Sync Node Metrics - DPS Operations")
public class DpsHandlerInstrumentation {
    private final DpsHandlerInstrumentationSamples dpsHandlerSamples = new DpsHandlerInstrumentationSamples();

    private AtomicInteger dpsInvocationTopologySync = new AtomicInteger(0);
    private AtomicInteger dpsInvocationAttributeSync = new AtomicInteger(0);
    private AtomicInteger dpsInvocationController = new AtomicInteger(0);
    private AtomicInteger dpsCounterForSuccessfulSync = new AtomicInteger(0);
    private AtomicInteger dpsNumberOfFailedSyncs = new AtomicInteger(0);

    private long averageDpsTopologyDataTimeTaken;
    private long averageDpsAttributeDataTimeTaken;
    private long averageNumberOfMosBeingSynced;
    private long averageNumberOfAttrBeingSynced;
    private long averageOverallSyncTimeTaken;
    private long maxOverallSyncTimeTaken;

    public void increaseDpsControllerInvocations() {
        dpsInvocationController.incrementAndGet();
    }

    public void setDpsInvocationController(final AtomicInteger dpsInvocationController) {
        this.dpsInvocationController = dpsInvocationController;
    }

    /**
     * Number of DPS Invocations for Controller Data.
     * @return the number of times DPS has been invoked for controller data.
     */
    @MonitoredAttribute(displayName = "Number of DPS Invocations for Controller Data", visibility = Visibility.ALL,
            units = Units.NONE, category = Category.UTILIZATION, interval = Interval.ONE_MIN, collectionType = CollectionType.TRENDSUP)
    public int getDpsInvocationController() {
        return dpsInvocationController.get();
    }

    public void increaseDpsTopologySyncInvocations() {
        dpsInvocationTopologySync.incrementAndGet();
    }

    public void setDpsInvocationTopologySync(final AtomicInteger dpsInvocationTopologySync) {
        this.dpsInvocationTopologySync = dpsInvocationTopologySync;
    }

    /**
     * Number of DPS Invocations for Topology Sync Data.
     * @return the number of times DPS has been invoked for topology sync data.
     */
    @MonitoredAttribute(displayName = "Number of DPS Invocations for Topology Sync Data", visibility = Visibility.ALL,
            units = Units.NONE, category = Category.UTILIZATION, interval = Interval.ONE_MIN, collectionType = CollectionType.TRENDSUP)
    public int getDpsInvocationTopologySync() {
        return dpsInvocationTopologySync.get();
    }

    public void setDpsInvocationAttributeSync(final AtomicInteger dpsInvocationAttributeSync) {
        this.dpsInvocationAttributeSync = dpsInvocationAttributeSync;
    }

    public void increaseDpsAttributeSyncInvocations() {
        dpsInvocationAttributeSync.incrementAndGet();
    }

    /**
     * Number of DPS Invocations for Attribute Sync Data.
     * @return the number of times DPS has been invoked for attribute sync data.
     */
    @MonitoredAttribute(displayName = "Number of DPS Invocations for Attribute Sync Data", visibility = Visibility.ALL,
            units = Units.NONE, category = Category.UTILIZATION, interval = Interval.ONE_MIN, collectionType = CollectionType.TRENDSUP)
    public int getDpsInvocationAttributeSync() {
        return dpsInvocationAttributeSync.get();
    }

    public void addToNumberOfMosBeingSyncedSamples(final Long numberOfMosBeingSynced) {
        dpsHandlerSamples.numberOfMosBeingSyncedSamples.add(numberOfMosBeingSynced);
    }

    public void setAverageNumberOfMosBeingSynced(final long averageNumberOfMosBeingSynced) {
        this.averageNumberOfMosBeingSynced = averageNumberOfMosBeingSynced;
    }

    /**
     * Average Number of Managed Objects being Synchronize.
     * @return the average number of Managed Objects being sync'd during Topology Sync.
     */
    @MonitoredAttribute(displayName = "Average Number of Managed Objects being Synchronized", visibility = Visibility.ALL,
            units = Units.NONE, category = Category.PERFORMANCE, interval = Interval.ONE_MIN, collectionType = CollectionType.DYNAMIC)
    public long getAverageNumberOfMosBeingSynced() {
        setAverageNumberOfMosBeingSynced(InstrumentationUtil.calculateAverageAndReset(dpsHandlerSamples.numberOfMosBeingSyncedSamples));
        // an attribute is required for the averageNumberOfMosBeingSyncd to
        // conform with SDK Instrumentation to expose the attribute using JMX
        return averageNumberOfMosBeingSynced;
    }

    public void addToNumberOfAttrBeingSyncedSamples(final Long numberOfAttrBeingSynced) {
        dpsHandlerSamples.numberOfAttrBeingSyncedSamples.add(numberOfAttrBeingSynced);
    }

    public void setAverageNumberOfAttrBeingSynced(final long numberOfAttrBeingSynced) {
        this.averageNumberOfAttrBeingSynced = numberOfAttrBeingSynced;
    }

    /**
     * Average Number of Attributes being Synchronized.
     * @return the average number of attributes being sync'd during Attribute Sync.
     */
    @MonitoredAttribute(displayName = "Average Number of Attributes being Synchronized", visibility = Visibility.ALL,
            units = Units.NONE, category = Category.PERFORMANCE, interval = Interval.ONE_MIN, collectionType = CollectionType.DYNAMIC)
    public long getAverageNumberOfAttrBeingSynced() {
        setAverageNumberOfAttrBeingSynced(InstrumentationUtil.calculateAverageAndReset(dpsHandlerSamples.numberOfAttrBeingSyncedSamples));
        // an attribute is required for the averageNumberOfAttrBeingSyncd to
        // conform with SDK Instrumentation to expose the attribute using JMX
        return averageNumberOfAttrBeingSynced;
    }

    public void addToAverageDpsTopologyDataTimesTaken(final Long averageDpsTopologyTimeTaken) {
        dpsHandlerSamples.dpsTopologyDataTimesSamples.add(averageDpsTopologyTimeTaken);
    }

    public void setAverageDpsTopologyDataTimeTaken(final long averageDpsTopologyDataTimeTaken) {
        this.averageDpsTopologyDataTimeTaken = averageDpsTopologyDataTimeTaken;
    }

    /**
     * Average Time Taken to Add Topology Data to DPS.
     * @return the average time taken to add Topology Sync data to the database.
     */
    @MonitoredAttribute(displayName = "Average Time Taken to Add Topology Data to DPS", visibility = Visibility.ALL,
            units = Units.MILLISECONDS, category = Category.PERFORMANCE, interval = Interval.ONE_MIN, collectionType = CollectionType.DYNAMIC)
    public long getAverageDpsTopologyDataTimeTaken() {
        setAverageDpsTopologyDataTimeTaken(InstrumentationUtil.calculateAverageAndReset(dpsHandlerSamples.dpsTopologyDataTimesSamples));
        return averageDpsTopologyDataTimeTaken;
    }

    public void addToAverageDpsAttributeDataTimesTaken(final long averageDpsAttributeDataTimeTaken) {
        dpsHandlerSamples.dpsAttributeDataTimesSamples.add(averageDpsAttributeDataTimeTaken);
    }

    public void setAverageDpsAttributeDataTimeTaken(final long averageDpsAttributeDataTimeTaken) {
        this.averageDpsAttributeDataTimeTaken = averageDpsAttributeDataTimeTaken;
    }

    /**
     * Average Time Taken to Add Attribute Data to DPS.
     * @return the average time taken to add Attribute Sync data to the database.
     */
    @MonitoredAttribute(displayName = "Average Time Taken to Add Attribute Data to DPS", visibility = Visibility.ALL,
            units = Units.MILLISECONDS, category = Category.PERFORMANCE, interval = Interval.ONE_MIN, collectionType = CollectionType.DYNAMIC)
    public long getAverageDpsAttributeDataTimeTaken() {
        setAverageDpsAttributeDataTimeTaken(InstrumentationUtil.calculateAverageAndReset(dpsHandlerSamples.dpsAttributeDataTimesSamples));
        return averageDpsAttributeDataTimeTaken;
    }

    /**
     * Number of Successful Synchronisations.
     * @return the Number of Successful Syncs.
     */
    @MonitoredAttribute(displayName = "Number of Successful Synchronisations", visibility = Visibility.ALL,
            units = Units.NONE, category = Category.UTILIZATION, interval = Interval.FIVE_MIN, collectionType = CollectionType.TRENDSUP)
    public int getDpsCounterForSuccessfulSync() {
        return dpsCounterForSuccessfulSync.get();
    }

    public void setDpsCounterForSuccessfulSync(final AtomicInteger dpsCounterForSuccessfulSync) {
        this.dpsCounterForSuccessfulSync = dpsCounterForSuccessfulSync;
    }

    public void increaseDpsCounterForSuccessfulSync() {
        this.dpsCounterForSuccessfulSync.incrementAndGet();
    }

    /**
     * Average Time Taken to Sync Node.
     * @return the average Overall Sync Time Taken (From DPS Controller to Adding Attribute Sync Data to Database)
     */
    @MonitoredAttribute(displayName = "Average Time Taken to Sync Node", visibility = Visibility.ALL,
            units = Units.MILLISECONDS, category = Category.PERFORMANCE, interval = Interval.FIVE_MIN, collectionType = CollectionType.DYNAMIC)
    public long getAverageOverallSyncTimeTaken() {
        setAverageOverallSyncTimeTaken(InstrumentationUtil.calculateAverageAndReset(dpsHandlerSamples.overallSyncTimesSamples));
        return averageOverallSyncTimeTaken;
    }

    public void setAverageOverallSyncTimeTaken(final long averageOverallSyncTimeTaken) {
        this.averageOverallSyncTimeTaken = averageOverallSyncTimeTaken;
    }

    public void addToAverageOverallSyncTimesTaken(final Long averageOverallSyncTimeTaken) {
        dpsHandlerSamples.overallSyncTimesSamples.add(averageOverallSyncTimeTaken);
    }

    /**
     * Maximum Time Taken to Sync a Node.
     * @return the Maximum Overall Sync Time Taken
     */
    @MonitoredAttribute(displayName = "Maximum Time Taken to Sync a Node", visibility = Visibility.ALL,
            units = Units.MILLISECONDS, category = Category.PERFORMANCE, interval = Interval.ONE_MIN, collectionType = CollectionType.DYNAMIC)
    public long getMaxOverallSyncTimeTaken() {
        setMaxOverallSyncTimeTaken(InstrumentationUtil.calculateMaxAndReset(dpsHandlerSamples.maxOverallSyncTimesSamples));
        return maxOverallSyncTimeTaken;
    }

    public void setMaxOverallSyncTimeTaken(final long maxOverallSyncTimeTaken) {
        this.maxOverallSyncTimeTaken = maxOverallSyncTimeTaken;
    }

    public void addToMaxOverallSyncTimesTaken(final Long maxOverallSyncTimeTaken) {
        dpsHandlerSamples.maxOverallSyncTimesSamples.add(maxOverallSyncTimeTaken);
    }

    /**
     * Number of Failed Syncs in Sync DPS Handler.
     * @return the dpsNumberOfFailedSyncs
     */
    @MonitoredAttribute(displayName = "Number of Failed Syncs in Sync DPS Handler", visibility = Visibility.ALL,
            units = Units.NONE, category = Category.PERFORMANCE, interval = Interval.ONE_MIN, collectionType = CollectionType.TRENDSUP)
    public int getDpsNumberOfFailedSyncs() {
        return dpsNumberOfFailedSyncs.get();
    }

    public void setDpsNumberOfFailedSyncs(final AtomicInteger dpsNumberOfFailedSyncs) {
        this.dpsNumberOfFailedSyncs = dpsNumberOfFailedSyncs;
    }

    public void increaseDpsNumberOfFailedSyncs() {
        dpsNumberOfFailedSyncs.incrementAndGet();
    }

    private final class DpsHandlerInstrumentationSamples {
        private final List<Long> dpsTopologyDataTimesSamples = new ArrayList<>();
        private final List<Long> dpsAttributeDataTimesSamples = new ArrayList<>();
        private final List<Long> numberOfMosBeingSyncedSamples = new ArrayList<>();
        private final List<Long> numberOfAttrBeingSyncedSamples = new ArrayList<>();
        private final List<Long> overallSyncTimesSamples = new ArrayList<>();
        private final List<Long> maxOverallSyncTimesSamples = new ArrayList<>();

        private DpsHandlerInstrumentationSamples() {
        }
    }
}
