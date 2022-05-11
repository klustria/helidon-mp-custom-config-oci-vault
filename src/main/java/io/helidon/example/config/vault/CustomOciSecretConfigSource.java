package io.helidon.example.config.vault;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.helidon.common.Base64Value;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.secrets.Secrets;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.model.SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;
import com.oracle.bmc.secrets.requests.GetSecretBundleByNameRequest;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;


public class CustomOciSecretConfigSource implements ConfigSource {
    private static final String NAME = "CustomOciSecretConfigSource";
    private static final int ORDINAL = 200; // Default for MP is 100 so having a bigger value gives this higher priority
    private static Map<String, String> PROPERTIES;
    private static Map yamlMap = getApplicationYamlMap(); // Contains Map content of application.yaml

    public CustomOciSecretConfigSource() {
        PROPERTIES =  Map.of(
                "javax.sql.DataSource.slDataSource.dataSource.password",
                getSecretContent()
        );
    }

    private String getSecretContent() {
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
             * Default is using secrets.getSecretBundle() to retrieve the content which requires the Secret Ocid as the
             * argument. This can be switched to getSecretBundleByName() if preferred but needs to pass in Secret Name and
             * Vault Ocid as arguments. A sample is provided below secrets.getSecretBundle(). Uncomment it out and remove
             * secrets.getSecretBundle() if that is the preferred way.
             */
            SecretBundleContentDetails content = secrets.getSecretBundle(GetSecretBundleRequest.builder()
                            .secretId(getOciSecretId())
                            .build())
                    .getSecretBundle()
                    .getSecretBundleContent();
            /* Uncomment below if getSecretBundleByName() is preferred
            SecretBundleContentDetails content = secrets.getSecretBundleByName(GetSecretBundleByNameRequest.builder()
                            .secretName(getOciSecretName())
                            .vaultId(getOciVaultId())
                            .build())
                    .getSecretBundle()
                    .getSecretBundleContent();
            */
            if (content != null && content instanceof Base64SecretBundleContentDetails) {
                return Base64Value.createFromEncoded(((Base64SecretBundleContentDetails) content).getContent()).toDecodedString();
            } else {
                throw new RuntimeException("Unable to retrieve Secret content");
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to get Secret content due to: " + e.getMessage());
        }
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

    /**
     * Helidon MP Config cannot be accessed from a Custom Config Source so these helper methods will perform  custom
     * parsing of application.yaml to retrieve the OCI Secret and Vault parameters. If you intend to use another file
     * or another file format, you need to modify this to suit your needs.
     */
    private String getOciSecretId() {
        Map<String, Map<String, String>> oci = getOciMap();
        Map<String, String> secret = getOciChildMap(oci, "secret");
        return secret.get("id");
    }

    private String getOciSecretName() {
        Map<String, Map<String, String>> oci = getOciMap();
        Map<String, String> secret = getOciChildMap(oci, "secret");
        return secret.get("name");
    }

    private String getOciVaultId() {
        Map<String, Map<String, String>> oci = getOciMap();
        Map<String, String> secret = getOciChildMap(oci, "vault");
        return secret.get("id");
    }

    private Map<String, Map<String, String>> getOciMap() {
        Map<String, Map<String, String>> oci = (Map<String, Map<String, String>>) this.yamlMap.get("oci");
        if (oci == null) {
            throw new RuntimeException("Unable to find 'oci' parameter from application.yaml");
        }
        return oci;
    }

    private Map<String, String> getOciChildMap(Map<String, Map<String, String>> oci, String key) {
        Map<String, String> secret = oci.get(key);
        if (secret == null) {
            throw new RuntimeException("Unable to find 'secret' parameter from application.yaml");
        }
        return secret;
    }

    private static Map getApplicationYamlMap() {
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
                return yamlMap;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read application.yaml", e);
            }
        }
        throw new RuntimeException("Failed to read application.yaml");
    }
}
