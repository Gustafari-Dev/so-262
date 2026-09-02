# Equipe:
# Caio Macilon
# Gabriel Lopes
# Gustavo Araújo

# Especificação de Projeto: Gerenciador de Processos para Simulador de SO

## Informações Administrativas e Diretrizes de Entrega
- **Formato de Trabalho:** Equipe (Máximo de 3 componentes).
- **Formato de Entrega:** Arquivo Markdown (`.md`).
- **Repositório:** Cada componente da equipe deve obrigatoriamente postar este documento em seu respectivo repositório pessoal no GitHub.
- **Propósito do Documento:** Esta especificação servirá como *prompt* (entrada de contexto) para a criação do código-fonte do simulador utilizando um Harness de IA (ex: Claude Code, Open Code). Por isso, as regras de negócio, estruturas de dados e fluxos estão detalhados tecnicamente.

---

## Parte 1: Visão Geral e Arquitetura do Simulador

### 1.1 Contexto do Simulador
O sistema a ser desenvolvido é um **Simulador de Gerenciamento de Processos** que opera inteiramente em modo usuário. Ele não interagirá diretamente com o hardware real da máquina hospedeira, mas simulará o comportamento interno de um núcleo (Kernel) de Sistema Operacional (SO), focando exclusivamente no subsistema de processos.

### 1.2 Estrutura do Hardware Simulado
O simulador deve instanciar uma abstração de hardware contendo os seguintes componentes virtuais:
- **CPU Virtual:** Responsável por "executar" os processos (contabilizar o tempo).
- **Registradores Básicos:**
  - `R0, R1, R2, R3`: Registradores de propósito geral (armazenam valores inteiros fictícios).
  - `PC (Program Counter)`: Aponta para a próxima instrução simulada do processo.
  - `SP (Stack Pointer)`: Aponta para o topo da pilha virtual do processo.
- **Relógio Lógico (Clock):** Uma variável global (ex: `system_clock`) que incrementa em unidades inteiras (ticks). Toda a passagem de tempo no simulador será medida com base nestes ticks (ex: 1 tick = 1 unidade de tempo de CPU).

### 1.3 Fluxo Geral de Execução
O simulador iniciará lendo um arquivo de lote de tarefas (Task File). Em seguida, entrará no **Loop Principal do Sistema (Main Loop)**, que a cada tick do relógio:
1. Verifica se novos processos chegaram no tempo atual.
2. Atualiza o status de processos bloqueados em E/S.
3. Invoca o Escalonador de CPU para decidir qual processo deve executar.
4. Simula a execução do processo escolhido (incrementa seu tempo de CPU).
5. Incrementa o relógio lógico do sistema.

---

## Parte 2: Bloco de Controle de Processo (PCB) e Tabela de Processos

### 2.1 Estrutura do PCB (Process Control Block)
O PCB é a estrutura de dados central do gerenciador. Para que a IA geradora de código compreenda os tipos de dados, a estrutura do PCB deve conter, no mínimo, os seguintes campos:

```text
ESTRUTURA PCB:
- PID (int): Identificador único do processo (gerado sequencialmente a partir de 1).
- PPID (int): Identificador do processo pai (0 se for o processo raiz/init).
- State (enum): Estado atual (NEW, READY, RUNNING, BLOCKED, TERMINATED).
- Priority (int): Prioridade base do processo (ex: 0 a 10, onde 0 é a maior prioridade).
- Dynamic_Priority (int): Prioridade atualizada dinamicamente para evitar inanição.
- Registers (dict/struct): Cópia dos registradores (R0-R3, PC, SP) salvos na troca de contexto.

Métricas de Tempo (Inteiros representando Ticks):
- Arrival_Time: Momento (tick do relógio) em que o processo foi submetido.
- Total_CPU_Burst: Tempo total de CPU necessário para o processo finalizar.
- Executed_Time: Tempo de CPU já consumido pelo processo.
- IO_Burst_List: Fila/Lista de interrupções de E/S programadas (ex: realizar E/S no tick 5 de execução).
- Waiting_Time: Tempo total que o processo passou no estado READY.
- Turnaround_Time: Tempo total desde a chegada até a finalização.
```

### 2.2 Tabela de Processos
A Tabela de Processos é a estrutura do Kernel que armazena todos os PCBs instanciados.
- **Implementação:** Deve ser implementada como uma Tabela Hash (Dicionário) ou um Array estático indexado pelo `PID`.
- **Capacidade Máxima (MAX_PROCESSES):** O simulador deve suportar uma configuração de limite máximo de processos simultâneos (ex: capacidade para 50 processos ativos). Tentativas de criar novos processos com a tabela cheia devem gerar erro de *Overhead*.

---

## Parte 3: Ciclo de Vida e Grafo de Transição de Estados

O ciclo de vida de um processo no simulador é regido por uma Máquina de Estados Finitos baseada no modelo de 5 estados (com foco nas transições entre os 3 estados ativos: Pronto, Em Execução e Bloqueado).

### 3.1 Estados
1. **NEW (Novo):** Processo recém-criado (simulação de `fork()`). O PCB foi alocado, mas o processo ainda não foi admitido na fila de prontos.
2. **READY (Pronto):** Processo carregado na memória principal, aguardando a CPU ser atribuída a ele pelo escalonador.
3. **RUNNING (Em Execução):** Processo que possui a posse da CPU e cujas instruções estão sendo simuladas.
4. **BLOCKED (Bloqueado):** Processo que não pode executar pois aguarda um evento externo (simulação de conclusão de E/S).
5. **TERMINATED (Terminado):** Processo concluiu sua execução (`exit()`) ou foi abortado. O PCB aguarda para ser desalocado.

