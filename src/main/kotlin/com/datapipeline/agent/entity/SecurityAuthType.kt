package com.datapipeline.agent.entity

enum class SecurityAuthType {
    SIMPLE,
    KERBEROS,
    SCRAM_SHA_1,
    SCRAM_SHA_256,
    SCRAM_SHA_512,
    PLAIN,
    LDAP,
    SSL,
    TLS,
    SSH
}