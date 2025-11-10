disable_mlock = true

storage "file" {
  path = "/vault/file"
}

listener "tcp" {
  address       = "0.0.0.0:8200"
  # tls_cert_file = "/vault/tls/server.crt"
  # tls_key_file  = "/vault/tls/server.key"
  tls_disable   = 1
}

api_addr = "http://vault:8200"
ui = true
default_lease_ttl = "168h"
max_lease_ttl     = "720h"
