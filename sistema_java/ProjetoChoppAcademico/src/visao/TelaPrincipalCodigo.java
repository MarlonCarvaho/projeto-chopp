package visao;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class TelaPrincipalCodigo extends JFrame {

    private final Color BG_DARK = new Color(0x12, 0x12, 0x12);
    private final Color PRIMARY = new Color(0xFF, 0x8C, 0x00);
    private final Color TEXT = new Color(0xF3, 0xF4, 0xF6);
    private final Color DANGER = new Color(0xEF, 0x44, 0x44);

    public TelaPrincipalCodigo() {
        setTitle("Painel Administrativo - Projeto Chopp");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(null);

        JLabel lblTitulo = new JLabel("PAINEL DE GERENCIAMENTO", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16)); lblTitulo.setForeground(PRIMARY);
        lblTitulo.setBounds(20, 30, 340, 30); add(lblTitulo);

        JButton btnEstoque = new JButton("Estoque Geral");
        btnEstoque.setFont(new Font("Segoe UI", Font.BOLD, 14)); btnEstoque.setBackground(PRIMARY); btnEstoque.setForeground(BG_DARK); btnEstoque.setFocusPainted(false); btnEstoque.setBorder(null); btnEstoque.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEstoque.setBounds(80, 90, 220, 40); add(btnEstoque);

        JButton btnFinanceiro = new JButton("Controle Financeiro");
        btnFinanceiro.setFont(new Font("Segoe UI", Font.BOLD, 14)); btnFinanceiro.setBackground(new Color(0x3B, 0x82, 0xF6)); btnFinanceiro.setForeground(TEXT); btnFinanceiro.setFocusPainted(false); btnFinanceiro.setBorder(null); btnFinanceiro.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFinanceiro.setBounds(80, 150, 220, 40); add(btnFinanceiro);

        JButton btnSair = new JButton("Sair do Sistema");
        btnSair.setFont(new Font("Segoe UI", Font.BOLD, 13)); btnSair.setBackground(BG_DARK); btnSair.setForeground(DANGER); btnSair.setFocusPainted(false); btnSair.setBorder(new LineBorder(DANGER, 1, true)); btnSair.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSair.setBounds(80, 210, 220, 40); add(btnSair);

        btnEstoque.addActionListener(e -> new TelaProdutoCodigo().setVisible(true));
        btnFinanceiro.addActionListener(e -> new TelaFinanceiroCodigo().setVisible(true));
        btnSair.addActionListener(e -> {
            if(JOptionPane.showConfirmDialog(null, "Deseja sair?", "Sair", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) System.exit(0);
        });
    }
}