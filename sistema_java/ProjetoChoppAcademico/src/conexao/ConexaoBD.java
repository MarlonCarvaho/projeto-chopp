package conexao;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoBD {
    public Connection getConexao() {
        try {
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/projeto_chopp", "postgres", "010600");
        } catch (SQLException e) {
            throw new RuntimeException("Erro na conexão: " + e.getMessage());
        }
    }
}