package com.projeto.th_piscinas_api.repository;

import com.projeto.th_piscinas_api.model.NfseDpsSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NfseDpsSequenceRepository extends JpaRepository<NfseDpsSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM NfseDpsSequence s WHERE s.cnpj = :cnpj AND s.serie = :serie AND s.ambiente = :ambiente")
    Optional<NfseDpsSequence> findByCnpjAndSerieAndAmbienteForUpdate(String cnpj, Integer serie, String ambiente);

    /** Lock-free read, for peeking (dry-run/preview) — doesn't consume numbering. */
    Optional<NfseDpsSequence> findByCnpjAndSerieAndAmbiente(String cnpj, Integer serie, String ambiente);
}
