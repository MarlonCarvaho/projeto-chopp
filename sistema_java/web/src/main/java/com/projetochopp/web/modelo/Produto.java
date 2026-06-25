package com.projetochopp.web.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produto")
    private Integer id;

    private String nome;
    private String categoria;

    @Column(name = "quantidade_estoque")
    private Integer quantidade;

    private Double preco;
    private String variacao;

    // NOVO CAMPO DA IMAGEM: O "TEXT" garante que cabe arquivos grandes
    @Column(columnDefinition = "TEXT")
    private String imagemBase64;

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Double getPreco() { return preco; }
    public void setPreco(Double preco) { this.preco = preco; }

    public String getVariacao() { return variacao; }
    public void setVariacao(String variacao) { this.variacao = variacao; }

    public String getImagemBase64() { return imagemBase64; }
    public void setImagemBase64(String imagemBase64) { this.imagemBase64 = imagemBase64; }
}