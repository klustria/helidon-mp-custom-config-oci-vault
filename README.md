# Helidon Datasource Configuration Example

This is a simple Helidon MP project that retrieves datasource configuration parameters from application.yaml, except
for the password which is fetched from the Oci Vault/Secret. The Oci Vault/Secret data fetching is performed using 
Custom Config Source in [CustomOciSecretConfigSource.java](src/main/java/io/helidon/example/config/vault/CustomOciSecretConfigSource.java).
This is registered as a service via [src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource](src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource).
The rest of the parameters will be retrieved from the regular Config Source which in this case will be data coming from  
[src/main/resources/application.yaml](src/main/resources/application.yaml).

The custom config source will use the Oci SDK for Vaults/Secrets to retrieve the target data which in this case is the
password. The parameter for the SDK invocation will be retrieved from the same 
[src/main/resources/application.yaml](src/main/resources/application.yaml). However, because custom config source cannot
access the Helidon MP config source, a customized parsing logic of the yaml file was used instead. At the discretion of 
the user, this can be replaced to use other file format parsing logic and/or can target another file or can be coded 
to use system environment variable. To disable custom config source, you can either delete 
[CustomOciSecretConfigSource.java](src/main/java/io/helidon/example/config/vault/CustomOciSecretConfigSource.java)
or [src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource](src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource)
and when this happens, the application will go back to retrieving the password from [application.yaml](src/main/resources/application.yaml).


## Prerequisites

1. OCI Vault Key and Secrets need to be created. There is a good example guide for this in  [Protect Your Sensitive Data With Secrets In The Oracle Cloud](https://recursive.codes/p/protect-your-sensitive-data-with-secrets-in-the-oracle-cloud). 
   Once created, get the Secret Ocid of the Datsource source password and use that as the value of oci.secret.id in application.yaml.
2. When authenticating using user principal, make sure to set up user credentials in ~/.oci/config.


## Additional Information/References

1. For more details about Helidon Custom Config Sources, please consult [Creating Custom Config Sources section in Helidon Microprofile Config Sources](https://helidon.io/docs/v2/#/mp/config/02_MP_config_sources).
2. Details about Oci Vault/Secrets SDK can be found in [Managing Secrets](https://docs.oracle.com/en-us/iaas/Content/KeyManagement/Tasks/managingsecrets.htm).


## System Requirements:
1. JDK 11+
2. mvn 3.8.3+
3. Helidon 2.5.0


## Build

```bash
mvn package
java -jar target/helidon-config-vault.jar
```

## Exercise the application

```
curl http://localhost:8080/datasource
{"javax.sql.DataSource.slDataSource.dataSourceClassName":"org.h2.jdbcx.JdbcDataSource","javax.sql.DataSource.slDataSource.dataSource.url":"Password123!","javax.sql.DataSource.slDataSource.dataSource.user":"sa"}
```