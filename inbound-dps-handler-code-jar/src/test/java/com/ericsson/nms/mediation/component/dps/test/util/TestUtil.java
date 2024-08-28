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

package com.ericsson.nms.mediation.component.dps.test.util;

import java.lang.reflect.Field;

public final class TestUtil {
    private TestUtil() {}

    /**
     * get the value of a private field by reflection.
     * @param classObject
     *            Class of the instance wanted to spy
     * @param name
     *            name of the field to recover
     * @param instance
     *            current instance
     * @return value of the field desired
     * @throws Exception
     *             if there are problems
     */
    public static Object getFieldValue(final Class<?> classObject, final String name, final Object instance) throws Exception {
        final Field field = createField(classObject, name);
        return field.get(instance);
    }

    /**
     * set the value of a private field by reflection.
     * @param fieldName
     *            name of the field to recover
     * @param fieldValue
     *            of the field desired to set
     * @param classObject
     *            Class of the instance wanted to spy
     * @param classInstance
     *            current instance
     * @throws Exception
     *             if there are problems* @param classObject
     */
    public static void setFieldValue(final String fieldName, final Object fieldValue, final Class<?> classObject, final Object classInstance)
            throws Exception {
        final Field field = createField(classObject, fieldName);
        field.set(classInstance, fieldValue);
    }

    /**
     * create the field object and set it as accessible.
     * @param classObject
     *            Class of the instance wanted to spy
     * @param name
     *            of the field to recover
     * @return Field object created
     * @throws Exception
     *             if there errors
     */
    private static Field createField(final Class<?> classObject, final String name) throws Exception {
        final Field field = classObject.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
