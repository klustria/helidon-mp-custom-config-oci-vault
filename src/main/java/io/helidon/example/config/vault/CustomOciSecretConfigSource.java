package io.helidon.example.config.vault;

import java.util.Map;

import io.helidon.config.yaml.mp.YamlMpConfigSource;
import io.helidon.common.Base64Value;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.secrets.Secrets;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.model.SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;
import com.oracle.bmc.secrets.requests.GetSecretBundleByNameRequest;

public class CustomOciSecretConfigSource implements ConfigSource {
    private static final String NAME = "CustomOciSecretConfigSource";
    private static final int ORDINAL = 200; // Default for MP is 100 so having a bigger value gives this higher priority
    private static Map<String, String> PROPERTIES;
    private static Config bootstrapConfig;
    private static final String configFileName = "application.yaml";

    public CustomOciSecretConfigSource() {
        bootstrapConfig = ConfigProviderResolver.instance()
                .getBuilder()
                .withSources(YamlMpConfigSource.classPath(configFileName).toArray(new ConfigSource[0]))
                .build();
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
                            .secretId(bootstrapConfig.getValue("oci.secret.id", String.class))
                            .build())
                    .getSecretBundle()
                    .getSecretBundleContent();
            /* Uncomment below if getSecretBundleByName() is preferred
            SecretBundleContentDetails content = secrets.getSecretBundleByName(GetSecretBundleByNameRequest.builder()
                            .secretName(bootstrapConfig.getValue("oci.secret.name", String.class))
                            .vaultId(bootstrapConfig.getValue("oci.vault.id", String.class))
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
}
