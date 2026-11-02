<script setup>
import { ref, onMounted } from 'vue'

const API_URL = 'http://localhost:8080/alunos'

const alunos = ref([])
const mensagem = ref('')
const erro = ref('')

const novoAluno = ref({
  plano: 'BASICO',
  possuiVoucher: false,
  cursosConcluidos: 0,
  cursosAdicionaisPermitidos: 0,
  notificadoMelhoriaPlano: false
})

async function listarAlunos() {
  try {
    erro.value = ''

    const resposta = await fetch(API_URL)

    if (!resposta.ok) {
      throw new Error('Erro ao buscar alunos')
    }

    alunos.value = await resposta.json()
  } catch (e) {
    erro.value = 'Não foi possível conectar com o servidor.'
  }
}

async function criarAluno() {
  try {
    mensagem.value = ''
    erro.value = ''

    const resposta = await fetch(API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(novoAluno.value)
    })

    if (!resposta.ok) {
      throw new Error('Erro ao criar aluno')
    }

    mensagem.value = 'Aluno cadastrado com sucesso!'

    novoAluno.value = {
      plano: 'BASICO',
      possuiVoucher: false,
      cursosConcluidos: 0,
      cursosAdicionaisPermitidos: 0,
      notificadoMelhoriaPlano: false
    }

    await listarAlunos()
  } catch (e) {
    erro.value = 'Não foi possível cadastrar o aluno.'
  }
}

async function concluirCurso(id) {
  try {
    mensagem.value = ''
    erro.value = ''

    const resposta = await fetch(`${API_URL}/${id}/concluir-curso`, {
      method: 'PUT'
    })

    if (!resposta.ok) {
      throw new Error('Erro ao concluir curso')
    }

    mensagem.value = 'Curso concluído com sucesso!'

    await listarAlunos()
  } catch (e) {
    erro.value = 'Não foi possível concluir o curso.'
  }
}

async function liberarCursos(id) {
  try {
    mensagem.value = ''
    erro.value = ''

    // B1 / ADR-002: a regra "média >= 7,0" agora é aplicada pelo domínio no backend
    const media = window.prompt('Média do aluno no curso (0 a 10):', '7.0')
    if (media === null) {
      return
    }

    const mediaNormalizada = encodeURIComponent(media.replace(',', '.'))
    const resposta = await fetch(`${API_URL}/${id}/liberar-cursos?media=${mediaNormalizada}`, {
      method: 'PUT'
    })

    if (!resposta.ok) {
      throw new Error('Erro ao liberar cursos')
    }

    const aluno = await resposta.json()
    mensagem.value = aluno.cursosAdicionaisPermitidos === 3
      ? '3 cursos adicionais liberados!'
      : 'Média abaixo de 7,0: cursos adicionais não liberados.'

    await listarAlunos()
  } catch (e) {
    erro.value = 'Não foi possível liberar os cursos.'
  }
}

async function notificar(id) {
  try {
    mensagem.value = ''
    erro.value = ''

    const resposta = await fetch(`${API_URL}/${id}/notificar`, {
      method: 'PUT'
    })

    if (!resposta.ok) {
      throw new Error('Erro ao enviar notificação')
    }

    mensagem.value = 'Notificação de mudança de plano registrada!'

    await listarAlunos()
  } catch (e) {
    erro.value = 'Não foi possível registrar a notificação.'
  }
}

async function deletarAluno(id) {
  try {
    mensagem.value = ''
    erro.value = ''

    const resposta = await fetch(`${API_URL}/${id}`, {
      method: 'DELETE'
    })

    if (!resposta.ok) {
      throw new Error('Erro ao excluir aluno')
    }

    mensagem.value = 'Aluno excluído com sucesso!'

    await listarAlunos()
  } catch (e) {
    erro.value = 'Não foi possível excluir o aluno.'
  }
}

onMounted(() => {
  listarAlunos()
})
</script>

