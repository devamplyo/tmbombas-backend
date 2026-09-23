package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.model.NfseDpsSequence;
import com.projeto.th_piscinas_api.repository.NfseDpsSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Allocates sequential numero_dps per (cnpj, serie, ambiente). Focus doesn't generate
 * this number — the issuer provides it, and a repeated number is rejected by
 * the city. Gaps in the numbering (from rejected invoices) are normal.
 */
@Service
@RequiredArgsConstructor
public class DpsNumberAllocator {

    public static final String HOMOLOGACAO = "HOMOLOGACAO";
    public static final String PRODUCAO = "PRODUCAO";

    // homologação starts high because low numbers were already used in earlier tests;
    // real production starts at 100
    private static final long PRIMEIRO_NUMERO_HOMOLOGACAO = 1000;
    private static final long PRIMEIRO_NUMERO_PRODUCAO = 100;

    private final NfseDpsSequenceRepository repository;

    private static long primeiroNumero(String ambiente) {
        return PRODUCAO.equals(ambiente) ? PRIMEIRO_NUMERO_PRODUCAO : PRIMEIRO_NUMERO_HOMOLOGACAO;
    }

    /**
     * {@code REQUIRES_NEW} is essential: the number is confirmed and persisted
     * even if the issuance that consumes it fails afterwards — never reused.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long next(String cnpj, int serie, String ambiente) {
        Optional<NfseDpsSequence> existente = repository.findByCnpjAndSerieAndAmbienteForUpdate(cnpj, serie, ambiente);
        NfseDpsSequence seq;
        if (existente.isPresent()) {
            seq = existente.get();
        } else {
            // First issuance for this (cnpj, serie): the pessimistic lock
            // doesn't protect an INSERT that doesn't exist yet, so two
            // concurrent transactions may try to create the row at the same
            // time. The database's unique constraint decides; the loser
            // retries and finds the row the other one already created.
            try {
                seq = repository.saveAndFlush(NfseDpsSequence.builder()
                        .cnpj(cnpj)
                        .serie(serie)
                        .ambiente(ambiente)
                        .ultimoNumero(primeiroNumero(ambiente) - 1)
                        .build());
            } catch (DataIntegrityViolationException e) {
                seq = repository.findByCnpjAndSerieAndAmbienteForUpdate(cnpj, serie, ambiente)
                        .orElseThrow(() -> e);
            }
        }

        long proximo = seq.getUltimoNumero() + 1;
        seq.setUltimoNumero(proximo);
        repository.save(seq);
        return proximo;
    }

    /** Peeks at the next number without consuming it — used by preview/dry-run. */
    @Transactional(readOnly = true)
    public long peekNext(String cnpj, int serie, String ambiente) {
        return repository.findByCnpjAndSerieAndAmbiente(cnpj, serie, ambiente)
                .map(s -> s.getUltimoNumero() + 1)
                .orElse(primeiroNumero(ambiente));
    }
}
