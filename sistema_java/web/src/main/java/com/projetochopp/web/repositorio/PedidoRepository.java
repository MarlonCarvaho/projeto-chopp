package com.projetochopp.web.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.projetochopp.web.modelo.Pedido;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    
    // NOVO: Busca os pedidos de um cliente específico pelo nome dele
    // O "OrderByIdDesc" faz com que os pedidos mais recentes apareçam primeiro na tela
    List<Pedido> findByClienteOrderByIdDesc(String cliente);
    
}