### 3.2 Gatilhos e Transições de Estado
A lógica do código gerado deve implementar as seguintes transições rigorosamente:
- **NEW → READY (Admissão):** Ocorre no tick em que `Arrival_Time == system_clock`.
- **READY → RUNNING (Despacho):** Acontece quando a CPU está ociosa e o Escalonador escolhe um processo da Fila de Prontos (`ready_queue`). Envolve a restauração de contexto (carregar registradores do PCB).
- **RUNNING → READY (Preempção por Relógio):** Ocorre quando o *Quantum* de tempo limite do processo expira antes dele terminar seu surto de CPU. Envolve salvar o contexto no PCB.
- **RUNNING → BLOCKED (Solicitação de E/S):** Ocorre se o processo atinge um momento de execução onde há uma E/S programada na sua `IO_Burst_List`.
- **BLOCKED → READY (Conclusão de E/S):** Ocorre quando o tempo de duração da requisição de E/S é concluído. O processo volta imediatamente para a fila de prontos.
- **RUNNING → TERMINATED (Término):** Ocorre quando `Executed_Time == Total_CPU_Burst`. O processo invoca a chamada fictícia `exit()`.

---

## Parte 4: Especificação do Escalonador de CPU

O simulador deve ser flexível, permitindo que o usuário escolha (via argumento de linha de comando ou arquivo de configuração) qual algoritmo de escalonamento será utilizado. A arquitetura deve prever a implementação de **dois algoritmos obrigatórios**:

### 4.1 Algoritmo 1: Circular (Round Robin - RR)
- **Fila de Prontos:** Gerenciada estritamente no formato FIFO (First In, First Out).
- **Parâmetro Quantum ($q$):** O tempo máximo contínuo que um processo pode ficar na CPU (ex: 4 ticks).
- **Regra:** Se o processo utilizar a CPU por $q$ ticks ininterruptos, ele sofre preempção e é enviado para o final da Fila de Prontos.
- **Troca de Contexto ($c$):** O simulador deve contabilizar 1 tick de overhead para salvar e restaurar o contexto a cada preempção.

### 4.2 Algoritmo 2: Prioridades Dinâmicas com Prevenção de Inanição (Aging)
- **Filas Múltiplas:** Uma lista de filas de prontos ordenadas por prioridade.
- **Escalonamento:** O escalonador sempre retira o processo do início da fila de maior prioridade (onde 0 é a maior e 10 é a menor). O algoritmo pode ser preemptivo (se chegar um processo de maior prioridade, o atual perde a CPU).
- **Mecanismo de Aging (Envelhecimento):** Para resolver o problema da Inanição (*Starvation*) — processos de baixa prioridade que nunca recebem a CPU —, o simulador deve implementar uma função periódica:
  - A cada $N$ ticks consecutivos que um processo passar na Fila de Prontos (Waiting_Time acumulado), sua `Dynamic_Priority` é melhorada em 1 nível (ex: de 5 para 4).

---

## Parte 5: Entradas, Casos de Teste e Diretrizes de Saída

Para facilitar o *parsing* e garantir a repetibilidade das simulações, o sistema deverá operar através de leitura de arquivos e geração de logs textuais detalhados.

### 5.1 Formato do Arquivo de Entrada (Task File)
O simulador deverá ler um arquivo de texto estruturado (sugere-se CSV) descrevendo os processos a serem criados.
**Layout das colunas:**
`PID, Tempo_Chegada, Prioridade_Base, Tempo_CPU_Total, Instantes_ES, Duracao_ES`

**Exemplo de linha do arquivo (`tasks.csv`):**
```csv
1, 0, 3, 10, [3, 7], [2, 1]
# Processo 1 chega no tick 0, prioridade 3, precisa de 10 ticks de CPU.
# Pausa para E/S no seu 3º tick executado (duração de 2 ticks) e no 7º tick (duração 1 tick).
2, 2, 1, 5, [], []
# Processo 2 chega no tick 2, prioridade 1, precisa de 5 ticks de CPU. Nenhuma E/S.
```

### 5.2 Saídas do Simulador
A IA geradora de código deverá implementar um módulo de Relatórios (Logger) que gerará três tipos de saída ao final da simulação:

#### A. Log de Transições de Estado (Console ou Arquivo de Log)
A cada tick onde houver alteração de estado, imprimir:
`[Tick 04] Processo 2 criado (NEW -> READY)`
`[Tick 05] Processo 1 solicitou E/S (RUNNING -> BLOCKED)`
`[Tick 05] Processo 2 assumiu a CPU (READY -> RUNNING)`

#### B. Gráfico de Gantt Textual
Uma linha do tempo impressa no terminal representando o uso da CPU e o tempo ocioso (Idle).
`[Tempo 00-03]: PID 1`
`[Tempo 03-05]: PID 1` (P1 continua se Quantum > 2)
`[Tempo 05-10]: PID 2`
*Alternativa em formato fita:* `| P1 | P1 | P1 | P2 | P2 | P2 | IDLE | P1 |`

#### C. Relatório Estatístico Final
Ao término de todos os processos da fila, exibir:
- **Estatísticas por Processo:** PID, Tempo de Chegada, Tempo de Término, Turnaround Total, Tempo Total de Espera.
- **Estatísticas do Sistema:**
  - Tempo Médio de Espera (Average Waiting Time).
  - Tempo Médio de Retorno (Average Turnaround Time).
  - Utilização da CPU (%) = `(Tempo de CPU Ocupada / Tempo Total do Sistema) * 100`.
  - Número de Trocas de Contexto Realizadas.
