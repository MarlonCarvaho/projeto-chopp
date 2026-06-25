package visao;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import dao.UsuarioDAO;

public class TelaLoginCodigo extends JFrame {

    private JTextField txtEmail;
    private JPasswordField txtSenha;
    private JButton btnEntrar;

    private final Color BG_DARK = new Color(0x12, 0x12, 0x12);
    private final Color BG_PANEL = new Color(0x1E, 0x1E, 0x1E);
    private final Color PRIMARY = new Color(0xFF, 0x8C, 0x00); 
    private final Color TEXT = new Color(0xF3, 0xF4, 0xF6);
    private final Color TEXT_MUTED = new Color(0x9C, 0xA3, 0xAF);

    public TelaLoginCodigo() {
        setTitle("Login - Projeto Chopp");
        setSize(360, 280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK); 
        setLayout(null);

        Font fonteLabels = new Font("Segoe UI", Font.BOLD, 12);
        Font fonteCampos = new Font("Segoe UI", Font.PLAIN, 13);

        JLabel lblEmail = new JLabel("E-mail:");
        lblEmail.setFont(fonteLabels); lblEmail.setForeground(TEXT_MUTED);
        lblEmail.setBounds(40, 30, 100, 20); add(lblEmail);

        txtEmail = new JTextField();
        txtEmail.setFont(fonteCampos); txtEmail.setBackground(BG_PANEL); txtEmail.setForeground(TEXT); txtEmail.setCaretColor(TEXT); 
        txtEmail.setBorder(new LineBorder(new Color(0x44, 0x44, 0x44), 1, true));
        txtEmail.setBounds(40, 55, 260, 30); add(txtEmail);

        JLabel lblSenha = new JLabel("Senha:");
        lblSenha.setFont(fonteLabels); lblSenha.setForeground(TEXT_MUTED);
        lblSenha.setBounds(40, 95, 100, 20); add(lblSenha);

        txtSenha = new JPasswordField();
        txtSenha.setFont(fonteCampos); txtSenha.setBackground(BG_PANEL); txtSenha.setForeground(TEXT); txtSenha.setCaretColor(TEXT);
        txtSenha.setBorder(new LineBorder(new Color(0x44, 0x44, 0x44), 1, true));
        txtSenha.setBounds(40, 120, 260, 30); add(txtSenha);

        btnEntrar = new JButton("Entrar");
        btnEntrar.setFont(new Font("Segoe UI", Font.BOLD, 14)); btnEntrar.setBackground(PRIMARY); btnEntrar.setForeground(BG_DARK); btnEntrar.setFocusPainted(false); btnEntrar.setBorder(null); btnEntrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEntrar.setBounds(40, 180, 260, 35); add(btnEntrar);

        btnEntrar.addActionListener((ActionEvent e) -> {
            String email = txtEmail.getText().trim();
            String senha = new String(txtSenha.getPassword());

            try {
                if (new UsuarioDAO().autenticar(email, senha)) {
                    new TelaPrincipalCodigo().setVisible(true);
                    dispose(); 
                } else {
                    JOptionPane.showMessageDialog(null, "E-mail ou senha incorretos.");
                }
            } catch (Exception erro) {
                JOptionPane.showMessageDialog(null, "Erro: " + erro.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        new TelaLoginCodigo().setVisible(true);
    }
}