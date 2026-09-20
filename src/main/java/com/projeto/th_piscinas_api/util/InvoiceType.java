package com.projeto.th_piscinas_api.util;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Kind of fiscal document stored in {@code invoices}. Serialized in lowercase
 * ("nfe", "nfse") to match the front.
 */
public enum InvoiceType {
    /** Service invoice (Nationwide NFS-e), issued from a completed service order. */
    NFSE,
    /** Product invoice (NF-e, model 55), issued from a sale. */
    NFE;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
