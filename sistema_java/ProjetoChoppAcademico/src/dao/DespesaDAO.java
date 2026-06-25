package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import conexao.ConexaoBD;
import modelo.Despesa;

public class DespesaDAO {
    public void cadastrar(Despesa d) {
        String sql = "INSERT INTO despesa (descricao, valor) VALUES (?, ?)";
        try (Connection conn = new ConexaoBD().getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, d.getDescricao());
            stmt.setDouble(2, d.getValor());
            stmt.execute();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao cadastrar despesa: " + e.getMessage());
        }
    }

    public List<Despesa> listar() {
        List<Despesa> lista = new ArrayList<>();
        String sql = "SELECT id_despesa, descricao, valor FROM despesa ORDER BY id_despesa DESC";
        try (Connection conn = new ConexaoBD().getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Despesa d = new Despesa();
                d.setId(rs.getInt("id_despesa"));
                d.setDescricao(rs.getString("descricao"));
                d.setValor(rs.getDouble("valor"));
                lista.add(d);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar despesas: " + e.getMessage());
        }
        return lista;
    }

    public void excluir(int id) {
        String sql = "DELETE FROM despesa WHERE id_despesa = ?";
        try (Connection conn = new ConexaoBD().getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.execute();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao excluir despesa: " + e.getMessage());
        }
    }
}