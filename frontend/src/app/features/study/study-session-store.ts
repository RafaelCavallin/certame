import { computed, Injectable, signal } from '@angular/core';
import {
  ModoSessao,
  QuestaoDaSerie,
  RespostaRegistrada,
  RespostaSessao,
} from '../../core/api/modelos-estudo';

@Injectable({ providedIn: 'root' })
export class StudySessionStore {
  readonly sessao = signal<RespostaSessao | null>(null);
  readonly indice = signal(0);
  readonly respostas = signal<RespostaRegistrada[]>([]);
  readonly alternativas = signal<Record<string, string>>({});
  readonly gabaritos = computed(
    () => new Map(this.respostas().map((resposta) => [resposta.questaoId, resposta])),
  );
  readonly questaoAtual = computed(() => this.sessao()?.questoes[this.indice()] ?? null);
  readonly modo = computed(() => this.sessao()?.modo ?? null);
  readonly emProva = computed(() => this.modo() === 'PROVA');
  iniciar(sessao: RespostaSessao): void {
    this.sessao.set(sessao);
    // indiceAtual aponta para a 1ª questão sem resposta; se todas já foram respondidas
    // (sessão retomada mas nunca finalizada), cai na última para não sair do intervalo válido.
    this.indice.set(Math.min(sessao.indiceAtual, Math.max(sessao.questoes.length - 1, 0)));
    this.respostas.set([...sessao.respostas]);
    this.alternativas.set(
      Object.fromEntries(
        sessao.respostas.map((resposta) => [resposta.questaoId, resposta.marcada]),
      ),
    );
  }
  limpar(): void {
    this.sessao.set(null);
    this.indice.set(0);
    this.respostas.set([]);
    this.alternativas.set({});
  }
  irPara(indice: number): void {
    if (indice >= 0 && indice < (this.sessao()?.questoes.length ?? 0)) this.indice.set(indice);
  }
  proxima(): void {
    this.irPara(this.indice() + 1);
  }
  anterior(): void {
    this.irPara(this.indice() - 1);
  }
  respostaDa(questaoId: string): RespostaRegistrada | undefined {
    return this.gabaritos().get(questaoId);
  }
  alternativaMarcada(questaoId: string): string | null {
    return this.alternativas()[questaoId] ?? null;
  }
  marcarResposta(resposta: RespostaRegistrada): void {
    this.respostas.update((atuais) => [
      ...atuais.filter((item) => item.questaoId !== resposta.questaoId),
      resposta,
    ]);
    this.alternativas.update((atuais) => ({ ...atuais, [resposta.questaoId]: resposta.marcada }));
  }
  marcarAlternativa(questaoId: string, marcada: string): void {
    this.alternativas.update((atuais) => ({ ...atuais, [questaoId]: marcada }));
    if (!this.emProva()) return;
    const atual = this.respostaDa(questaoId);
    this.respostas.update((atuais) => [
      ...atuais.filter((item) => item.questaoId !== questaoId),
      {
        ...(atual ?? ({} as RespostaRegistrada)),
        questaoId,
        marcada,
        correta: false,
        gabarito: '',
        comentario: null,
        estatistica: null,
        tempoMs: atual?.tempoMs ?? 0,
        respondidaEm: atual?.respondidaEm ?? '',
      },
    ]);
  }
  trocarModo(modo: ModoSessao): void {
    const sessao = this.sessao();
    if (sessao) this.sessao.set({ ...sessao, modo });
  }
  atualizarQuestao(questao: QuestaoDaSerie): void {
    const sessao = this.sessao();
    if (!sessao) return;
    this.sessao.set({
      ...sessao,
      questoes: sessao.questoes.map((item) => (item.id === questao.id ? questao : item)),
    });
  }
}
