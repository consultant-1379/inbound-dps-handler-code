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

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;

/**
 * Records metrics about Mim Switch operation such as number of successful invocations and failed invocations.
 */
@ApplicationScoped
@InstrumentedBean(description = "Records metrics about Mim Switch operation such as number of successful invocations and failed invocations",
        displayName = "Mim Switch Metrics")
public class MimSwitchInstrumentation {

    private final AtomicInteger numberOfSuccessfulMibUpgrade = new AtomicInteger(0);
    private final AtomicInteger numberOfFailedMibUpgrade = new AtomicInteger(0);

    public void increaseNumberOfMibUpgradePerformed(final boolean result) {
        if (result) {
            numberOfSuccessfulMibUpgrade.incrementAndGet();
        } else {
            numberOfFailedMibUpgrade.incrementAndGet();
        }
    }

    /**
     * Number of successful Mib Upgrades.
     *
     * @return numberOfSuccessfulMibUpgrade
     */
    @MonitoredAttribute(displayName = "Number of MIB upgrade performed successfully",
            visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
            category = MonitoredAttribute.Category.UTILIZATION, interval = MonitoredAttribute.Interval.ONE_MIN,
            collectionType = MonitoredAttribute.CollectionType.TRENDSUP)
    public synchronized int getNumberOfSuccessfulMibUpgrade() {
        return numberOfSuccessfulMibUpgrade.get();
    }

    /**
     * Number of failed Mib Upgrades.
     *
     * @return numberOfFailedMibUpgrade
     */
    @MonitoredAttribute(displayName = "Number of failed MIB upgrade",
            visibility = MonitoredAttribute.Visibility.ALL, units = MonitoredAttribute.Units.NONE,
            category = MonitoredAttribute.Category.UTILIZATION, interval = MonitoredAttribute.Interval.ONE_MIN,
            collectionType = MonitoredAttribute.CollectionType.TRENDSUP)
    public synchronized int getNumberOfFailedMibUpgrade() {
        return numberOfFailedMibUpgrade.get();
    }
}
