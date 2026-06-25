package com.projetochopp.web.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "despesa")
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_despesa")
    private Integer id;

    private String descricao;

    private Double valor;

    @Column(name = "data_registro")
    private LocalDate data;

    // Essa mágica preenche a data do dia automaticamente!
    @PrePersist
    protected void onCreate() {
        this.data = LocalDate.now();
    }

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
}