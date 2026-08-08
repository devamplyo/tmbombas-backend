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
 * Allocates sequential numero_dps per (cnpj, serie). Focus doesn't generate
 * this number — the issuer provides it, and a repeated number is rejected by
 * the city. Gaps in the numbering (from rejected invoices) are normal.
 */
@Service
@RequiredArgsConstructor
public class DpsNumberAllocator {

    private final NfseDpsSequenceRepository repository;

    /**
     * {@code REQUIRES_NEW} is essential: the number is confirmed and persisted
     * even if the issuance that consumes it fails afterwards — never reused.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long next(String cnpj, int serie) {
        Optional<NfseDpsSequence> existente = repository.findByCnpjAndSerieForUpdate(cnpj, serie);
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
                        .ultimoNumero(0L)
                        .build());
            } catch (DataIntegrityViolationException e) {
                seq = repository.findByCnpjAndSerieForUpdate(cnpj, serie)
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
    public long peekNext(String cnpj, int serie) {
        return repository.findByCnpjAndSerie(cnpj, serie)
                .map(s -> s.getUltimoNumero() + 1)
                .orElse(1L);
    }
}
