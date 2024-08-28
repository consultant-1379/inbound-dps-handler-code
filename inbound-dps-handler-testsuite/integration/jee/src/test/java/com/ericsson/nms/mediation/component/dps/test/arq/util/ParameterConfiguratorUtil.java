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

package com.ericsson.nms.mediation.component.dps.test.arq.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows to send REST requests to add configuration parameters for Platform Integration Bridge.
 */
public abstract class ParameterConfiguratorUtil {
    private static final Logger logger = LoggerFactory.getLogger(ParameterConfiguratorUtil.class);

    public static void addConfigParameter(final String paramName, final String paramValue) throws MalformedURLException, IOException,
            ProtocolException {
        logger.debug("Adding '{}' parameter with value: '{}'...", paramName, paramValue);

        final URL url = new URL(generateAddParamRestUrl(paramName, paramValue));
        final HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        final String userName = "pibUser";
        final String password = "3ric550N*";
        final String encoding = DatatypeConverter.printBase64Binary((userName + ":" + password).getBytes(StandardCharsets.UTF_8));
        httpConnection.setRequestProperty("Authorization", "Basic " + encoding);
        httpConnection.setRequestMethod("GET");
        logger.debug("Connection response code for changing parameter: '{}' is: '{}'.", paramName, httpConnection.getResponseCode());
    }

    private static String generateAddParamRestUrl(final String paramName, final String paramValue) {
        final String jvmInstanceId = System.getProperty("com.ericsson.oss.sdk.node.identifier", "DPS-HDLR-JEE-TEST");
        final Integer portOffset = Integer.getInteger("jboss.socket.binding.port-offset", 629);
        final String jbossAddress = System.getProperty("jboss.bind.address.internal", "localhost");
        final int port = 8080 + portOffset;

        final String url = new StringBuilder()
                .append("http://")
                .append(jbossAddress)
                .append(":")
                .append(port)
                .append("/pib/configurationService/updateConfigParameterValue")
                .append("?paramName=")
                .append(paramName)
                .append("&paramValue=")
                .append(paramValue)
                .append("&serviceIdentifier=cm-validity-controller")
                .append("&paramType=long")
                .append("&paramScopeType=JVM_AND_SERVICE")
                .append("&jvmIdentifier=")
                .append(jvmInstanceId).toString();
        logger.debug("Constructed URL: '{}'", url);
        return url;
    }

}
