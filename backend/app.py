from flask import Flask, render_template, jsonify, request, session, redirect, url_for
import os
import psycopg2
from dotenv import load_dotenv
from datetime import datetime
import gspread 
from oauth2client.service_account import ServiceAccountCredentials 

load_dotenv()

def buscar_caminho_json():
    caminho_raiz = os.path.join(os.getcwd(), 'credenciais.json')
    if os.path.exists(caminho_raiz): return caminho_raiz
    return os.path.join(os.path.dirname(os.path.abspath(__file__)), 'credenciais.json')

def sincronizar_google_sheets(nome_prod, categoria, qtd, preco):
    try:
        caminho_json = buscar_caminho_json()
        escopo = ["https://spreadsheets.google.com/feeds", 'https://www.googleapis.com/auth/spreadsheets', "https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/drive"]
        creds = ServiceAccountCredentials.from_json_keyfile_name(caminho_json, escopo)
        client = gspread.authorize(creds)
        planilha = client.open("Estoque Chopp").sheet1
        planilha.append_row([nome_prod, categoria, qtd, preco, str(datetime.now().strftime("%d/%m/%Y %H:%M:%S"))])
        return True
    except Exception as e: return False

def atualizar_estoque_google(nome_prod, novo_estoque):
    try:
        caminho_json = buscar_caminho_json()
        escopo = ["https://spreadsheets.google.com/feeds", 'https://www.googleapis.com/auth/spreadsheets', "https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/drive"]
        creds = ServiceAccountCredentials.from_json_keyfile_name(caminho_json, escopo)
        client = gspread.authorize(creds)
        planilha = client.open("Estoque Chopp").sheet1
        celula = planilha.find(nome_prod)
        planilha.update_cell(celula.row, 3, novo_estoque)
        return True
    except Exception as e: return False

def sincronizar_do_google_para_banco():
    try:
        caminho_json = buscar_caminho_json()
        escopo = ["https://spreadsheets.google.com/feeds", 'https://www.googleapis.com/auth/spreadsheets', "https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/drive"]
        creds = ServiceAccountCredentials.from_json_keyfile_name(caminho_json, escopo)
        client = gspread.authorize(creds)
        planilha = client.open("Estoque Chopp").sheet1
        dados_planilha = planilha.get_all_values()
        
        conn = get_db_connection()
        cur = conn.cursor()
        for linha in dados_planilha:
            if not linha or len(linha) < 3: continue
            nome_prod = linha[0].strip()
            # ERRO CORRIGIDO AQUI:
            qtd_str = linha[2].strip()
            if qtd_str.isdigit():
                cur.execute("UPDATE produto SET quantidade_estoque = %s WHERE nome = %s", (int(qtd_str), nome_prod))
        conn.commit()
        cur.close()
        conn.close()
        return True
    except Exception as e: return False

pasta_frontend = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'frontend'))
app = Flask(__name__, template_folder=pasta_frontend, static_folder=os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'static')))
app.secret_key = os.getenv('SECRET_KEY', 'chave_super_secreta_padrao')

def get_db_connection():
    db_url = os.getenv('DATABASE_URL')
    if db_url: return psycopg2.connect(db_url)
    return psycopg2.connect(host=os.getenv('DB_HOST'), database=os.getenv('DB_NAME'), user=os.getenv('DB_USER'), password=os.getenv('DB_PASSWORD'), port=os.getenv('DB_PORT'))

@app.route('/')
def home(): return render_template('index.html')

@app.route('/cadastro')
def tela_cadastro(): return render_template('cadastro.html')

@app.route('/cadastrar', methods=['POST'])
def cadastrar():
    nome = request.form['nome']
    email = request.form['email']
    senha = request.form['senha']
    documento = request.form['documento']
    data_nascimento = datetime.strptime(request.form['data_nascimento'], '%Y-%m-%d').date()
    idade = datetime.today().year - data_nascimento.year - ((datetime.today().month, datetime.today().day) < (data_nascimento.month, data_nascimento.day))
    
    if idade < 18: return render_template('cadastro.html', erro="Menor de 18 anos.")
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute('INSERT INTO usuario (nome, email, senha, documento, data_nascimento) VALUES (%s, %s, %s, %s, %s)', (nome, email, senha, documento, data_nascimento))
        conn.commit()
        cur.close()
        conn.close()
        return render_template('cadastro.html', sucesso="Cadastro realizado!")
    except Exception as e: return render_template('cadastro.html', erro=f"Erro: {str(e)}")