<template>
  <div class="container">

    <header>
      <h1>Educação Continuada Gamificada</h1>
      <p>Gerenciamento de alunos</p>
    </header>

    <!-- CADASTRO -->
    <section class="card">
      <h2>Cadastrar aluno</h2>

      <form @submit.prevent="criarAluno">

        <div class="campo">
          <label>Plano</label>

          <select v-model="novoAluno.plano">
            <option value="BASICO">BASICO</option>
            <option value="PREMIUM">PREMIUM</option>
          </select>
        </div>

        <div class="campo">
          <label>Cursos concluídos</label>

          <input
            v-model.number="novoAluno.cursosConcluidos"
            type="number"
            min="0"
          />
        </div>

        <div class="campo">
          <label>Cursos adicionais permitidos</label>

          <input
            v-model.number="novoAluno.cursosAdicionaisPermitidos"
            type="number"
            min="0"
          />
        </div>

        <button type="submit">
          Cadastrar aluno
        </button>

      </form>
    </section>

    <!-- MENSAGENS -->
    <p v-if="mensagem" class="sucesso">
      {{ mensagem }}
    </p>

    <p v-if="erro" class="erro">
      {{ erro }}
    </p>

    <!-- LISTA -->
    <section class="card">

      <div class="titulo-lista">
        <h2>Alunos cadastrados</h2>

        <button @click="listarAlunos">
          Atualizar
        </button>
      </div>

      <div v-if="alunos.length === 0" class="vazio">
        Nenhum aluno cadastrado.
      </div>

      <div
        v-for="aluno in alunos"
        :key="aluno.id"
        class="aluno"
      >

        <div class="informacoes">

          <h3>Aluno #{{ aluno.id }}</h3>

          <p>
            <strong>Plano:</strong>

            <span
              :class="aluno.plano === 'PREMIUM' ? 'premium' : 'basico'"
            >
              {{ aluno.plano }}
            </span>
          </p>

          <p>
            <strong>Cursos concluídos:</strong>
            {{ aluno.cursosConcluidos }}
          </p>

          <p>
            <strong>Cursos adicionais permitidos:</strong>
            {{ aluno.cursosAdicionaisPermitidos }}
          </p>

          <p>
            <strong>Voucher:</strong>

            <span v-if="aluno.possuiVoucher" class="positivo">
              Sim 🎟️
            </span>

            <span v-else>
              Não
            </span>
          </p>

          <p>
            <strong>Notificação:</strong>

            <span
              v-if="aluno.notificadoMelhoriaPlano"
              class="positivo"
            >
              Sim 🔔
            </span>

            <span v-else>
              Não
            </span>
          </p>

          <!-- AÇÕES -->
          <div class="acoes">

            <button
              class="botao-curso"
              @click="concluirCurso(aluno.id)"
            >
              ➕ Concluir curso
            </button>

            <button
              class="botao-extra"
              @click="liberarCursos(aluno.id)"
            >
              🎓 Liberar 3 cursos
            </button>

            <button
              v-if="aluno.plano === 'PREMIUM'"
              class="botao-notificacao"
              @click="notificar(aluno.id)"
            >
              🔔 Notificar mudança
            </button>

            <button
              class="excluir"
              @click="deletarAluno(aluno.id)"
            >
              🗑️ Excluir
            </button>

          </div>

        </div>

      </div>

    </section>

  </div>
</template>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: Arial, sans-serif;
  background: #121212;
  color: #f1f1f1;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
}

header {
  margin-bottom: 30px;
}

header h1 {
  margin-bottom: 8px;
  color: #ffffff;
}

header p {
  color: #bdbdbd;
}

.card {
  background: #1e1e1e;
  padding: 25px;
  border-radius: 10px;
  margin-bottom: 25px;
  border: 1px solid #333;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.card h2 {
  color: #ffffff;
  margin-top: 0;
}

.campo {
  display: flex;
  flex-direction: column;
  margin-bottom: 15px;
}

.campo label {
  margin-bottom: 6px;
  font-weight: bold;
  color: #e0e0e0;
}

.campo input,
.campo select {
  padding: 10px;
  border: 1px solid #444;
  border-radius: 5px;
  background: #2a2a2a;
  color: #ffffff;
  font-size: 14px;
}

.campo input:focus,
.campo select:focus {
  outline: none;
  border-color: #777;
}

button {
  border: none;
  border-radius: 5px;
  padding: 10px 16px;
  cursor: pointer;
  font-weight: bold;
  background: #eeeeee;
  color: #121212;
}

button:hover {
  background: #ffffff;
}

.titulo-lista {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.aluno {
  border-top: 1px solid #333;
  padding: 25px 0;
}

.aluno h3 {
  margin-top: 0;
  color: #ffffff;
}

.aluno p {
  margin: 8px 0;
  color: #d0d0d0;
}

.aluno strong {
  color: #ffffff;
}

.acoes {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.acoes button {
  background: #eeeeee;
  color: #121212;
}

.acoes button:hover {
  background: #ffffff;
}

.premium {
  color: #70d670;
  font-weight: bold;
}

.basico {
  color: #d0d0d0;
  font-weight: bold;
}

.positivo {
  color: #70d670;
  font-weight: bold;
}

.excluir {
  background: #b83b3b !important;
  color: #ffffff !important;
}

.excluir:hover {
  background: #d14a4a !important;
}

.botao-curso {
  background: #eeeeee;
}

.botao-extra {
  background: #eeeeee;
}

.botao-notificacao {
  background: #eeeeee;
}

.sucesso {
  color: #70d670;
  font-weight: bold;
  margin: 15px 0;
}

.erro {
  color: #ff6b6b;
  font-weight: bold;
  margin: 15px 0;
}

.vazio {
  color: #999999;
  padding: 15px 0;
}
</style>