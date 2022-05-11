
package io.helidon.example.config.vault;

import java.util.Collections;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * A simple JAX-RS resource that will retrieve Datasource configuration parameter values from application.yaml.
 * Example:
 *
 * Get datasource configuration paramter values:
 * curl -X GET http://localhost:8080/datasource
 *
 * The paramter values are returned as a JSON object.
 */
@Path("/datasource")
public class DatasourceResource {
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    private final String datasourceClassName;
    private final String datasourceUrl;
    private final String datasourceUser;
    private final String datasourcePassword;

    private final String DATASOURCE_CLASS_NAME = "javax.sql.DataSource.slDataSource.dataSourceClassName";
    private final String DATASOURCE_URL = "javax.sql.DataSource.slDataSource.dataSource.url";
    private final String DATASOURCE_USER = "javax.sql.DataSource.slDataSource.dataSource.user";
    private final String DATASOURCE_PASSWORD = "javax.sql.DataSource.slDataSource.dataSource.password";

    @Inject
    public DatasourceResource(
            @ConfigProperty(name = DATASOURCE_CLASS_NAME) String datasourceClassName,
            @ConfigProperty(name = DATASOURCE_URL) String datasourceUrl,
            @ConfigProperty(name = DATASOURCE_USER) String datasourceUser,
            @ConfigProperty(name = DATASOURCE_PASSWORD) String datasourcePassword
                         ) {
        this.datasourceClassName = datasourceClassName;
        this.datasourceUrl = datasourceUrl;
        this.datasourceUser = datasourceUser;
        this.datasourcePassword = datasourcePassword;
    }

    /**
     * Return the datasource configuration values.
     *
     * @return {@link JsonObject}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getDatasourceConfig() {
        return JSON.createObjectBuilder()
                .add(DATASOURCE_CLASS_NAME, datasourceClassName)
                .add(DATASOURCE_URL, datasourceUrl)
                .add(DATASOURCE_USER, datasourceUser)
                .add(DATASOURCE_PASSWORD, datasourcePassword)
                .build();
    }
}
