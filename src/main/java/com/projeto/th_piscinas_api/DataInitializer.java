package com.projeto.th_piscinas_api;

import com.projeto.th_piscinas_api.model.Product;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ProductRepository;
import com.projeto.th_piscinas_api.repository.UserRepository;
import com.projeto.th_piscinas_api.util.Perfil;
import com.projeto.th_piscinas_api.util.ProductCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {


    private final UserRepository usuarioRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_MATRICULA}")
    private String matricula;
    @Value("${ADMIN_SENHA}")
    private String senha;
    @Value("${ADMIN_NOME}")
    private String nome;

    @Override
    public void run(String... args) {
        if (usuarioRepository.countByPerfil(Perfil.ADM_MASTER) == 0) {
            usuarioRepository.save(User.builder()
                    .nome(nome).matricula(matricula)
                    .senha(passwordEncoder.encode(senha))
                    .perfil(Perfil.ADM_MASTER).ativo(true).build());
            log.warn("ADM Master criado (matricula={}). TROQUE A SENHA!", matricula);
        }
        seedDev("vint", "Vendedor Interno Dev", Perfil.VENDEDOR_INTERNO);
        seedDev("vext", "Vendedor Externo Dev", Perfil.VENDEDOR_EXTERNO);
        seedDev("tec",  "Tecnico Dev",          Perfil.TECNICO_CONDOMINIAL);
    }

    private void seedDev(String mat, String nomeUser, Perfil perfil) {
        if (usuarioRepository.findByMatricula(mat).isEmpty()) {
            usuarioRepository.save(User.builder()
                    .nome(nomeUser).matricula(mat)
                    .senha(passwordEncoder.encode("123"))
                    .perfil(perfil).ativo(true).build());
            log.info("Usuário dev criado: {}", mat);
        }
    }
}
