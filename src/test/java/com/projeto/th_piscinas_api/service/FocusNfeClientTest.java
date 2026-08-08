package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.config.NfseProperties;
import com.projeto.th_piscinas_api.model.Invoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks in the {@code /v2/nfsen} payload that already authorized a real invoice
 * in production (company MRM, Recife/PE, 07/27/2026) — see NFSE-NACIONAL-RECIFE.md.
 * Any change that breaks one of these fields reproduces an already-cataloged
 * error (E0713, E0312, E0116/E0120).
 */
class FocusNfeClientTest {

    private NfseProperties props;
    private FocusNfeClient client;

    @BeforeEach
    void setUp() {
        props = new NfseProperties();
        props.setCnpjPrestador("42194869000164");
        props.setInscricaoMunicipal("7387172");
        props.setCodigoMunicipio("2611606");
        props.setCodigoTributacaoNacional("171201");
        props.setCodigoTributacaoMunicipal("501");
        props.setCodigoOpcaoSimplesNacional(1); // not opted in
        props.setPercentualTributosFederais(new BigDecimal("0.00"));
        props.setPercentualTributosEstaduais(new BigDecimal("0.00"));
        props.setPercentualTributosMunicipais(new BigDecimal("5.00"));
        client = new FocusNfeClient(props);
    }

    private Invoice buildInvoice() {
        return Invoice.builder()
                .clientDocument("65480593000186")
                .clientName("Cliente Teste")
                .description("Prestação de serviço")
                .value(new BigDecimal("100.00"))
                .dataCompetencia(LocalDate.of(2026, 7, 27))
                .build();
    }

    @Test
    void buildPayload_naoOptante_enviaOsTresPercentuaisENaoOSN() {
        Map<String, Object> payload = client.buildPayload(buildInvoice(), 2, 5L);

        assertThat(payload).containsEntry("percentual_total_tributos_federais", new BigDecimal("0.00"));
        assertThat(payload).containsEntry("percentual_total_tributos_estaduais", new BigDecimal("0.00"));
        assertThat(payload).containsEntry("percentual_total_tributos_municipais", new BigDecimal("5.00"));
        // E0713: a non-opted-in taxpayer can't send Simples fields.
        assertThat(payload).doesNotContainKey("percentual_total_tributos_simples_nacional");
        assertThat(payload).doesNotContainKey("indicador_total_tributacao");
    }

    @Test
    void buildPayload_optanteSimples_enviaPTotTribSNENaoOsTresPercentuais() {
        props.setCodigoOpcaoSimplesNacional(3); // ME/EPP (small business)
        props.setPercentualTributosSimplesNacional(new BigDecimal("6.00"));

        Map<String, Object> payload = client.buildPayload(buildInvoice(), 2, 5L);

        assertThat(payload).containsEntry("percentual_total_tributos_simples_nacional", new BigDecimal("6.00"));
        assertThat(payload).doesNotContainKey("percentual_total_tributos_federais");
        assertThat(payload).doesNotContainKey("percentual_total_tributos_estaduais");
        assertThat(payload).doesNotContainKey("percentual_total_tributos_municipais");
    }

    @Test
    void buildPayload_codigosDeTributacaoVaoComoNumero_naoString() {
        Map<String, Object> payload = client.buildPayload(buildInvoice(), 2, 5L);

        // E0312 happened with the right code but missing the municipal one — here
        // we lock in both being present and as an Integer (not a String).
        assertThat(payload.get("codigo_tributacao_nacional_iss")).isInstanceOf(Integer.class).isEqualTo(171201);
        assertThat(payload.get("codigo_tributacao_municipal_iss")).isInstanceOf(Integer.class).isEqualTo(501);
    }

    @Test
    void buildPayload_dataEmissaoUsaOffsetRecife() {
        Map<String, Object> payload = client.buildPayload(buildInvoice(), 2, 5L);

        assertThat((String) payload.get("data_emissao")).endsWith("-03:00");
        assertThat(payload.get("data_competencia")).isEqualTo("2026-07-27");
    }

    @Test
    void buildPayload_tomadorComCnpjLongoVaiComoCnpjTomador() {
        Invoice invoice = buildInvoice(); // document with 14 digits
        Map<String, Object> payload = client.buildPayload(invoice, 2, 5L);

        assertThat(payload).containsEntry("cnpj_tomador", "65480593000186");
        assertThat(payload).doesNotContainKey("cpf_tomador");
    }

    @Test
    void buildPayload_tomadorComCpfVaiComoCpfTomador() {
        Invoice invoice = buildInvoice();
        invoice.setClientDocument("12345678909"); // 11 digits
        Map<String, Object> payload = client.buildPayload(invoice, 2, 5L);

        assertThat(payload).containsEntry("cpf_tomador", "12345678909");
        assertThat(payload).doesNotContainKey("cnpj_tomador");
    }

    @Test
    void buildPayload_inscricaoMunicipalAusenteQuandoNaoConfigurada() {
        props.setInscricaoMunicipal("");
        Map<String, Object> payload = client.buildPayload(buildInvoice(), 2, 5L);

        // E0120: some registrations forbid the field — omit it instead of sending it empty.
        assertThat(payload).doesNotContainKey("inscricao_municipal_prestador");
    }

    @Test
    void buildPayload_inscricaoMunicipalPresenteQuandoConfigurada() {
        Map<String, Object> payload = client.buildPayload(buildInvoice(), 2, 5L);

        // E0116: other registrations require the field.
        assertThat(payload).containsEntry("inscricao_municipal_prestador", "7387172");
    }

    @Test
    void buildPayload_serieENumeroDpsInformadosPeloEmissor() {
        Map<String, Object> payload = client.buildPayload(buildInvoice(), 2, 42L);

        assertThat(payload).containsEntry("serie_dps", 2);
        assertThat(payload).containsEntry("numero_dps", 42L);
    }
}
