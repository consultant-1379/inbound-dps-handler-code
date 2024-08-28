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

package com.ericsson.nms.mediation.component.dps.common;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SyncTypeTest {
    private static final String DELTA_SYNC_TYPE = SyncType.DELTA.toString();

    SyncType syncType;

    @Before
    public void setUp() {
        syncType = SyncType.FULL;
    }

    @Test
    public void testSetterAndGetter() {
        syncType.setType(DELTA_SYNC_TYPE);
        assertEquals(DELTA_SYNC_TYPE, syncType.getType());
    }
}
