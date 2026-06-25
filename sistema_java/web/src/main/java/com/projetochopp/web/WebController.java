package com.projetochopp.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import com.projetochopp.web.repositorio.*;
import com.projetochopp.web.modelo.*;

import java.util.Base64;
import java.util.List;

@Controller
public class WebController {

    @Autowired private ProdutoRepository     produtoRepository;
    @Autowired private DespesaRepository     despesaRepository;
    @Autowired private EquipamentoRepository equipamentoRepository;
    @Autowired private PedidoRepository      pedidoRepository;
    @Autowired private UsuarioRepository     usuarioRepository;

    // ──────────────────────────────────────────────
    //  AUTENTICAÇÃO (LOGIN / CADASTRO)
    // ──────────────────────────────────────────────

    @GetMapping("/")
    public String telaLogin() { return "login"; }

    @PostMapping("/fazer_login")
    public String fazerLogin(@RequestParam String email,
                             @RequestParam String senha,
                             Model model,
                             HttpSession session) {
        Usuario u = usuarioRepository.findByEmailAndSenha(email, senha);
        if (u != null) {
            session.setAttribute("usuarioLogado", u.getNome());
            session.setAttribute("usuarioId",     u.getId());
            session.setAttribute("tipo_usuario",  u.getTipoUsuario());
            return u.getTipoUsuario().equals("cliente") ? "redirect:/cliente" : "redirect:/admin";
        }
        model.addAttribute("erro", "E-mail ou senha incorretos!");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/cadastro")
    public String telaCadastro() { return "cadastro"; }

    @PostMapping("/fazer_cadastro")
    public String fazerCadastro(@RequestParam String nome,
                                @RequestParam String email,
                                @RequestParam String senha,
                                RedirectAttributes ra) {
        if (usuarioRepository.findByEmail(email) != null) {
            ra.addFlashAttribute("erro", "Este e-mail já está cadastrado!");
            return "redirect:/cadastro";
        }
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenha(senha);
        u.setTipoUsuario("cliente");
        usuarioRepository.save(u);
        ra.addFlashAttribute("sucesso", "Conta criada! Faça seu login.");
        return "redirect:/";
    }

    // ──────────────────────────────────────────────
    //  MÓDULO DO CLIENTE (VITRINE / CHECKOUT)
    // ──────────────────────────────────────────────

    @GetMapping("/cliente")
    public String telaCliente(Model model, HttpSession session) {
        String nomeUsuario = (String) session.getAttribute("usuarioLogado");
        if (nomeUsuario == null) {
            return "redirect:/";
        }
        try {
            model.addAttribute("produtos", produtoRepository.findAll());
            
            // NOVO: Busca o histórico de pedidos deste cliente específico no banco
            List<Pedido> meusPedidos = pedidoRepository.findByClienteOrderByIdDesc(nomeUsuario);
            if(meusPedidos != null && !meusPedidos.isEmpty()) {
                model.addAttribute("meus_pedidos", meusPedidos);
            }
            
        } catch (Exception e) {
            model.addAttribute("produtos", List.of());
        }
        model.addAttribute("nomeUsuario", nomeUsuario);
        return "cliente";
    }

   @GetMapping("/checkout")
    public String telaCheckout(HttpSession session, Model model) {
        String nome = (String) session.getAttribute("usuarioLogado");
        if (nome == null) {
            return "redirect:/";
        }
        // Envia o nome da sessão para a tela preencher o input automaticamente
        model.addAttribute("nomeUsuario", nome);
        return "checkout";
    }

    @PostMapping("/finalizar_pedido")
    public String finalizarPedido(@RequestParam(required = false) String cliente,
                                  @RequestParam String endereco,
                                  @RequestParam String produtos,
                                  @RequestParam("valor_total") String valorTotal,
                                  HttpSession session,
                                  RedirectAttributes ra) {
        
        // Pega o nome EXATO do usuário logado na sessão
        String nomeOficial = (String) session.getAttribute("usuarioLogado");
        
        if (nomeOficial == null) {
            return "redirect:/";
        }
        
        try {
            double valor = Double.parseDouble(
                valorTotal.replace("R$", "").replace(".", "").replace(",", ".").trim()
            );
            
            // 1. Grava o pedido com o nome oficial da conta
            Pedido p = new Pedido();
            p.setCliente(nomeOficial); // <-- AQUI ESTÁ A MÁGICA QUE LIGA O PEDIDO À CONTA!
            p.setEndereco(endereco == null || endereco.isBlank() ? "Retirada na Loja" : endereco);
            p.setProdutos(produtos);
            p.setStatus("pendente");
            p.setValorTotal(valor);
            pedidoRepository.save(p);

            // 2. Processa a Baixa Automática de Estoque
            if (produtos != null && !produtos.isBlank()) {
                String[] itens = produtos.split(" \\| ");
                for (String item : itens) {
                    if (item.contains("x ")) {
                        String[] partes = item.split("x ", 2);
                        int qtdComprada = Integer.parseInt(partes[0].trim());
                        String nomeProduto = partes[1].trim();

                        Produto prod = produtoRepository.findByNome(nomeProduto);
                        if (prod != null) {
                            int estoqueAtual = prod.getQuantidade() != null ? prod.getQuantidade() : 0;
                            int novoEstoque = Math.max(0, estoqueAtual - qtdComprada);
                            prod.setQuantidade(novoEstoque);
                            produtoRepository.save(prod);
                        }
                    }
                }
            }

            ra.addFlashAttribute("sucesso", "Pedido realizado com sucesso!");
        } catch (NumberFormatException e) {
            ra.addFlashAttribute("erro", "Valor do pedido inválido. Tente novamente.");
        }
        return "redirect:/cliente";
    }

    // ──────────────────────────────────────────────
    //  PAINEL ADMINISTRATIVO (DASHBOARD)
    // ──────────────────────────────────────────────

    @GetMapping("/admin")
    public String telaAdmin(Model model, HttpSession session) {
        String tipo = (String) session.getAttribute("tipo_usuario");
        if (tipo == null || tipo.equals("cliente")) {
            return "redirect:/";
        }

        try {
            List<Produto>    listaProdutos  = produtoRepository.findAll();
            List<Despesa>    listaDespesas  = despesaRepository.findAll();
            List<Pedido>     listaPedidos   = pedidoRepository.findAll();
            List<Usuario>    listaUsuarios  = usuarioRepository.findAll();

            model.addAttribute("produtos",     listaProdutos);
            model.addAttribute("despesas",     listaDespesas);
            model.addAttribute("equipamentos", equipamentoRepository.findAll());
            model.addAttribute("pedidos",      listaPedidos);
            model.addAttribute("usuarios",     listaUsuarios);

            // Fluxo de Caixa Dinâmico
            double despesaTotal = listaDespesas.stream()
                .mapToDouble(d -> d.getValor() != null ? d.getValor() : 0.0)
                .sum();
            double receiverTotal = listaPedidos.stream()
                .mapToDouble(p -> p.getValorTotal() != null ? p.getValorTotal() : 0.0)
                .sum();
            double lucroLiquido = receiverTotal - despesaTotal;

            model.addAttribute("receita_total",        String.format("%,.2f", receiverTotal));
            model.addAttribute("despesa_total",        String.format("%,.2f", despesaTotal));
            model.addAttribute("lucro_liquido",        String.format("%,.2f", lucroLiquido));
            model.addAttribute("faturamento_semana",   String.format("%,.2f", receiverTotal));

            double valorEstoque = listaProdutos.stream()
                .mapToDouble(p -> (p.getQuantidade() != null ? p.getQuantidade() : 0)
                                * (p.getPreco()     != null ? p.getPreco()     : 0.0))
                .sum();

            model.addAttribute("total_usuarios",       usuarioRepository.count());
            model.addAttribute("total_pedidos_semana", pedidoRepository.count());
            model.addAttribute("valor_estoque",        String.format("%,.2f", valorEstoque));

        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("tipo_usuario",  tipo);
        model.addAttribute("nome_usuario",  session.getAttribute("usuarioLogado"));
        return "admin";
    }

    // ──────────────────────────────────────────────
    //  GESTÃO DE PRODUTOS
    // ──────────────────────────────────────────────

    @PostMapping("/cadastrar_produto")
    public String cadastrarProduto(@RequestParam String nome,
                                   @RequestParam String categoria,
                                   @RequestParam Integer quantidade,
                                   @RequestParam String preco,
                                   @RequestParam(required = false) String variacao,
                                   @RequestParam(value = "imagem_arquivo", required = false) MultipartFile imagemArquivo,
                                   RedirectAttributes ra) {
        try {
            Produto p = new Produto();
            p.setNome(nome);
            p.setCategoria(categoria);
            p.setQuantidade(quantidade);
            p.setPreco(Double.parseDouble(preco.replace(",", ".")));
            p.setVariacao(variacao);
            if (imagemArquivo != null && !imagemArquivo.isEmpty()) {
                p.setImagemBase64(Base64.getEncoder().encodeToString(imagemArquivo.getBytes()));
            }
            produtoRepository.save(p);
            ra.addFlashAttribute("sucesso", "Produto cadastrado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao cadastrar produto: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/editar_produto")
    public String editarProduto(@RequestParam("id_produto") Integer id,
                                @RequestParam String nome,
                                @RequestParam String categoria,
                                @RequestParam Integer quantidade,
                                @RequestParam String preco,
                                @RequestParam(required = false) String variacao,
                                @RequestParam(value = "imagem_arquivo", required = false) MultipartFile imagemArquivo,
                                RedirectAttributes ra) {
        produtoRepository.findById(id).ifPresent(p -> {
            try {
                p.setNome(nome);
                p.setCategoria(categoria);
                p.setQuantidade(quantidade);
                p.setPreco(Double.parseDouble(preco.replace(",", ".")));
                p.setVariacao(variacao);
                if (imagemArquivo != null && !imagemArquivo.isEmpty()) {
                    p.setImagemBase64(Base64.getEncoder().encodeToString(imagemArquivo.getBytes()));
                }
                produtoRepository.save(p);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        ra.addFlashAttribute("sucesso", "Produto atualizado!");
        return "redirect:/admin";
    }

    @PostMapping("/deletar_produto/{id}")
    public String deletarProduto(@PathVariable Integer id, RedirectAttributes ra) {
        produtoRepository.deleteById(id);
        ra.addFlashAttribute("sucesso", "Produto removido.");
        return "redirect:/admin";
    }

    // ──────────────────────────────────────────────
    //  GESTÃO DE DESPESAS
    // ──────────────────────────────────────────────

    @PostMapping("/cadastrar_despesa")
    public String cadastrarDespesa(@RequestParam String descricao,
                                   @RequestParam String valor,
                                   RedirectAttributes ra) {
        try {
            Despesa d = new Despesa();
            d.setDescricao(descricao);
            d.setValor(Double.parseDouble(valor.replace(",", ".")));
            despesaRepository.save(d);
            ra.addFlashAttribute("sucesso", "Despesa lançada!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Valor inválido para a despesa.");
        }
        return "redirect:/admin";
    }

    @PostMapping("/deletar_despesa/{id}")
    public String deletarDespesa(@PathVariable Integer id) {
        despesaRepository.deleteById(id);
        return "redirect:/admin";
    }

    // ──────────────────────────────────────────────
    //  GESTÃO DE EQUIPAMENTOS
    // ──────────────────────────────────────────────

    @PostMapping("/cadastrar_equipamento")
    public String cadastrarEquipamento(@RequestParam String nome,
                                       @RequestParam String status,
                                       RedirectAttributes ra) {
        Equipamento e = new Equipamento();
        e.setNome(nome);
        e.setStatus(status);
        equipamentoRepository.save(e);
        ra.addFlashAttribute("sucesso", "Equipamento cadastrado!");
        return "redirect:/admin";
    }

    @PostMapping("/atualizar_equipamento")
    public String atualizarEquipamento(@RequestParam("id_equipamento") Integer id,
                                       @RequestParam String status) {
        equipamentoRepository.findById(id).ifPresent(e -> {
            e.setStatus(status);
            equipamentoRepository.save(e);
        });
        return "redirect:/admin";
    }

    // ──────────────────────────────────────────────
    //  GESTÃO DE PEDIDOS
    // ──────────────────────────────────────────────

    @PostMapping("/atualizar_pedido")
    public String atualizarPedido(@RequestParam("id_pedido") Integer id,
                                  @RequestParam String status) {
        pedidoRepository.findById(id).ifPresent(p -> {
            p.setStatus(status);
            pedidoRepository.save(p);
        });
        return "redirect:/admin";
    }

    // ──────────────────────────────────────────────
    //  GESTÃO DE CONTAS USUÁRIOS
    // ──────────────────────────────────────────────

    @PostMapping("/promover_usuario/{id}/{cargo}")
    public String promoverUsuario(@PathVariable Integer id,
                                  @PathVariable String cargo,
                                  HttpSession session) {
        Integer meuId = (Integer) session.getAttribute("usuarioId");
        if (id.equals(meuId) && !cargo.equals("admin")) {
            return "redirect:/admin";
        }
        usuarioRepository.findById(id).ifPresent(u -> {
            u.setTipoUsuario(cargo);
            usuarioRepository.save(u);
        });
        return "redirect:/admin";
    }

    @PostMapping("/deletar_usuario/{id}")
    public String deletarUsuario(@PathVariable Integer id,
                                 HttpSession session) {
        Integer meuId = (Integer) session.getAttribute("usuarioId");
        if (!id.equals(meuId)) {
            usuarioRepository.deleteById(id);
        }
        return "redirect:/admin";
    }
}