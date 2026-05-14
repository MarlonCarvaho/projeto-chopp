from flask import Flask, render_template, jsonify, request, session, redirect, url_for
import os
import psycopg2
from dotenv import load_dotenv
from datetime import datetime
import gspread 
from oauth2client.service_account import ServiceAccountCredentials 

load_dotenv()

# --- FUNÇÃO PARA PEGAR O CAMINHO DO JSON (AJUSTADA PARA O RENDER) ---
def buscar_caminho_json():
    # Primeiro tenta achar na pasta atual (raiz), que é onde o Render coloca o Secret File
    caminho_raiz = os.path.join(os.getcwd(), 'credenciais.json')
    if os.path.exists(caminho_raiz):
        return caminho_raiz
    # Se não achar na raiz, procura dentro da pasta backend (como era no seu PC)
    return os.path.join(os.path.dirname(os.path.abspath(__file__)), 'credenciais.json')

# --- FUNÇÕES GOOGLE SHEETS ---
def sincronizar_google_sheets(nome_prod, categoria, qtd, preco):
    try:
        caminho_json = buscar_caminho_json()
        escopo = ["https://spreadsheets.google.com/feeds", 'https://www.googleapis.com/auth/spreadsheets',
                  "https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/drive"]
        creds = ServiceAccountCredentials.from_json_keyfile_name(caminho_json, escopo)
        client = gspread.authorize(creds)
        planilha = client.open("Estoque Chopp").sheet1
        planilha.append_row([nome_prod, categoria, qtd, preco, str(datetime.now().strftime("%d/%m/%Y %H:%M:%S"))])
        return True
    except Exception as e:
        print(f"Erro ao adicionar na planilha: {e}")
        return False

def atualizar_estoque_google(nome_prod, novo_estoque):
    try:
        caminho_json = buscar_caminho_json()
        escopo = ["https://spreadsheets.google.com/feeds", 'https://www.googleapis.com/auth/spreadsheets',
                  "https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/drive"]
        creds = ServiceAccountCredentials.from_json_keyfile_name(caminho_json, escopo)
        client = gspread.authorize(creds)
        planilha = client.open("Estoque Chopp").sheet1
        celula = planilha.find(nome_prod)
        planilha.update_cell(celula.row, 3, novo_estoque)
        return True
    except Exception as e:
        print(f"Erro ao atualizar venda na planilha: {e}")
        return False

def sincronizar_do_google_para_banco():
    try:
        caminho_json = buscar_caminho_json()
        escopo = ["https://spreadsheets.google.com/feeds", 'https://www.googleapis.com/auth/spreadsheets',
                  "https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/drive"]
        creds = ServiceAccountCredentials.from_json_keyfile_name(caminho_json, escopo)
        client = gspread.authorize(creds)
        planilha = client.open("Estoque Chopp").sheet1
        dados_planilha = planilha.get_all_values()
        
        conn = get_db_connection()
        cur = conn.cursor()
        
        for linha in dados_planilha:
            if not linha or len(linha) < 3:
                continue
            nome_prod = linha[0].strip()
            qtd_str = linha[2].strip()
            if qtd_str.isdigit():
                nova_qtd = int(qtd_str)
                cur.execute("UPDATE produto SET quantidade_estoque = %s WHERE nome = %s", (nova_qtd, nome_prod))
        
        conn.commit()
        cur.close()
        conn.close()
        return True
    except Exception as e:
        print(f"Erro ao trazer dados da planilha: {e}")
        return False

# --- CONFIGURAÇÃO DO FLASK E BANCO DE DADOS ---
# Procura a pasta frontend um nível acima da pasta backend
pasta_frontend = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'frontend'))
app = Flask(__name__, template_folder=pasta_frontend)
app.secret_key = os.getenv('SECRET_KEY', 'chave_super_secreta_padrao')

def get_db_connection():
    # O Render usa a variável DATABASE_URL para conectar
    db_url = os.getenv('DATABASE_URL')
    if db_url:
        return psycopg2.connect(db_url)
    else:
        # Se não tiver DATABASE_URL (caso você rode no seu PC), usa os dados individuais
        return psycopg2.connect(
            host=os.getenv('DB_HOST'),
            database=os.getenv('DB_NAME'),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            port=os.getenv('DB_PORT')
        )

@app.route('/')
def home():
    return render_template('index.html')

@app.route('/cadastro')
def tela_cadastro():
    return render_template('cadastro.html')

