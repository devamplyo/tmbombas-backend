package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.model.NfseDpsSequence;
import com.projeto.th_piscinas_api.repository.NfseDpsSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
    void next_primeiraChamada_criaSequenciaEComecaEm1() {
        when(repository.findByCnpjAndSerieForUpdate("42194869000164", 2)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(NfseDpsSequence.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(NfseDpsSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        long numero = allocator.next("42194869000164", 2);

        assertThat(numero).isEqualTo(1L);
    }

    @Test
    void next_chamadasConsecutivas_nuncaRepetemNumero() {
        NfseDpsSequence seq = NfseDpsSequence.builder()
                .id(1L).cnpj("42194869000164").serie(2).ultimoNumero(5L).build();
        when(repository.findByCnpjAndSerieForUpdate("42194869000164", 2)).thenReturn(Optional.of(seq));
        when(repository.save(any(NfseDpsSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        long primeiro = allocator.next("42194869000164", 2);
        long segundo = allocator.next("42194869000164", 2);
        long terceiro = allocator.next("42194869000164", 2);

        assertThat(primeiro).isEqualTo(6L);
        assertThat(segundo).isEqualTo(7L);
        assertThat(terceiro).isEqualTo(8L);
    }

    @Test
    void peekNext_semSequenciaExistente_retorna1SemCriarLinha() {
        when(repository.findByCnpjAndSerie("42194869000164", 2)).thenReturn(Optional.empty());

        long proximo = allocator.peekNext("42194869000164", 2);

        assertThat(proximo).isEqualTo(1L);
        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void peekNext_naoConsomeNumero() {
        NfseDpsSequence seq = NfseDpsSequence.builder()
                .id(1L).cnpj("42194869000164").serie(2).ultimoNumero(5L).build();
        when(repository.findByCnpjAndSerie("42194869000164", 2)).thenReturn(Optional.of(seq));

        long primeiro = allocator.peekNext("42194869000164", 2);
        long segundo = allocator.peekNext("42194869000164", 2);

        assertThat(primeiro).isEqualTo(6L);
        assertThat(segundo).isEqualTo(6L); // didn't advance - peek doesn't consume
    }
}
