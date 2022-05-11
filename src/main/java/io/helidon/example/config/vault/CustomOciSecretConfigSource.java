package io.helidon.example.config.vault;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Enumeration;
import java.util.Map;

import io.helidon.common.Base64Value;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.secrets.Secrets;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.model.SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;


public class CustomOciSecretConfigSource implements ConfigSource {
    private static final String NAME = "CustomOciSecretConfigSource";
    private static final int ORDINAL = 200; // Default for MP is 100 so having a bigger value gives this higher priority
    private static Map<String, String> PROPERTIES;

    public CustomOciSecretConfigSource() {
        PROPERTIES =  Map.of(
                "javax.sql.DataSource.slDataSource.dataSource.password",
                getSecretContent(getOciSecretId())
        );
    }

    private String getSecretContent(String ociSecretsId) {
        try {
            /**
             * This is using user principal authentication via ~/.oci/config using "DEFAULT" profile. This can be switched
             * to other authentication type. For example, if the desired authentication is Instance principal, then
             * InstancePrincipalsAuthenticationDetailsProvider should be used instead. Please check OCI SDK for more
             * details.
             */
            ConfigFileAuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider("DEFAULT");
            Secrets secrets = SecretsClient.builder().
                              build(provider);
            /**
             * This is using secrets.getSecretBundle() to retrieve the content which requires the Secret Ocid as the
             * argument. This can be switched to getSecretBundleByName() if preferred but needs to pass in Secret Name and
             * Vault Ocid as arguments. Please check the Java SDK example in
             * https://docs.oracle.com/en-us/iaas/api/#/en/secretretrieval/20190301/SecretBundle/GetSecretBundleByName
             */
            SecretBundleContentDetails content = secrets.getSecretBundle(GetSecretBundleRequest.builder()
                            .secretId(ociSecretsId)
                            .build())
                    .getSecretBundle()
                    .getSecretBundleContent();
            if (content != null && content instanceof Base64SecretBundleContentDetails) {
                return Base64Value.createFromEncoded(((Base64SecretBundleContentDetails) content).getContent()).toDecodedString();
            } else {
                throw new RuntimeException("Unable to retrieve Secret content");
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to get Secret content due to: " + e.getMessage());
        }
    }

    /**
     * Helidon MP Config cannot be accessed from a Custom Config Source so this method will perform  custom
     * parsing of application.yaml to retrieve the OCI Secret OCID. If you intend to use another file or another
     * file format, you need to modify this to suit your need.
     */
    private String getOciSecretId() {
        Enumeration<URL> resources;
        try {
            resources = Thread.currentThread().getContextClassLoader().getResources("application.yaml");
        } catch (IOException e) {
            throw new RuntimeException("Failed to find application.yaml from classpath", e);
        }

        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try (InputStreamReader reader = new InputStreamReader(url.openConnection().getInputStream(), StandardCharsets.UTF_8)) {
                Map yamlMap = (Map) new Yaml(new SafeConstructor()).loadAs(reader, Object.class);
                if (yamlMap == null) {
                    throw new RuntimeException("Failed to read application.yaml");
                }
                Map<String, Map<String, String>> oci = (Map<String, Map<String, String>>) yamlMap.get("oci");
                if (oci == null) {
                    throw new RuntimeException("Unable to find 'oci' parameter from application.yaml");
                }
                Map<String, String> secret = oci.get("secret");
                if (secret == null) {
                    throw new RuntimeException("Unable to find 'secret' parameter from application.yaml");
                }
                return secret.get("id");
            } catch (Exception e) {
                throw new RuntimeException("Failed to read application.yaml", e);
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, String> getProperties() {
        return PROPERTIES;
    }

    @Override
    public String getValue(String key) {
        return PROPERTIES.get(key);
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }
}