@app.route('/cadastrar', methods=['POST'])
def cadastrar():
    nome = request.form['nome']
    email = request.form['email']
    senha = request.form['senha']
    documento = request.form['documento']
    data_nasc_str = request.form['data_nascimento']
    data_nascimento = datetime.strptime(data_nasc_str, '%Y-%m-%d').date()
    hoje = datetime.today().date()
    idade = hoje.year - data_nascimento.year - ((hoje.month, hoje.day) < (data_nascimento.month, data_nascimento.day))
    
    if idade < 18:
        return render_template('cadastro.html', erro="Acesso negado: Menor de 18 anos.")
        
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute('INSERT INTO usuario (nome, email, senha, documento, data_nascimento) VALUES (%s, %s, %s, %s, %s)',
                    (nome, email, senha, documento, data_nascimento))
        conn.commit()
        cur.close()
        conn.close()
        return render_template('cadastro.html', sucesso="Cadastro realizado!")
    except Exception as e:
        return render_template('cadastro.html', erro=f"Erro: {str(e)}")

@app.route('/login')
def tela_login():
    return render_template('login.html')

@app.route('/fazer_login', methods=['POST'])
def fazer_login():
    email = request.form['email']
    senha = request.form['senha']
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute('SELECT id_usuario, nome, tipo_usuario FROM usuario WHERE email = %s AND senha = %s', (email, senha))
    usuario = cur.fetchone()
    cur.close()
    conn.close()
    
    if usuario:
        session['id_usuario'] = usuario[0]
        session['nome'] = usuario[1]
        session['tipo_usuario'] = usuario[2]
        return redirect(url_for('painel_admin')) if usuario[2] == 'admin' else redirect(url_for('painel_cliente'))
    return render_template('login.html', erro="E-mail ou senha incorretos.")

# ----- ÁREA DO ADMINISTRADOR -----
@app.route('/painel_admin')
def painel_admin():
    if 'id_usuario' in session and session['tipo_usuario'] == 'admin':
        sucesso = request.args.get('sucesso')
        try:
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute('SELECT * FROM usuario ORDER BY id_usuario DESC')
            usuarios = cur.fetchall()
            cur.execute('SELECT * FROM produto ORDER BY id_produto DESC')
            produtos = cur.fetchall()
            cur.execute('SELECT * FROM equipamento ORDER BY id_equipamento DESC')
            equipamentos = cur.fetchall()
            cur.execute('''
                SELECT p.id_pedido, u.nome, prod.nome, ip.quantidade, p.status, p.data_pedido, e.nome
                FROM pedido p
                JOIN usuario u ON p.id_usuario = u.id_usuario
                JOIN item_pedido ip ON p.id_pedido = ip.id_pedido
                JOIN produto prod ON ip.id_produto = prod.id_produto
                LEFT JOIN equipamento e ON p.id_equipamento = e.id_equipamento
                ORDER BY p.id_pedido DESC
            ''')
            pedidos = cur.fetchall()
            cur.close()
            conn.close()
            return render_template('admin.html', usuarios=usuarios, produtos=produtos, pedidos=pedidos, equipamentos=equipamentos, sucesso=sucesso)
        except Exception as e:
            return f"Erro: {str(e)}"
    return redirect(url_for('tela_login'))

@app.route('/cadastrar_produto', methods=['POST'])
def cadastrar_produto():
    if 'id_usuario' in session and session['tipo_usuario'] == 'admin':
        nome = request.form['nome']
        categoria = request.form['categoria']
        quantidade = request.form['quantidade']
        preco = request.form['preco']
        try:
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute('INSERT INTO produto (nome, categoria, quantidade_estoque, preco) VALUES (%s, %s, %s, %s)',
                        (nome, categoria, quantidade, preco))
            conn.commit()
            cur.close()
            conn.close()
            sincronizar_google_sheets(nome, categoria, quantidade, preco)
            return redirect(url_for('painel_admin', sucesso="Produto cadastrado com sucesso!"))
        except Exception as e:
            return f"Erro: {str(e)}"
    return redirect(url_for('tela_login'))

@app.route('/sincronizar_estoque')
def sincronizar_estoque():
    if 'id_usuario' in session and session['tipo_usuario'] == 'admin':
        if sincronizar_do_google_para_banco():
            return redirect(url_for('painel_admin', sucesso="Estoque atualizado com a Planilha!"))
        return redirect(url_for('painel_admin', erro="Falha na sincronização."))
    return redirect(url_for('tela_login'))

@app.route('/atualizar_pedido', methods=['POST'])
def atualizar_pedido():
    if 'id_usuario' in session and session['tipo_usuario'] == 'admin':
        id_pedido = request.form['id_pedido']
        novo_status = request.form['status']
        try:
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute("UPDATE pedido SET status = %s WHERE id_pedido = %s", (novo_status, id_pedido))
            conn.commit()
            cur.close()
            conn.close()
            return redirect(url_for('painel_admin', sucesso=f"Pedido #{id_pedido} alterado para {novo_status.upper()}!"))
        except Exception as e:
            return f"Erro: {str(e)}"
    return redirect(url_for('tela_login'))

