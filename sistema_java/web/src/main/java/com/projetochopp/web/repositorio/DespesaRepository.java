package com.projetochopp.web.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.projetochopp.web.modelo.Despesa;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Integer> {
}