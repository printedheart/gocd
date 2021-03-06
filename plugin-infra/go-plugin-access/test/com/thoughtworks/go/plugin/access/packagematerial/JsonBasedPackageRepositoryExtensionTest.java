/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.packagematerial.JsonBasedPackageRepositoryExtension.EXTENSION_NAME;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class JsonBasedPackageRepositoryExtensionTest {

    public static final String PLUGIN_ID = "plugin-id";
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private PluginManager pluginManager;
    private JsonBasedPackageRepositoryExtension jsonBasedPackageRepositoryExtension;
    private RepositoryConfiguration repositoryConfiguration;
    private PackageConfiguration packageConfiguration;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        pluginManager = Mockito.mock(PluginManager.class);
        jsonBasedPackageRepositoryExtension = new JsonBasedPackageRepositoryExtension(pluginManager);

        repositoryConfiguration = new RepositoryConfiguration();
        repositoryConfiguration.add(new PackageMaterialProperty("key-one", "value-one"));
        repositoryConfiguration.add(new PackageMaterialProperty("key-two", "value-two"));

        packageConfiguration = new PackageConfiguration();
        packageConfiguration.add(new PackageMaterialProperty("key-three", "value-three"));
        packageConfiguration.add(new PackageMaterialProperty("key-four", "value-four"));

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, asList("1.0"))).thenReturn("1.0");
    }

    @Test
    public void shouldTalkToPluginToGetRepositoryConfiguration() throws Exception {
        String expectedRequestBody = null;

        String expectedResponseBody = "{" +
                "\"key-one\":{}," +
                "\"key-two\":{\"default-value\":\"two\",\"part-of-identity\":true,\"secure\":true,\"required\":true,\"display-name\":\"display-two\",\"display-order\":\"1\"}," +
                "\"key-three\":{\"default-value\":\"three\",\"part-of-identity\":false,\"secure\":false,\"required\":false,\"display-name\":\"display-three\",\"display-order\":\"2\"}" +
                "}";

        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(expectedResponseBody));

        RepositoryConfiguration repositoryConfiguration = jsonBasedPackageRepositoryExtension.getRepositoryConfiguration(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), EXTENSION_NAME, "1.0", JsonBasedPackageRepositoryExtension.REQUEST_REPOSITORY_CONFIGURATION, expectedRequestBody);
        assertPropertyConfiguration((PackageMaterialProperty) repositoryConfiguration.get("key-one"), "key-one", null, true, true, false, "", 0);
        assertPropertyConfiguration((PackageMaterialProperty) repositoryConfiguration.get("key-two"), "key-two", "two", true, true, true, "display-two", 1);
        assertPropertyConfiguration((PackageMaterialProperty) repositoryConfiguration.get("key-three"), "key-three", "three", false, false, false, "display-three", 2);
    }

    @Test
    public void shouldTalkToPluginToGetPackageConfiguration() throws Exception {
        String expectedRequestBody = null;

        String expectedResponseBody = "{" +
                "\"key-one\":{}," +
                "\"key-two\":{\"default-value\":\"two\",\"part-of-identity\":true,\"secure\":true,\"required\":true,\"display-name\":\"display-two\",\"display-order\":\"1\"}," +
                "\"key-three\":{\"default-value\":\"three\",\"part-of-identity\":false,\"secure\":false,\"required\":false,\"display-name\":\"display-three\",\"display-order\":\"2\"}" +
                "}";
        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(expectedResponseBody));

        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = jsonBasedPackageRepositoryExtension.getPackageConfiguration(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), EXTENSION_NAME, "1.0", JsonBasedPackageRepositoryExtension.REQUEST_PACKAGE_CONFIGURATION, expectedRequestBody);
        assertPropertyConfiguration((PackageMaterialProperty) packageConfiguration.get("key-one"), "key-one", null, true, true, false, "", 0);
        assertPropertyConfiguration((PackageMaterialProperty) packageConfiguration.get("key-two"), "key-two", "two", true, true, true, "display-two", 1);
        assertPropertyConfiguration((PackageMaterialProperty) packageConfiguration.get("key-three"), "key-three", "three", false, false, false, "display-three", 2);
    }

    @Test
    public void shouldTalkToPluginToCheckIfRepositoryConfigurationIsValid() throws Exception {
        String expectedRequestBody = "{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}";

        String expectedResponseBody = "[{\"key\":\"key-one\",\"message\":\"incorrect value\"},{\"message\":\"general error\"}]";

        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(expectedResponseBody));

        ValidationResult validationResult = jsonBasedPackageRepositoryExtension.isRepositoryConfigurationValid(PLUGIN_ID, repositoryConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), EXTENSION_NAME, "1.0", JsonBasedPackageRepositoryExtension.REQUEST_VALIDATE_REPOSITORY_CONFIGURATION, expectedRequestBody);
        assertValidationError(validationResult.getErrors().get(0), "key-one", "incorrect value");
        assertValidationError(validationResult.getErrors().get(1), "", "general error");
    }

    @Test
    public void shouldTalkToPluginToCheckIfPackageConfigurationIsValid() throws Exception {
        String expectedRequestBody = "{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}," +
                "\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}}";

        String expectedResponseBody = "[{\"key\":\"key-one\",\"message\":\"incorrect value\"},{\"message\":\"general error\"}]";

        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(expectedResponseBody));

        ValidationResult validationResult = jsonBasedPackageRepositoryExtension.isPackageConfigurationValid(PLUGIN_ID, packageConfiguration, repositoryConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), EXTENSION_NAME, "1.0", JsonBasedPackageRepositoryExtension.REQUEST_VALIDATE_PACKAGE_CONFIGURATION, expectedRequestBody);
        assertValidationError(validationResult.getErrors().get(0), "key-one", "incorrect value");
        assertValidationError(validationResult.getErrors().get(1), "", "general error");
    }

    @Test
    public void shouldTalkToPluginToGetLatestModification() throws Exception {
        String expectedRequestBody = "{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}," +
                "\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}}";


        String expectedResponseBody = "{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\"," +
                "\"trackbackUrl\":\"http:\\\\localhost:9999\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}";

        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(expectedResponseBody));

        PackageRevision packageRevision = jsonBasedPackageRepositoryExtension.getLatestRevision(PLUGIN_ID, packageConfiguration, repositoryConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), EXTENSION_NAME, "1.0", JsonBasedPackageRepositoryExtension.REQUEST_LATEST_REVISION, expectedRequestBody);
        assertPackageRevision(packageRevision, "abc.rpm", "some-user", "2011-07-14T19:43:37.100Z", "comment", "http:\\localhost:9999");
    }

    @Test
    public void shouldTalkToPluginToGetLatestModificationSinceLastRevision() throws Exception {
        String expectedRequestBody = "{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}," +
                "\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}," +
                "\"previous-revision\":{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-13T19:43:37.100Z\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}}";


        String expectedResponseBody = "{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\"," +
                "\"trackbackUrl\":\"http:\\\\localhost:9999\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}";

        Date timestamp = new SimpleDateFormat(DATE_FORMAT).parse("2011-07-13T19:43:37.100Z");
        Map data = new LinkedHashMap();
        data.put("dataKeyOne", "data-value-one");
        data.put("dataKeyTwo", "data-value-two");
        PackageRevision previouslyKnownRevision = new PackageRevision("abc.rpm", timestamp, "someuser", "comment", null, data);

        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(expectedResponseBody));

        PackageRevision packageRevision = jsonBasedPackageRepositoryExtension.latestModificationSince(PLUGIN_ID, packageConfiguration, repositoryConfiguration, previouslyKnownRevision);

        assertRequest(requestArgumentCaptor.getValue(), EXTENSION_NAME, "1.0", JsonBasedPackageRepositoryExtension.REQUEST_LATEST_REVISION_SINCE, expectedRequestBody);
        assertPackageRevision(packageRevision, "abc.rpm", "some-user", "2011-07-14T19:43:37.100Z", "comment", "http:\\localhost:9999");
    }

    @Test
    public void shouldTalkToPluginToCheckRepositoryConnectionSuccessful() throws Exception {
        String expectedRequestBody = "{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}";

        String expectedResponseBody = "{\"status\":\"success\",messages=[\"message-one\",\"message-two\"]}";

        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(expectedResponseBody));

        Result result = jsonBasedPackageRepositoryExtension.checkConnectionToRepository(PLUGIN_ID, repositoryConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), EXTENSION_NAME, "1.0", JsonBasedPackageRepositoryExtension.REQUEST_CHECK_REPOSITORY_CONNECTION, expectedRequestBody);
        assertSuccessResult(result, asList("message-one", "message-two"));
    }

    @Test
    public void shouldTalkToPluginToCheckRepositoryConnectionFailure() throws Exception {
        String expectedRequestBody = "{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}";

        String expectedResponseBody = "{\"status\":\"failed\",messages=[\"message-one\",\"message-two\"]}";

        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(expectedResponseBody));

        Result result = jsonBasedPackageRepositoryExtension.checkConnectionToRepository(PLUGIN_ID, repositoryConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), EXTENSION_NAME, "1.0", JsonBasedPackageRepositoryExtension.REQUEST_CHECK_REPOSITORY_CONNECTION, expectedRequestBody);
        assertFailureResult(result, asList("message-one", "message-two"));
    }

    @Test
    public void shouldTalkToPluginToCheckPackageConnectionSuccessful() throws Exception {
        String expectedRequestBody = "{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}," +
                "\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}}";

        String expectedResponseBody = "{\"status\":\"success\",messages=[\"message-one\",\"message-two\"]}";

        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(expectedResponseBody));

        Result result = jsonBasedPackageRepositoryExtension.checkConnectionToPackage(PLUGIN_ID, packageConfiguration, repositoryConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), EXTENSION_NAME, "1.0", JsonBasedPackageRepositoryExtension.REQUEST_CHECK_PACKAGE_CONNECTION, expectedRequestBody);
        assertSuccessResult(result, asList("message-one", "message-two"));
    }

    @Test
    public void shouldTalkToPluginToCheckPackageConnectionFailure() throws Exception {
        String expectedRequestBody = "{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}," +
                "\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}}";

        String expectedResponseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";

        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(expectedResponseBody));

        Result result = jsonBasedPackageRepositoryExtension.checkConnectionToPackage(PLUGIN_ID, packageConfiguration, repositoryConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), EXTENSION_NAME, "1.0", JsonBasedPackageRepositoryExtension.REQUEST_CHECK_PACKAGE_CONNECTION, expectedRequestBody);
        assertFailureResult(result, asList("message-one", "message-two"));
    }

    @Test
    public void shouldHandleExceptionDuringPluginInteraction() throws Exception {
        when(pluginManager.isPluginOfType(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenThrow(new RuntimeException("exception-from-plugin"));
        try {
            jsonBasedPackageRepositoryExtension.checkConnectionToPackage(PLUGIN_ID, packageConfiguration, repositoryConfiguration);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Interaction with plugin with id 'plugin-id' implementing 'package-repository' extension failed while requesting for 'check-package-connection'. Reason: [exception-from-plugin]"));
        }
    }

    private void assertPropertyConfiguration(PackageMaterialProperty property, String key, String value, boolean partOfIdentity, boolean required, boolean secure, String displayName, int displayOrder) {
        assertThat(property.getKey(), is(key));
        assertThat(property.getValue(), is(value));
        assertThat(property.getOption(Property.PART_OF_IDENTITY), is(partOfIdentity));
        assertThat(property.getOption(Property.REQUIRED), is(required));
        assertThat(property.getOption(Property.SECURE), is(secure));
        assertThat(property.getOption(Property.DISPLAY_NAME), is(displayName));
        assertThat(property.getOption(Property.DISPLAY_ORDER), is(displayOrder));
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        assertThat(goPluginApiRequest.extension(), is(extensionName));
        assertThat(goPluginApiRequest.extensionVersion(), is(version));
        assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertThat(goPluginApiRequest.requestBody(), is(requestBody));
    }

    private void assertValidationError(ValidationError validationError, String expectedKey, String expectedMessage) {
        assertThat(validationError.getKey(), is(expectedKey));
        assertThat(validationError.getMessage(), is(expectedMessage));
    }

    private void assertPackageRevision(PackageRevision packageRevision, String revision, String user, String timestamp, String comment, String trackbackUrl) throws ParseException {
        assertThat(packageRevision.getRevision(), is(revision));
        assertThat(packageRevision.getUser(), is(user));
        assertThat(packageRevision.getTimestamp(), is(new SimpleDateFormat(DATE_FORMAT).parse(timestamp)));
        assertThat(packageRevision.getRevisionComment(), is(comment));
        assertThat(packageRevision.getTrackbackUrl(), is(trackbackUrl));
        assertThat(packageRevision.getData().size(), is(2));
        assertThat(packageRevision.getDataFor("dataKeyOne"), is("data-value-one"));
        assertThat(packageRevision.getDataFor("dataKeyTwo"), is("data-value-two"));
    }

    private void assertSuccessResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.getMessages(), is(messages));
    }

    private void assertFailureResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getMessages(), is(messages));
    }
}