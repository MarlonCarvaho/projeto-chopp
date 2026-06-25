package visao;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import modelo.Produto;
import dao.ProdutoDAO;

public class TelaProdutoCodigo extends JFrame {

    private JTextField txtId, txtNome, txtCategoria, txtQuantidade, txtPreco;
    private JButton btnSalvar, btnEditar, btnExcluir;
    private JTable tabela;
    private DefaultTableModel modelo;

    private final Color BG_DARK = new Color(0x12, 0x12, 0x12);
    private final Color BG_PANEL = new Color(0x1E, 0x1E, 0x1E);
    private final Color PRIMARY = new Color(0xFF, 0x8C, 0x00);
    private final Color TEXT = new Color(0xF3, 0xF4, 0xF6);

    public TelaProdutoCodigo() {
        setTitle("Produtos - Painel Admin"); setSize(580, 580); setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); setLocationRelativeTo(null); getContentPane().setBackground(BG_DARK); setLayout(null);

        txtId = new JTextField(); txtId.setVisible(false); add(txtId);
        Font fieldsFont = new Font("Segoe UI", Font.PLAIN, 13); LineBorder border = new LineBorder(new Color(0x44, 0x44, 0x44), 1, true);

        txtNome = new JTextField(); txtNome.setFont(fieldsFont); txtNome.setBackground(BG_PANEL); txtNome.setForeground(TEXT); txtNome.setCaretColor(TEXT); txtNome.setBorder(border); txtNome.setBounds(30, 45, 240, 30); add(txtNome);
        txtCategoria = new JTextField(); txtCategoria.setFont(fieldsFont); txtCategoria.setBackground(BG_PANEL); txtCategoria.setForeground(TEXT); txtCategoria.setCaretColor(TEXT); txtCategoria.setBorder(border); txtCategoria.setBounds(290, 45, 240, 30); add(txtCategoria);
        txtQuantidade = new JTextField(); txtQuantidade.setFont(fieldsFont); txtQuantidade.setBackground(BG_PANEL); txtQuantidade.setForeground(TEXT); txtQuantidade.setCaretColor(TEXT); txtQuantidade.setBorder(border); txtQuantidade.setBounds(30, 110, 110, 30); add(txtQuantidade);
        txtPreco = new JTextField(); txtPreco.setFont(fieldsFont); txtPreco.setBackground(BG_PANEL); txtPreco.setForeground(TEXT); txtPreco.setCaretColor(TEXT); txtPreco.setBorder(border); txtPreco.setBounds(160, 110, 110, 30); add(txtPreco);

        btnSalvar = new JButton("Cadastrar"); btnSalvar.setBackground(PRIMARY); btnSalvar.setBounds(290, 110, 110, 30); add(btnSalvar);
        btnEditar = new JButton("Alterar"); btnEditar.setBackground(new Color(0x3B, 0x82, 0xF6)); btnEditar.setForeground(TEXT); btnEditar.setBounds(420, 110, 110, 30); add(btnEditar);
        btnExcluir = new JButton("Excluir"); btnExcluir.setBackground(new Color(0xEF, 0x44, 0x44)); btnExcluir.setForeground(TEXT); btnExcluir.setBounds(30, 160, 110, 30); add(btnExcluir);

        modelo = new DefaultTableModel() { @Override public boolean isCellEditable(int r, int c) { return false; } };
        modelo.addColumn("ID"); modelo.addColumn("NOME"); modelo.addColumn("CAT"); modelo.addColumn("QTD"); modelo.addColumn("PREÇO");

        tabela = new JTable(modelo); tabela.setBackground(BG_PANEL); tabela.setForeground(TEXT); tabela.setRowHeight(30);
        JTableHeader header = tabela.getTableHeader(); header.setBackground(BG_DARK); header.setForeground(TEXT);
        JScrollPane scroll = new JScrollPane(tabela); scroll.setBounds(30, 210, 500, 300); add(scroll);

        atualizarTabela();

        tabela.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int l = tabela.getSelectedRow();
                txtId.setText(tabela.getValueAt(l, 0).toString()); txtNome.setText(tabela.getValueAt(l, 1).toString());
                txtCategoria.setText(tabela.getValueAt(l, 2).toString()); txtQuantidade.setText(tabela.getValueAt(l, 3).toString().replace(" un.",""));
                txtPreco.setText(tabela.getValueAt(l, 4).toString().replace("R$ ",""));
            }
        });

        btnSalvar.addActionListener(e -> {
            try { Produto p = new Produto(); p.setNome(txtNome.getText()); p.setCategoria(txtCategoria.getText()); p.setQuantidade(Integer.parseInt(txtQuantidade.getText())); p.setPreco(Double.parseDouble(txtPreco.getText())); new ProdutoDAO().cadastrar(p); atualizarTabela(); } catch(Exception ex){}
        });
        btnEditar.addActionListener(e -> {
            try { if(txtId.getText().isEmpty()) return; Produto p = new Produto(); p.setId(Integer.parseInt(txtId.getText())); p.setNome(txtNome.getText()); p.setCategoria(txtCategoria.getText()); p.setQuantidade(Integer.parseInt(txtQuantidade.getText())); p.setPreco(Double.parseDouble(txtPreco.getText())); new ProdutoDAO().alterar(p); atualizarTabela(); } catch(Exception ex){}
        });
        btnExcluir.addActionListener(e -> {
            try { if(txtId.getText().isEmpty()) return; new ProdutoDAO().excluir(Integer.parseInt(txtId.getText())); atualizarTabela(); } catch(Exception ex){}
        });
    }

    private void atualizarTabela() {
        try { modelo.setNumRows(0); for (Produto p : new ProdutoDAO().listar()) modelo.addRow(new Object[]{ p.getId(), p.getNome(), p.getCategoria(), p.getQuantidade() + " un.", "R$ " + String.format("%.2f", p.getPreco()) }); } catch (Exception e) {}
    }
}