@app.route('/login')
def tela_login(): return render_template('login.html')

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
        session['carrinho'] = [] 
        return redirect(url_for('painel_admin')) if usuario[2] in ['admin', 'auxiliar'] else redirect(url_for('painel_cliente'))
    return render_template('login.html', erro="E-mail ou senha incorretos.")

# ----- ÁREA DE GERENCIAMENTO (ADMIN / AUXILIAR) -----
@app.route('/painel_admin')
def painel_admin():
    if 'id_usuario' in session and session['tipo_usuario'] in ['admin', 'auxiliar']:
        sucesso = request.args.get('sucesso')
        erro = request.args.get('erro')
        try:
            conn = get_db_connection()
            cur = conn.cursor()
            
            cur.execute('SELECT id_usuario, nome, email, tipo_usuario FROM usuario ORDER BY id_usuario DESC')
            usuarios = cur.fetchall()
            
            cur.execute('SELECT * FROM produto ORDER BY id_produto DESC')
            produtos = cur.fetchall()
            
            cur.execute('SELECT * FROM equipamento ORDER BY id_equipamento DESC')
            equipamentos = cur.fetchall()
            
            cur.execute('''
                SELECT p.id_pedido, u.nome, 
                       string_agg(prod.nome || ' (' || COALESCE(ip.variacao_escolhida, 'Padrão') || ') - ' || ip.quantidade || 'x', ', ') as itens, 
                       p.status, p.data_pedido, e.nome, p.endereco_entrega
                FROM pedido p
                JOIN usuario u ON p.id_usuario = u.id_usuario
                JOIN item_pedido ip ON p.id_pedido = ip.id_pedido
                JOIN produto prod ON ip.id_produto = prod.id_produto
                LEFT JOIN equipamento e ON p.id_equipamento = e.id_equipamento
                GROUP BY p.id_pedido, u.nome, p.status, p.data_pedido, e.nome, p.endereco_entrega
                ORDER BY p.id_pedido DESC
            ''')
            pedidos = cur.fetchall()
            
            faturamento_hoje = 0
            if session['tipo_usuario'] == 'admin':
                cur.execute('''
                    SELECT COALESCE(SUM(ip.quantidade * prod.preco), 0)
                    FROM pedido p
                    JOIN item_pedido ip ON p.id_pedido = ip.id_pedido
                    JOIN produto prod ON ip.id_produto = prod.id_produto
                    WHERE DATE(p.data_pedido) = CURRENT_DATE
                ''')
                faturamento_hoje = cur.fetchone()[0]
            
            cur.close()
            conn.close()
            return render_template('admin.html', usuarios=usuarios, produtos=produtos, pedidos=pedidos, 
                                   equipamentos=equipamentos, faturamento_hoje=faturamento_hoje, 
                                   sucesso=sucesso, erro=erro)
        except Exception as e:
            return f"Erro: {str(e)}"
    return redirect(url_for('tela_login'))

@app.route('/cadastrar_produto', methods=['POST'])
def cadastrar_produto():
    if 'id_usuario' in session and session['tipo_usuario'] in ['admin', 'auxiliar']:
        nome = request.form['nome']
        categoria = request.form['categoria']
        quantidade = request.form['quantidade']
        preco = request.form['preco']
        imagem = request.form.get('imagem_url', 'default.webp')
        variacao = request.form.get('variacao', '')
        try:
            conn = get_db_connection()
            cur = conn.cursor()
            # ERRO CORRIGIDO AQUI EMBAIXO: tirei o "quantity ="
            cur.execute('INSERT INTO produto (nome, categoria, quantidade_estoque, preco, imagem_url, variacao) VALUES (%s, %s, %s, %s, %s, %s)',
                        (nome, categoria, quantidade, preco, imagem, variacao))
            conn.commit()
            cur.close()
            conn.close()
            sincronizar_google_sheets(nome, categoria, quantidade, preco)
            return redirect(url_for('painel_admin', sucesso="Produto cadastrado com sucesso!"))
        except Exception as e:
            return redirect(url_for('painel_admin', erro=f"Erro ao cadastrar: {str(e)}"))
    return redirect(url_for('tela_login'))

@app.route('/deletar_produto/<int:id_produto>', methods=['POST'])
def deletar_produto(id_produto):
    if 'id_usuario' in session and session['tipo_usuario'] in ['admin', 'auxiliar']:
        try:
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute("DELETE FROM item_pedido WHERE id_produto = %s", (id_produto,))
            cur.execute("DELETE FROM produto WHERE id_produto = %s", (id_produto,))
            conn.commit()
            cur.close()
            conn.close()
            return redirect(url_for('painel_admin', sucesso="Produto eliminado!"))
        except Exception as e:
            return redirect(url_for('painel_admin', erro=f"Erro ao eliminar: {str(e)}"))
    return redirect(url_for('tela_login'))

