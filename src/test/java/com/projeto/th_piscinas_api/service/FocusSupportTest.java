package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.util.NfseStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FocusSupportTest {

    @Test
    void cpf_validaOsDigitosVerificadores() {
        assertThat(FocusSupport.isValidCpf("111.444.777-35")).isTrue();
        assertThat(FocusSupport.isValidCpf("11144477736")).isFalse();
        // the one SEFAZ refused in homologation ("CPF do destinatário inválido")
        assertThat(FocusSupport.isValidCpf("99999999999")).isFalse();
        assertThat(FocusSupport.isValidCpf("123")).isFalse();
        assertThat(FocusSupport.isValidCpf(null)).isFalse();
    }

    @Test
    void cnpj_validaOsDigitosVerificadores() {
        assertThat(FocusSupport.isValidCnpj("65.480.593/0001-86")).isTrue();
        assertThat(FocusSupport.isValidCnpj("59774374000107")).isTrue();
        assertThat(FocusSupport.isValidCnpj("65480593000187")).isFalse();
        assertThat(FocusSupport.isValidCnpj("00000000000000")).isFalse();
        assertThat(FocusSupport.isValidCnpj("123")).isFalse();
    }

    @Test
    void mapStatus_traduzOsStatusDaFocus() {
        assertThat(FocusSupport.mapStatus("autorizado")).isEqualTo(NfseStatus.AUTORIZADO);
        assertThat(FocusSupport.mapStatus("cancelado")).isEqualTo(NfseStatus.CANCELADO);
        assertThat(FocusSupport.mapStatus("erro_autorizacao")).isEqualTo(NfseStatus.ERRO_AUTORIZACAO);
        assertThat(FocusSupport.mapStatus("processando_autorizacao")).isEqualTo(NfseStatus.PROCESSANDO);
    }

    @Test
    void extractError_rejeicaoDaSefaz() {
        // exactly what Focus returned for the invalid CPF
        Map<String, Object> body = Map.of("status", "erro_autorizacao", "status_sefaz", "237",
                "mensagem_sefaz", "Rejeição: CPF do destinatário inválido");

        assertThat(FocusSupport.extractError(body)).isEqualTo("237: Rejeição: CPF do destinatário inválido");
    }

    @Test
    void extractError_listaDeErrosDaPrefeitura() {
        // exactly what Focus returned for the NFS-e without the Simples regime
        Map<String, Object> body = Map.of("status", "erro_autorizacao", "erros", List.of(
                Map.of("codigo", "E0166", "mensagem", "É obrigatorio o preenchimento do campo de regime de apuração")));

        assertThat(FocusSupport.extractError(body)).startsWith("E0166: ");
    }

    @Test
    void extractError_empresaNaoHabilitada() {
        Map<String, Object> body = Map.of("codigo", "empresa_nao_habilitada",
                "mensagem", "Empresa ainda não habilitada para emissão de NFe, por favor contate o suporte técnico.");

        assertThat(FocusSupport.extractError(body)).startsWith("empresa_nao_habilitada: Empresa ainda não habilitada");
    }

    @Test
    void extractError_notaAutorizada_semErro() {
        Map<String, Object> body = Map.of("status", "autorizado", "status_sefaz", "100",
                "mensagem_sefaz", "Autorizado o uso da NF-e");

        assertThat(FocusSupport.extractError(body)).isNull();
    }
}
