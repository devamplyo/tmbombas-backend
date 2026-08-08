package com.projeto.th_piscinas_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Configuration for issuing Nationwide NFS-e (the {@code nfse:} block in application.yaml).
 *
 * <p>{@code simulate=true} (default in dev) issues a "NO FISCAL VALUE" invoice
 * locally, without calling Focus NFe — useful while there's no token yet.</p>
 *
 * <p>Fields and enum codes documented in NFSE-NACIONAL-RECIFE.md (root of the
 * projects repository) — payload validated with an invoice authorized in production.</p>
 */
@Component
@ConfigurationProperties(prefix = "nfse")
public class NfseProperties {

    /** Fakes the issuance locally, without calling Focus. */
    private boolean simulate = true;

    /** Explicit safety lock: real issuance in production only happens if true. */
    private boolean producaoConfirmada = false;

    private String token = "";
    private String ambiente = "homologacao";
    private String cnpjPrestador = "";
    private String inscricaoMunicipal = "";
    private String codigoMunicipio = "";
    private String razaoSocial = "TH Bombas";
    private String emailPrestador = "";

    /** codigo_tributacao_nacional_iss (Integer[6], XML tag cTribNac). E.g.: 171201. */
    private String codigoTributacaoNacional = "";

    /** codigo_tributacao_municipal_iss (Integer[3], XML tag cTribMun). E.g.: 501. */
    private String codigoTributacaoMunicipal = "";

    private BigDecimal aliquota = new BigDecimal("5.00");

    /** 1=Not opted in | 2=MEI opt-in | 3=ME/EPP opt-in (XML tag opSimpNac). */
    private int codigoOpcaoSimplesNacional = 1;

    /** 0=None | 1=Cooperative act | 2=Estimate | ... (see Focus field docs). */
    private int regimeEspecialTributacao = 0;

    /** 1=Taxable operation | 2=Immunity | 3=Export | 4=Non-taxable. */
    private int tributacaoIss = 1;

    private int tipoRetencaoIss = 1;

    /**
     * Approximate tax percentages (totTrib) — mandatory for NON opt-in
     * taxpayers. The tax block can't be left empty (schema error), but a
     * non-opt-in taxpayer can't send indicador_total_tributacao nor
     * percentual_total_tributos_simples_nacional (error E0713).
     */
    private BigDecimal percentualTributosFederais = new BigDecimal("0.00");
    private BigDecimal percentualTributosEstaduais = new BigDecimal("0.00");
    private BigDecimal percentualTributosMunicipais = new BigDecimal("0.00");

    /**
     * percentual_total_tributos_simples_nacional (pTotTribSN) — only for a
     * Simples Nacional opt-in taxpayer (codigoOpcaoSimplesNacional != 1). The
     * effective Simples tax rate, comes from the PGDAS statement. Null while
     * not configured.
     */
    private BigDecimal percentualTributosSimplesNacional;

    /**
     * DPS series used by the backend. Recommended to use its own series
     * (e.g.: 2) to avoid colliding with numbering already used manually
     * outside the system (e.g.: Postman tests on series 1).
     */
    private int serieDps = 1;

    /** True if issuance is possible: in simulation, or with minimal real credentials. */
    public boolean isConfigured() {
        return simulate || (!token.isBlank() && !cnpjPrestador.isBlank()
                && !codigoMunicipio.isBlank() && !codigoTributacaoNacional.isBlank());
    }

    /** True if opted into Simples Nacional (any variant other than "not opted in"). */
    public boolean isOptanteSimplesNacional() {
        return codigoOpcaoSimplesNacional != 1;
    }

    public boolean isSimulate() { return simulate; }
    public void setSimulate(boolean simulate) { this.simulate = simulate; }

    public boolean isProducaoConfirmada() { return producaoConfirmada; }
    public void setProducaoConfirmada(boolean producaoConfirmada) { this.producaoConfirmada = producaoConfirmada; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getAmbiente() { return ambiente; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }

    public String getCnpjPrestador() { return cnpjPrestador; }
    public void setCnpjPrestador(String cnpjPrestador) { this.cnpjPrestador = cnpjPrestador; }

    public String getInscricaoMunicipal() { return inscricaoMunicipal; }
    public void setInscricaoMunicipal(String inscricaoMunicipal) { this.inscricaoMunicipal = inscricaoMunicipal; }

    public String getCodigoMunicipio() { return codigoMunicipio; }
    public void setCodigoMunicipio(String codigoMunicipio) { this.codigoMunicipio = codigoMunicipio; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getEmailPrestador() { return emailPrestador; }
    public void setEmailPrestador(String emailPrestador) { this.emailPrestador = emailPrestador; }

    public String getCodigoTributacaoNacional() { return codigoTributacaoNacional; }
    public void setCodigoTributacaoNacional(String codigoTributacaoNacional) { this.codigoTributacaoNacional = codigoTributacaoNacional; }

    public String getCodigoTributacaoMunicipal() { return codigoTributacaoMunicipal; }
    public void setCodigoTributacaoMunicipal(String codigoTributacaoMunicipal) { this.codigoTributacaoMunicipal = codigoTributacaoMunicipal; }

    public BigDecimal getAliquota() { return aliquota; }
    public void setAliquota(BigDecimal aliquota) { this.aliquota = aliquota; }

    public int getCodigoOpcaoSimplesNacional() { return codigoOpcaoSimplesNacional; }
    public void setCodigoOpcaoSimplesNacional(int codigoOpcaoSimplesNacional) { this.codigoOpcaoSimplesNacional = codigoOpcaoSimplesNacional; }

    public int getRegimeEspecialTributacao() { return regimeEspecialTributacao; }
    public void setRegimeEspecialTributacao(int regimeEspecialTributacao) { this.regimeEspecialTributacao = regimeEspecialTributacao; }

    public int getTributacaoIss() { return tributacaoIss; }
    public void setTributacaoIss(int tributacaoIss) { this.tributacaoIss = tributacaoIss; }

    public int getTipoRetencaoIss() { return tipoRetencaoIss; }
    public void setTipoRetencaoIss(int tipoRetencaoIss) { this.tipoRetencaoIss = tipoRetencaoIss; }

    public BigDecimal getPercentualTributosFederais() { return percentualTributosFederais; }
    public void setPercentualTributosFederais(BigDecimal percentualTributosFederais) { this.percentualTributosFederais = percentualTributosFederais; }

    public BigDecimal getPercentualTributosEstaduais() { return percentualTributosEstaduais; }
    public void setPercentualTributosEstaduais(BigDecimal percentualTributosEstaduais) { this.percentualTributosEstaduais = percentualTributosEstaduais; }

    public BigDecimal getPercentualTributosMunicipais() { return percentualTributosMunicipais; }
    public void setPercentualTributosMunicipais(BigDecimal percentualTributosMunicipais) { this.percentualTributosMunicipais = percentualTributosMunicipais; }

    public BigDecimal getPercentualTributosSimplesNacional() { return percentualTributosSimplesNacional; }
    public void setPercentualTributosSimplesNacional(BigDecimal percentualTributosSimplesNacional) { this.percentualTributosSimplesNacional = percentualTributosSimplesNacional; }

    public int getSerieDps() { return serieDps; }
    public void setSerieDps(int serieDps) { this.serieDps = serieDps; }
}