@app.route('/deletar_pedido/<int:id_pedido>', methods=['POST'])
def deletar_pedido(id_pedido):
    if 'id_usuario' in session and session['tipo_usuario'] == 'admin':
        try:
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute("DELETE FROM item_pedido WHERE id_pedido = %s", (id_pedido,))
            cur.execute("DELETE FROM pedido WHERE id_pedido = %s", (id_pedido,))
            conn.commit()
            cur.close()
            conn.close()
            return redirect(url_for('painel_admin', sucesso="Pedido excluído!"))
        except Exception as e:
            return redirect(url_for('painel_admin', erro=f"Erro ao excluir: {str(e)}"))
    return redirect(url_for('tela_login'))

@app.route('/sincronizar_estoque')
def sincronizar_estoque():
    if 'id_usuario' in session and session['tipo_usuario'] in ['admin', 'auxiliar']:
        if sincronizar_do_google_para_banco(): return redirect(url_for('painel_admin', sucesso="Sincronizado!"))
        return redirect(url_for('painel_admin', erro="Falha."))
    return redirect(url_for('tela_login'))

@app.route('/atualizar_pedido', methods=['POST'])
def atualizar_pedido():
    if 'id_usuario' in session and session['tipo_usuario'] in ['admin', 'auxiliar']:
        id_pedido = request.form['id_pedido']
        novo_status = request.form['status']
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute("UPDATE pedido SET status = %s WHERE id_pedido = %s", (novo_status, id_pedido))
        conn.commit()
        cur.close()
        conn.close()
        return redirect(url_for('painel_admin', sucesso=f"Pedido #{id_pedido} alterado!"))
    return redirect(url_for('tela_login'))

@app.route('/cadastrar_equipamento', methods=['POST'])
def cadastrar_equipamento():
    if 'id_usuario' in session and session['tipo_usuario'] in ['admin', 'auxiliar']:
        nome = request.form['nome']
        status = request.form['status']
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute('INSERT INTO equipamento (nome, status) VALUES (%s, %s)', (nome, status))
        conn.commit()
        cur.close()
        conn.close()
        return redirect(url_for('painel_admin', sucesso="Equipamento cadastrado!"))
    return redirect(url_for('tela_login'))

@app.route('/atualizar_equipamento', methods=['POST'])
def atualizar_equipamento():
    if 'id_usuario' in session and session['tipo_usuario'] in ['admin', 'auxiliar']:
        id_equipamento = request.form['id_equipamento']
        novo_status = request.form['status']
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute("UPDATE equipamento SET status = %s WHERE id_equipamento = %s", (novo_status, id_equipamento))
        conn.commit()
        cur.close()
        conn.close()
        return redirect(url_for('painel_admin', sucesso="Status alterado!"))
    return redirect(url_for('tela_login'))

# ----- ÁREA DO CLIENTE COM CARRINHO E AJAX -----
@app.route('/painel_cliente')
def painel_cliente():
    if 'id_usuario' in session:
        sucesso = request.args.get('sucesso')
        if 'carrinho' not in session: session['carrinho'] = []
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute('SELECT id_produto, nome, categoria, quantidade_estoque, preco, imagem_url, variacao FROM produto WHERE quantidade_estoque > 0 ORDER BY nome ASC')
        produtos = [{'id': p[0], 'nome': p[1], 'categoria': p[2], 'estoque': p[3], 'preco': p[4], 'imagem': p[5], 'variacoes': [v.strip() for v in p[6].split(',')] if p[6] else []} for p in cur.fetchall()]
        cur.execute('''
            SELECT p.id_pedido, p.data_pedido, p.status, e.nome, string_agg(prod.nome || ' (' || COALESCE(ip.variacao_escolhida, 'Padrão') || ') - ' || ip.quantidade || 'x', ', ') as itens, p.endereco_entrega
            FROM pedido p JOIN item_pedido ip ON p.id_pedido = ip.id_pedido JOIN produto prod ON ip.id_produto = prod.id_produto LEFT JOIN equipamento e ON p.id_equipamento = e.id_equipamento
            WHERE p.id_usuario = %s GROUP BY p.id_pedido, p.data_pedido, p.status, e.nome, p.endereco_entrega ORDER BY p.data_pedido DESC
        ''', (session['id_usuario'],))
        meus_pedidos = cur.fetchall()
        cur.close()
        conn.close()
        return render_template('cliente.html', nome=session['nome'], produtos=produtos, meus_pedidos=meus_pedidos, sucesso=sucesso)
    return redirect(url_for('tela_login'))

