package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import conexao.ConexaoBD;
import modelo.Produto;

public class ProdutoDAO {
    public void cadastrar(Produto p) {
        String sql = "INSERT INTO produto (nome, categoria, quantidade_estoque, preco) VALUES (?, ?, ?, ?)";
        try (Connection conn = new ConexaoBD().getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getNome());
            stmt.setString(2, p.getCategoria());
            stmt.setInt(3, p.getQuantidade());
            stmt.setDouble(4, p.getPreco());
            stmt.execute();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao cadastrar: " + e.getMessage());
        }
    }

    public List<Produto> listar() {
        List<Produto> lista = new ArrayList<>();
        // Já com o nome correto id_produto para evitar aquele erro antigo
        String sql = "SELECT id_produto, nome, categoria, quantidade_estoque, preco FROM produto ORDER BY id_produto DESC";
        try (Connection conn = new ConexaoBD().getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Produto p = new Produto();
                p.setId(rs.getInt("id_produto"));
                p.setNome(rs.getString("nome"));
                p.setCategoria(rs.getString("categoria"));
                p.setQuantidade(rs.getInt("quantidade_estoque"));
                p.setPreco(rs.getDouble("preco"));
                lista.add(p);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar: " + e.getMessage());
        }
        return lista;
    }

    public void alterar(Produto p) {
        String sql = "UPDATE produto SET nome = ?, categoria = ?, quantidade_estoque = ?, preco = ? WHERE id_produto = ?";
        try (Connection conn = new ConexaoBD().getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getNome());
            stmt.setString(2, p.getCategoria());
            stmt.setInt(3, p.getQuantidade());
            stmt.setDouble(4, p.getPreco());
            stmt.setInt(5, p.getId());
            stmt.execute();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao alterar: " + e.getMessage());
        }
    }

    public void excluir(int id) {
        String sql = "DELETE FROM produto WHERE id_produto = ?";
        try (Connection conn = new ConexaoBD().getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.execute();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao excluir: " + e.getMessage());
        }
    }
}