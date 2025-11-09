https://www.baeldung.com/vault
https://www.baeldung.com/spring-cloud-vault

vault secrets enable database

https://developer.hashicorp.com/vault/docs/secrets/databases/postgresql
vault write database/config/authdb \
    plugin_name="postgresql-database-plugin" \
    allowed_roles="java-lab-rw" \
    connection_url="postgresql://{{username}}:{{password}}@postgres:5432/authdb" \
    username="authuser" \
    password="authpass" \
    password_authentication="scram-sha-256"

vault write database/roles/java-lab-rw \
    db_name="authdb" \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
        GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

vault read database/creds/java-lab-rw