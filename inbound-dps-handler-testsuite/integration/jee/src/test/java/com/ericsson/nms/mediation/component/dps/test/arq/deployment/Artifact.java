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

package com.ericsson.nms.mediation.component.dps.test.arq.deployment;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.jboss.arquillian.protocol.servlet.arq514hack.descriptors.api.application.ApplicationDescriptor;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;

public final class Artifact {
    public static final String COM_ERICSSON_NMS_MEDIATION_DPS_HANDLER_CODE_EAR =
            "com.ericsson.nms.mediation.component:inbound-dps-handler-code-ear:ear:?";
    public static final String MOCKITO = "org.mockito:mockito-all:jar:?";
    public static final String COM_ERICSSON_OSS_ITPF_PIB = "com.ericsson.oss.itpf.common:PlatformIntegrationBridge-ear:ear:?";
    public static final String DPS_TEST_SUPPORT = "com.ericsson.oss.itpf.datalayer.dps:dps-test-support:jar:?";

    private Artifact() {}

    /**
     * Maven resolver that will try to resolve dependencies using pom.xml of the project where this class is located.
     * @return MavenDependencyResolver
     */
    private static PomEquippedResolveStage getMavenResolver() {
        return Maven.resolver().loadPomFromFile("pom.xml");
    }

    /**
     * Resolve artifacts without dependencies.
     * @param artifactCoordinates
     *            coordinates
     * @return artifacts
     *         first object in a file list with artifactCoordinates
     */
    public static File resolveArtifactWithoutDependencies(final String artifactCoordinates) {
        final File artifact = getMavenResolver().resolve(artifactCoordinates).withoutTransitivity().asSingleFile();
        if (artifact == null) {
            throw new IllegalStateException("Artifact with coordinates " + artifactCoordinates + " was not resolved");
        }
        return artifact;
    }

    public static File[] resolveArtifactWithDependencies(final String artifactCoordinates) {
        final File[] artifacts = getMavenResolver().resolve(artifactCoordinates).withTransitivity().asFile();
        if (artifacts == null || artifacts.length == 0) {
            throw new IllegalStateException("Artifact with coordinates " + artifactCoordinates + " was not resolved");
        }
        return artifacts;
    }

    public static void createCustomApplicationXmlFile(final EnterpriseArchive ear, final String webModuleName) {
        final Node node = ear.get("META-INF/application.xml");
        final ApplicationDescriptor desc = Descriptors.importAs(ApplicationDescriptor.class).fromStream(node.getAsset().openStream());
        desc.webModule(webModuleName + ".war", webModuleName);
        ear.delete(node.getPath());
        ear.setApplicationXML(() -> new ByteArrayInputStream(desc.exportAsString().getBytes()));
    }
}
