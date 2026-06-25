package com.projetochopp.web.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.projetochopp.web.modelo.Produto;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Integer> {
    
    // Utilizado pela baixa automática para localizar o item comprado pelo nome
    Produto findByNome(String nome);
}