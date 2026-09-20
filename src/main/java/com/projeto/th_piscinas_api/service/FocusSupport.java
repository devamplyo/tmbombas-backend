package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.util.NfseStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helpers shared by the fiscal documents issued through Focus NFe: status mapping, readable
 * error extraction and CPF/CNPJ check-digit validation (so an obviously invalid document is
 * refused here, with a clear message, instead of a SEFAZ rejection).
 */
final class FocusSupport {

    private FocusSupport() {
    }

    static String digits(String v) {
        return v == null ? "" : v.replaceAll("\\D", "");
    }

    static NfseStatus mapStatus(String focus) {
        return switch (focus) {
            case "autorizado" -> NfseStatus.AUTORIZADO;
            case "cancelado" -> NfseStatus.CANCELADO;
            case "erro_autorizacao" -> NfseStatus.ERRO_AUTORIZACAO;
            case "erro" -> NfseStatus.ERRO;
            default -> NfseStatus.PROCESSANDO; // processando_autorizacao, ...
        };
    }

    /**
     * Extracts a readable message from Focus's error formats: SEFAZ rejection at the root
     * ({@code status_sefaz} + {@code mensagem_sefaz}), a list {@code erros:[{codigo,mensagem}]},
     * or {@code codigo}/{@code mensagem} at the root (e.g. company not enabled).
     */
    @SuppressWarnings("unchecked")
    static String extractError(Map<String, Object> body) {
        if (body == null) return null;
        StringBuilder out = new StringBuilder();

        Object mensagemSefaz = body.get("mensagem_sefaz");
        String status = body.get("status") == null ? "" : body.get("status").toString();
        if (mensagemSefaz != null && (status.startsWith("erro") || body.get("erros") != null)) {
            Object statusSefaz = body.get("status_sefaz");
            out.append(statusSefaz != null ? statusSefaz + ": " : "").append(mensagemSefaz);
        }

        Object errosObj = body.get("erros");
        if (errosObj instanceof List<?> erros && !erros.isEmpty()) {
            String lista = erros.stream()
                    .filter(Map.class::isInstance)
                    .map(e -> formatCodigoMensagem((Map<String, Object>) e))
                    .filter(t -> !t.isBlank())
                    .collect(Collectors.joining(" | "));
            if (!lista.isBlank()) {
                if (out.length() > 0) out.append(" | ");
                out.append(lista);
            }
        }

        if (out.length() == 0 && (body.get("codigo") != null || body.get("mensagem") != null)) {
            out.append(formatCodigoMensagem(body));
        }
        return out.length() == 0 ? null : out.toString();
    }

    private static String formatCodigoMensagem(Map<String, Object> m) {
        Object codigo = m.get("codigo");
        Object mensagem = m.get("mensagem");
        String c = codigo == null ? "" : codigo.toString();
        return (c.isBlank() ? "" : c + ": ") + (mensagem != null ? mensagem : "");
    }

    // ---------- CPF / CNPJ ----------

    static boolean isValidCpf(String raw) {
        String cpf = digits(raw);
        if (cpf.length() != 11 || allSame(cpf)) return false;
        return cpf.charAt(9) - '0' == cpfDigit(cpf, 9) && cpf.charAt(10) - '0' == cpfDigit(cpf, 10);
    }

    private static int cpfDigit(String cpf, int len) {
        int sum = 0;
        for (int i = 0; i < len; i++) {
            sum += (cpf.charAt(i) - '0') * (len + 1 - i);
        }
        int r = (sum * 10) % 11;
        return r == 10 ? 0 : r;
    }

    static boolean isValidCnpj(String raw) {
        String cnpj = digits(raw);
        if (cnpj.length() != 14 || allSame(cnpj)) return false;
        return cnpj.charAt(12) - '0' == cnpjDigit(cnpj, 12) && cnpj.charAt(13) - '0' == cnpjDigit(cnpj, 13);
    }

    private static int cnpjDigit(String cnpj, int len) {
        int weight = len - 7;
        int sum = 0;
        for (int i = 0; i < len; i++) {
            sum += (cnpj.charAt(i) - '0') * weight--;
            if (weight < 2) weight = 9;
        }
        int r = sum % 11;
        return r < 2 ? 0 : 11 - r;
    }

    private static boolean allSame(String s) {
        return s.chars().distinct().count() == 1;
    }
}
