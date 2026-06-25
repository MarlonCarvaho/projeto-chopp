package visao;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import modelo.Despesa;
import dao.DespesaDAO;

public class TelaFinanceiroCodigo extends JFrame {

    private JTextField txtId, txtDescricao, txtValor;
    private JButton btnSalvar, btnExcluir;
    private JTable tabela;
    private DefaultTableModel modelo;

    private final Color BG_DARK = new Color(0x12, 0x12, 0x12);
    private final Color BG_PANEL = new Color(0x1E, 0x1E, 0x1E);
    private final Color PRIMARY = new Color(0xFF, 0x8C, 0x00);
    private final Color TEXT = new Color(0xF3, 0xF4, 0xF6);

    public TelaFinanceiroCodigo() {
        setTitle("Financeiro"); setSize(450, 500); setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); setLocationRelativeTo(null); getContentPane().setBackground(BG_DARK); setLayout(null);

        txtId = new JTextField(); txtId.setVisible(false); add(txtId);
        Font fieldsFont = new Font("Segoe UI", Font.PLAIN, 13); LineBorder border = new LineBorder(new Color(0x44, 0x44, 0x44), 1, true);

        txtDescricao = new JTextField(); txtDescricao.setFont(fieldsFont); txtDescricao.setBackground(BG_PANEL); txtDescricao.setForeground(TEXT); txtDescricao.setBorder(border); txtDescricao.setBounds(30, 45, 250, 30); add(txtDescricao);
        txtValor = new JTextField(); txtValor.setFont(fieldsFont); txtValor.setBackground(BG_PANEL); txtValor.setForeground(TEXT); txtValor.setBorder(border); txtValor.setBounds(290, 45, 110, 30); add(txtValor);

        btnSalvar = new JButton("Registrar"); btnSalvar.setBackground(PRIMARY); btnSalvar.setBounds(30, 95, 150, 35); add(btnSalvar);
        btnExcluir = new JButton("Excluir"); btnExcluir.setBackground(new Color(0xEF, 0x44, 0x44)); btnExcluir.setForeground(TEXT); btnExcluir.setBounds(190, 95, 120, 35); add(btnExcluir);

        modelo = new DefaultTableModel() { @Override public boolean isCellEditable(int r, int c) { return false; } };
        modelo.addColumn("ID"); modelo.addColumn("DESCRIÇÃO"); modelo.addColumn("VALOR");

        tabela = new JTable(modelo); tabela.setBackground(BG_PANEL); tabela.setForeground(TEXT); tabela.setRowHeight(30);
        JTableHeader header = tabela.getTableHeader(); header.setBackground(BG_DARK); header.setForeground(TEXT);
        JScrollPane scroll = new JScrollPane(tabela); scroll.setBounds(30, 150, 370, 280); add(scroll);

        atualizarTabela();

        tabela.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int l = tabela.getSelectedRow();
                txtId.setText(tabela.getValueAt(l, 0).toString()); txtDescricao.setText(tabela.getValueAt(l, 1).toString());
                txtValor.setText(tabela.getValueAt(l, 2).toString().replace("R$ ","").replace(",","."));
            }
        });

        btnSalvar.addActionListener(e -> {
            try { Despesa d = new Despesa(); d.setDescricao(txtDescricao.getText()); d.setValor(Double.parseDouble(txtValor.getText().replace(",","."))); new DespesaDAO().cadastrar(d); atualizarTabela(); } catch(Exception ex){}
        });
        btnExcluir.addActionListener(e -> {
            try { if(!txtId.getText().isEmpty()) new DespesaDAO().excluir(Integer.parseInt(txtId.getText())); atualizarTabela(); } catch(Exception ex){}
        });
    }

    private void atualizarTabela() {
        try { modelo.setNumRows(0); for (Despesa d : new DespesaDAO().listar()) modelo.addRow(new Object[]{ d.getId(), d.getDescricao(), "R$ " + String.format("%.2f", d.getValor()) }); } catch (Exception e) {}
    }
}