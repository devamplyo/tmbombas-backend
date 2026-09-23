package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.model.NfseDpsSequence;
import com.projeto.th_piscinas_api.repository.NfseDpsSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.projeto.th_piscinas_api.service.DpsNumberAllocator.HOMOLOGACAO;
import static com.projeto.th_piscinas_api.service.DpsNumberAllocator.PRODUCAO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DpsNumberAllocatorTest {

    @Mock
    private NfseDpsSequenceRepository repository;

    @InjectMocks
    private DpsNumberAllocator allocator;

    @Test
    void next_primeiraChamadaHomologacao_criaSequenciaEComecaEm1000() {
        when(repository.findByCnpjAndSerieAndAmbienteForUpdate("59774374000107", 2, HOMOLOGACAO))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(NfseDpsSequence.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(NfseDpsSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        long numero = allocator.next("59774374000107", 2, HOMOLOGACAO);

        assertThat(numero).isEqualTo(1000L);
    }

    @Test
    void next_primeiraChamadaProducao_comecaEm100() {
        when(repository.findByCnpjAndSerieAndAmbienteForUpdate("59774374000107", 2, PRODUCAO))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(NfseDpsSequence.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(NfseDpsSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        long numero = allocator.next("59774374000107", 2, PRODUCAO);

        assertThat(numero).isEqualTo(100L);
    }

    @Test
    void next_chamadasConsecutivas_nuncaRepetemNumero() {
        NfseDpsSequence seq = NfseDpsSequence.builder()
                .id(1L).cnpj("59774374000107").serie(2).ambiente(HOMOLOGACAO).ultimoNumero(5L).build();
        when(repository.findByCnpjAndSerieAndAmbienteForUpdate("59774374000107", 2, HOMOLOGACAO))
                .thenReturn(Optional.of(seq));
        when(repository.save(any(NfseDpsSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        long primeiro = allocator.next("59774374000107", 2, HOMOLOGACAO);
        long segundo = allocator.next("59774374000107", 2, HOMOLOGACAO);
        long terceiro = allocator.next("59774374000107", 2, HOMOLOGACAO);

        assertThat(primeiro).isEqualTo(6L);
        assertThat(segundo).isEqualTo(7L);
        assertThat(terceiro).isEqualTo(8L);
    }

    @Test
    void peekNext_semSequenciaExistente_retornaOPrimeiroDoAmbienteSemCriarLinha() {
        when(repository.findByCnpjAndSerieAndAmbiente("59774374000107", 2, HOMOLOGACAO)).thenReturn(Optional.empty());
        when(repository.findByCnpjAndSerieAndAmbiente("59774374000107", 2, PRODUCAO)).thenReturn(Optional.empty());

        assertThat(allocator.peekNext("59774374000107", 2, HOMOLOGACAO)).isEqualTo(1000L);
        assertThat(allocator.peekNext("59774374000107", 2, PRODUCAO)).isEqualTo(100L);
        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void peekNext_naoConsomeNumero() {
        NfseDpsSequence seq = NfseDpsSequence.builder()
                .id(1L).cnpj("59774374000107").serie(2).ambiente(HOMOLOGACAO).ultimoNumero(5L).build();
        when(repository.findByCnpjAndSerieAndAmbiente("59774374000107", 2, HOMOLOGACAO)).thenReturn(Optional.of(seq));

        long primeiro = allocator.peekNext("59774374000107", 2, HOMOLOGACAO);
        long segundo = allocator.peekNext("59774374000107", 2, HOMOLOGACAO);

        assertThat(primeiro).isEqualTo(6L);
        assertThat(segundo).isEqualTo(6L); // didn't advance - peek doesn't consume
    }
}
