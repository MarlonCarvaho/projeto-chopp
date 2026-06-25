package com.projetochopp.web.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.projetochopp.web.modelo.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    
    // Utilizado para autenticação no Login
    Usuario findByEmailAndSenha(String email, String senha);
    
    // Utilizado para evitar e-mails duplicados no Cadastro
    Usuario findByEmail(String email);
}