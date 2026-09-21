package com.projeto.th_piscinas_api.util;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Data de "hoje" no fuso do negócio (Recife, UTC-3, sem horário de verão).
 *
 * O servidor roda em UTC. Entre 21h e meia-noite em Recife o relógio dele já está
 * no dia seguinte, e {@code LocalDate.now()} devolvia a data errada: a NFS-e saía
 * com competência de amanhã e a prefeitura recusava (E0015). Toda data de "hoje" que
 * o usuário enxerga como dia civil deve vir daqui, não de {@code LocalDate.now()}.
 */
public final class Datas {

    public static final ZoneId FUSO = ZoneId.of("America/Recife");

    private Datas() {
    }

    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }
}