@app.route('/adicionar_carrinho_ajax', methods=['POST'])
def adicionar_carrinho_ajax():
    if 'id_usuario' not in session: return jsonify({'status': 'erro'})
    id_produto, quantidade, variacao, nome_produto, preco_produto = int(request.form['id_produto']), int(request.form['quantidade']), request.form.get('variacao_escolhida', 'Padrão'), request.form['nome_produto'], float(request.form['preco_produto'])
    if 'carrinho' not in session: session['carrinho'] = []
    carrinho = session['carrinho']
    item_existe = False
    for item in carrinho:
        if item['id_produto'] == id_produto and item['variacao'] == variacao:
            item['quantidade'] += quantidade
            item_existe = True
            break
    if not item_existe: carrinho.append({'id_produto': id_produto, 'nome': nome_produto, 'preco': preco_produto, 'quantidade': quantidade, 'variacao': variacao})
    session['carrinho'] = carrinho
    session.modified = True
    return jsonify({'status': 'ok', 'nome_produto': nome_produto})

@app.route('/checkout')
def checkout():
    if 'id_usuario' not in session: return redirect(url_for('tela_login'))
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM equipamento WHERE status = 'disponivel'")
    equipamentos = cur.fetchall()
    cur.close()
    conn.close()
    return render_template('checkout.html', carrinho=session.get('carrinho', []), equipamentos=equipamentos)

@app.route('/finalizar_pedido', methods=['POST'])
def finalizar_pedido():
    if 'id_usuario' not in session or not session.get('carrinho'): return redirect(url_for('painel_cliente'))
    id_eq = request.form.get('id_equipamento')
    endereco = request.form.get('endereco_entrega', '').strip() if request.form.get('tipo_entrega') == 'entrega' else 'Retirada'
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute("INSERT INTO pedido (id_usuario, endereco_entrega, status, id_equipamento) VALUES (%s, %s, %s, %s) RETURNING id_pedido", (session['id_usuario'], endereco, 'pendente', int(id_eq) if id_eq else None))
        id_pedido = cur.fetchone()[0]
        if id_eq: cur.execute("UPDATE equipamento SET status = 'em_uso' WHERE id_equipamento = %s", (id_eq,))
        for item in session['carrinho']:
            cur.execute("INSERT INTO item_pedido (id_pedido, id_produto, quantidade, variacao_escolhida) VALUES (%s, %s, %s, %s)", (id_pedido, item['id_produto'], item['quantidade'], item['variacao']))
            cur.execute("UPDATE produto SET quantidade_estoque = quantidade_estoque - %s WHERE id_produto = %s RETURNING quantidade_estoque", (item['quantidade'], item['id_produto']))
            atualizar_estoque_google(item['nome'], cur.fetchone()[0])
        conn.commit()
        session['carrinho'] = [] 
        session.modified = True
        return redirect(url_for('painel_cliente', sucesso="Pedido gerado!"))
    except Exception as e:
        conn.rollback()
        return redirect(url_for('painel_cliente', erro=f"Erro: {str(e)}"))
    finally:
        cur.close()
        conn.close()

# ----- GESTÃO DE CONTAS (EXCLUSIVO ADMIN) -----
@app.route('/promover_usuario/<int:id_usuario>/<string:cargo>', methods=['POST'])
def promover_usuario(id_usuario, cargo):
    if 'id_usuario' in session and session['tipo_usuario'] == 'admin' and cargo in ['admin', 'auxiliar', 'cliente']:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute("UPDATE usuario SET tipo_usuario = %s WHERE id_usuario = %s", (cargo, id_usuario))
        conn.commit()
        cur.close()
        conn.close()
        return redirect(url_for('painel_admin', sucesso="Cargo do usuário atualizado!"))
    return redirect(url_for('painel_admin'))

@app.route('/deletar_usuario/<int:id_usuario>', methods=['POST'])
def deletar_usuario(id_usuario):
    if 'id_usuario' in session and session['tipo_usuario'] == 'admin':
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute("DELETE FROM item_pedido WHERE id_pedido IN (SELECT id_pedido FROM pedido WHERE id_usuario = %s)", (id_usuario,))
        cur.execute("DELETE FROM pedido WHERE id_usuario = %s", (id_usuario,))
        cur.execute("DELETE FROM usuario WHERE id_usuario = %s", (id_usuario,))
        conn.commit()
        cur.close()
        conn.close()
        return redirect(url_for('painel_admin', sucesso="Usuário excluído!"))
    return redirect(url_for('painel_admin'))

@app.route('/logout')
def logout():
    session.clear()
    return redirect(url_for('tela_login'))

if __name__ == '__main__':
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port)