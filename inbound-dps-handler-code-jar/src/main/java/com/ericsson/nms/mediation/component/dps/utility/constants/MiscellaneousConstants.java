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

package com.ericsson.nms.mediation.component.dps.utility.constants;

/**
 * General constants shared among the handlers.
 */
public final class MiscellaneousConstants {

    // Header names (MOs)
    public static final String INVOKING_FDN_HEADER_NAME = "fdn";
    public static final String ME_CONTEXT_HEADER_NAME = "meContextFdn";

    // Header attributes (names)
    public static final String PO_ID_HEADER_NAME = "poId";
    public static final String OSS_PREFIX_HEADER_NAME = "ossPrefix";
    public static final String OSS_MODEL_IDENTITY_HEADER_NAME = "ossModelIdendity";
    public static final String IP_ADDRESS_HEADER_NAME = "ipAddress";
    public static final String NODE_TYPE_HEADER_NAME = "nodeType";
    public static final String CM_FUNCTION_SYNC_ACTION_HEADER_NAME = "cmFunctionSyncAction";
    public static final String OSS_MODEL_IDENTITY_UPDATED_HEADER_NAME = "ossModelIdentityUpdated";
    public static final String SOFTWARE_SYNC_FAILURE_HEADER_NAME = "softwareSyncFailure";
    public static final String SOFTWARE_SYNC_FAILURE_MSG_HEADER_NAME = "softwareSyncFailureMsg";

    // Header attributes (values)
    public static final String SWSYNCINITIATOR_CM_FUNCTION_SYNC_VALUE = "CM_FUNCTION_SYNC";

    // Header names (other)
    public static final String SYNC_START_TIME_HEADER_NAME = "sync start time";
    public static final String DELTA_SYNC_START_TIME_HEADER_NAME = "deltaSyncStartTime";
    public static final String CLIENT_TYPE_HEADER_NAME = "clientType";
    public static final String SYNC_TYPE_HEADER_NAME = "synctype";
    public static final String LARGE_NODE_FLAG = "largeNode";
    public static final String MO_INSTANCE_COUNT = "moInstanceCount";
    public static final String SOFTWARE_SYNC_SUCCESS = "softwareSyncSuccess";
    public static final String SOFTWARE_SYNC_MESSAGE = "softwareSyncCompletionMsg";
    public static final String IS_FIRST_SYNC_FLAG = "isFirstSyncInEnm";

    // MOs
    public static final String SHM_INVENTORY_MO_WITH_EQUALS = "Inventory=";

    // Lookup strings
    public static final String TX_MANAGER_LOOKUP_STRING = "java:jboss/TransactionManager";

    // Recording
    public static final String EVENT_SOURCE = "Flow Engine";
    public static final String EVENT_RESOURCE = "DPS Database";
    public static final String COMPLETE_SYNC_LABEL = "COMPLETE SYNC";
    public static final String HANDLER_NAME_DELTA_SYNC = "DELTA SYNC";
    public static final String GENERATION_COUNTER_HEADER_NAME = "generationCounter";
    public static final String HANDLER_NAME_TOPOLOGY_SYNC = "TOPOLOGY SYNC";
    public static final String HANDLER_NAME_ATTRIBUTE_SYNC = "ATTRIBUTE SYNC";
    public static final String TIME_TAKEN_TO_DELETE_MOS = "TIME_TAKEN_TO_DELETE_MOS";
    public static final String NUMBER_MOS_DELETED = "NUMBER_MOS_DELETED";
    public static final String TIME_TAKEN_TO_CREATE_MOS = "TIME_TAKEN_TO_CREATE_MOS";
    public static final String NUMBER_MOS_CREATED = "NUMBER_MOS_CREATED";
    public static final String TIME_TAKEN_READ_TOPOLOGY = "TIME_TAKEN_READ_TOPOLOGY";
    public static final String PRE_TOPOLOGY_SIZE = "PRE_TOPOLOGY_SIZE";
    public static final String TOTAL_NUMBER_OF_MOS = "TOTAL_NUMBER_OF_MOS";
    public static final String ROOT_MO = "ROOT_MO";
    public static final String GENERATION_COUNTER_DIFFERENCE = "GENERATION_COUNTER_DIFFERENCE";

    public static final long DEFAULT_ATTRIBUTE_WRITE_MAX_TIME = 4 * 60 * 1000;

    public static final double DEFAULT_ATTRIBUTES_BATCH_SIZE_FACTOR = 0.75;
    public static final double REDUCED_ATTRIBUTES_BATCH_SIZE_FACTOR = 0.5;

    // Large Node MO count threshold
    public static final int LARGE_NODE_MO_THRESHOLD = 15000;
    public static final int DEFAULT_DPS_WRITE_BATCH_SIZE = 3000;

    public static final String TOPOLOGY_DPS_WRITE_BATCH_SIZE = "topology_dps_write_batch_size";
    public static final String ATTRIBUTES_DPS_WRITE_MAX_TIME = "attributes_dps_write_max_time";

    private MiscellaneousConstants() {}
}
