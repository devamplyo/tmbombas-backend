package com.projeto.th_piscinas_api.util;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * NFS-e status. The value serialized to JSON is lowercase
 * (processando, autorizado, ...) to match the front's STATUS map.
 */
public enum NfseStatus {
    PROCESSANDO,
    AUTORIZADO,
    CANCELADO,
    ERRO,
    ERRO_AUTORIZACAO;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
