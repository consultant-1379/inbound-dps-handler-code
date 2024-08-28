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

package com.ericsson.nms.mediation.component.dps.test.arq.mock.dps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.jobs.JobStatus;
import com.ericsson.oss.itpf.datalayer.dps.object.upgrade.AppliedMibUpgrade;
import com.ericsson.oss.itpf.datalayer.dps.object.upgrade.DataUpgradeHandler;
import com.ericsson.oss.itpf.datalayer.dps.object.upgrade.MibUpgrade;
import com.ericsson.oss.itpf.datalayer.dps.object.upgrade.MibUpgradeResult;
import com.ericsson.oss.itpf.datalayer.dps.object.upgrade.MibUpgradeScope;

@Singleton
@Local(DataUpgradeHandler.class)
@EJB(name = DataUpgradeHandler.JNDI_LOOKUP_NAME, beanInterface = DataUpgradeHandler.class)
public class DataUpgradeHandlerMock implements DataUpgradeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataUpgradeHandlerMock.class);

    @Override
    public List<MibUpgradeResult> performAllMibUpgrades(final MibUpgradeScope... mibUpgradeScopes) {
        LOGGER.info("[DataUpgradeHandler] Invoked performAllMibUpgrades: {}", Arrays.toString(mibUpgradeScopes));
        final List<MibUpgradeResult> mibUpgradeResults = new ArrayList<>(mibUpgradeScopes.length);
        for (final MibUpgradeScope mibUpgradeScope : mibUpgradeScopes) {
            final List<AppliedMibUpgrade> appliedMibUpgrades = new ArrayList<>(mibUpgradeScope.getMibUpgrades().size());
            for (final MibUpgrade mibUpgrade : mibUpgradeScope.getMibUpgrades()) {
                appliedMibUpgrades.add(createAppliedMibUpgrade(mibUpgrade));
            }
            mibUpgradeResults.add(createMibUpgradeResult(mibUpgradeScope, appliedMibUpgrades));
        }
        return null;
    }

    private static AppliedMibUpgrade createAppliedMibUpgrade(final MibUpgrade mibUpgrade) {
        return new AppliedMibUpgrade() {
            @Override
            public JobStatus getStatus() {
                return JobStatus.COMPLETED;
            }

            @Override
            public String getInfoMessage() {
                return null;
            }

            @Override
            public String getMibRootFdn() {
                return mibUpgrade.getFdn();
            }

            @Override
            public String getNewVersion() {
                return mibUpgrade.getNewVersion();
            }
        };
    }

    private static MibUpgradeResult createMibUpgradeResult(final MibUpgradeScope mibUpgradeScope, final List<AppliedMibUpgrade> appliedMibUpgrades) {
        return new MibUpgradeResult() {
            @Override
            public MibUpgradeScope getMibUpgradeScope() {
                return mibUpgradeScope;
            }

            @Override
            public List<AppliedMibUpgrade> getMibUpgrades() {
                return appliedMibUpgrades;
            }
        };
    }

}