@app.route('/cadastrar_equipamento', methods=['POST'])
def cadastrar_equipamento():
    if 'id_usuario' in session and session['tipo_usuario'] == 'admin':
        nome = request.form['nome']
        status = request.form['status']
        try:
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute('INSERT INTO equipamento (nome, status) VALUES (%s, %s)', (nome, status))
            conn.commit()
            cur.close()
            conn.close()
            return redirect(url_for('painel_admin', sucesso="Equipamento cadastrado com sucesso!"))
        except Exception as e:
            return f"Erro: {str(e)}"
    return redirect(url_for('tela_login'))

@app.route('/atualizar_equipamento', methods=['POST'])
def atualizar_equipamento():
    if 'id_usuario' in session and session['tipo_usuario'] == 'admin':
        id_equipamento = request.form['id_equipamento']
        novo_status = request.form['status']
        try:
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute("UPDATE equipamento SET status = %s WHERE id_equipamento = %s", (novo_status, id_equipamento))
            conn.commit()
            cur.close()
            conn.close()
            return redirect(url_for('painel_admin', sucesso=f"Status do equipamento #{id_equipamento} alterado!"))
        except Exception as e:
            return f"Erro: {str(e)}"
    return redirect(url_for('tela_login'))

# ----- ÁREA DO CLIENTE -----
@app.route('/painel_cliente')
def painel_cliente():
    if 'id_usuario' in session:
        sucesso = request.args.get('sucesso')
        erro = request.args.get('erro')
        id_user_logado = session['id_usuario']
        try:
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute('SELECT * FROM produto WHERE quantidade_estoque > 0 ORDER BY nome ASC')
            produtos = cur.fetchall()
            cur.execute("SELECT * FROM equipamento WHERE status = 'disponivel'")
            equipamentos = cur.fetchall()
            cur.execute('''
                SELECT p.id_pedido, prod.nome, ip.quantidade, p.data_pedido, p.status, e.nome 
                FROM pedido p
                JOIN item_pedido ip ON p.id_pedido = ip.id_pedido
                JOIN produto prod ON ip.id_produto = prod.id_produto
                LEFT JOIN equipamento e ON p.id_equipamento = e.id_equipamento
                WHERE p.id_usuario = %s
                ORDER BY p.data_pedido DESC
            ''', (id_user_logado,))
            meus_pedidos = cur.fetchall()
            cur.close()
            conn.close()
            return render_template('cliente.html', nome=session['nome'], produtos=produtos, equipamentos=equipamentos, meus_pedidos=meus_pedidos, sucesso=sucesso, erro=erro)
        except Exception as e:
            return f"Erro ao carregar loja: {str(e)}"
    return redirect(url_for('tela_login'))

@app.route('/fazer_pedido', methods=['POST'])
def fazer_pedido():
    if 'id_usuario' in session:
        id_produto = request.form['id_produto']
        quantidade_pedida = int(request.form['quantidade'])
        id_equipamento = request.form.get('id_equipamento')
        conn = get_db_connection()
        cur = conn.cursor()
        try:
            if id_equipamento:
                cur.execute("INSERT INTO pedido (id_usuario, endereco_entrega, status, id_equipamento) VALUES (%s, %s, %s, %s) RETURNING id_pedido", 
                            (session['id_usuario'], 'Retirada', 'pendente', id_equipamento))
                id_pedido = cur.fetchone()[0]
                cur.execute("UPDATE equipamento SET status = 'em_uso' WHERE id_equipamento = %s", (id_equipamento,))
            else:
                cur.execute("INSERT INTO pedido (id_usuario, endereco_entrega, status) VALUES (%s, %s, %s) RETURNING id_pedido", 
                            (session['id_usuario'], 'Retirada', 'pendente'))
                id_pedido = cur.fetchone()[0]
            cur.execute("INSERT INTO item_pedido (id_pedido, id_produto, quantidade) VALUES (%s, %s, %s)",
                        (id_pedido, id_produto, quantidade_pedida))
            cur.execute("UPDATE produto SET quantidade_estoque = quantidade_estoque - %s WHERE id_produto = %s",
                        (quantidade_pedida, id_produto))
            cur.execute("SELECT nome, quantidade_estoque FROM produto WHERE id_produto = %s", (id_produto,))
            p_info = cur.fetchone()
            conn.commit()
            cur.close()
            conn.close()
            atualizar_estoque_google(p_info[0], p_info[1])
            return redirect(url_for('painel_cliente', sucesso="Pedido feito com sucesso!"))
        except Exception as e:
            return redirect(url_for('painel_cliente', erro=f"Erro: {str(e)}"))
    return redirect(url_for('tela_login'))

@app.route('/logout')
def logout():
    session.clear()
    return redirect(url_for('tela_login'))

if __name__ == '__main__':
    # No PC, roda na porta 5000. Na nuvem, o Render define a porta automaticamente.
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port)