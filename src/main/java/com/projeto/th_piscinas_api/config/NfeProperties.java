package com.projeto.th_piscinas_api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Company data and fiscal defaults for issuing NF-e (product invoice, model 55).
 *
 * <p>Token, environment ({@code homologacao}/{@code producao}), simulation switch, production
 * lock and the issuer's CNPJ are shared with the NFS-e and come from {@link NfseProperties}
 * ({@code nfse.*}) — one Focus account, one safety lock.</p>
 *
 * <p>The {@code *Padrao} values are used while a product has no fiscal data of its own.
 * They are only placeholders that let the flow be tested: the accountant must confirm the
 * real NCM/CFOP/CSOSN of each product before any real issuance.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nfe")
public class NfeProperties {

    /** Issuer's state registration (inscrição estadual), digits only. */
    private String ieEmitente = "";

    /** Issuer's state (UF): decides intra-state (CFOP 5xxx) vs inter-state (6xxx). */
    private String ufEmitente = "PE";

    /** 1 = Simples Nacional, 2 = Simples (excess of sub-limit), 3 = Normal regime (not supported yet). */
    private int regimeTributario = 1;

    private String naturezaOperacao = "Venda de mercadoria";

    /** Default NCM (8 digits) when the product has none. */
    private String ncmPadrao = "84137090";

    /** Default CFOP for intra-state sales (5102); inter-state uses {@link #cfopInterestadualPadrao}. */
    private String cfopPadrao = "5102";

    private String cfopInterestadualPadrao = "6102";

    /** Default origin of the goods (0 = national). */
    private int origemPadrao = 0;

    /** Default CSOSN (102 = taxed by Simples Nacional without credit). */
    private String csosnPadrao = "102";

    /** PIS/COFINS situation code used with Simples Nacional (49 = other output operations). */
    private String pisCofinsCstPadrao = "49";
}
