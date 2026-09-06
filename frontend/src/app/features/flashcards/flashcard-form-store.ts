import { computed, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CatalogoApi } from '../../core/api/catalogo-api';
import { extrairErroApi } from '../../core/api/erro-api';
import { FlashcardApi } from '../../core/api/flashcard-api';
import {
  ContextoQuestaoFlashcard,
  FlashcardResumo,
  RequisicaoFlashcard,
  RespostaFlashcardCriado,
} from '../../core/api/modelos-flashcard';
import { DisciplinaArvore } from '../../core/api/modelos-questao';
import { gravarRascunho, lerRascunho, limparRascunho } from './rascunho-flashcard';

const TAMANHO_MINIMO_TEXTO = 3;
const LIMITE_TAGS = 10;
type ErroCampoFrenteVerso = Record<'frente' | 'verso', string | null>;

@Injectable()
export class FlashcardFormStore {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(FlashcardApi);
  private readonly catalogo = inject(CatalogoApi);
  readonly form = this.fb.group({
    frente: this.fb.nonNullable.control(''),
    verso: this.fb.nonNullable.control(''),
    tags: this.fb.nonNullable.control(''),
    deckId: this.fb.control<string | null>(null),
    disciplinaId: this.fb.control<string | null>(null),
    assuntoId: this.fb.control<string | null>(null),
  });
  readonly contexto = signal<ContextoQuestaoFlashcard | null>(null);
  readonly editando = signal<FlashcardResumo | null>(null);
  readonly modoEdicao = computed(() => this.editando() !== null);
  readonly modoAvulso = computed(() =>
    this.editando() ? this.editando()!.origem === null : this.contexto() === null,
  );
  readonly rotuloContexto = computed(() => {
    const c = this.contexto();
    return c ? [c.codigo, c.disciplinaNome, c.assuntoNome].filter(Boolean).join(' · ') : '';
  });
  readonly nomeDisciplinaLeitura = computed(
    () => this.editando()?.disciplina?.nome ?? this.contexto()?.disciplinaNome ?? '—',
  );
  readonly nomeAssuntoLeitura = computed(
    () => this.editando()?.assunto?.nome ?? this.contexto()?.assuntoNome ?? '—',
  );
  readonly erroDeCampo = signal<ErroCampoFrenteVerso>({ frente: null, verso: null });
  readonly erroGeral = signal<string | null>(null);
  readonly confirmandoDescarte = signal(false);
  readonly enviando = signal(false);
  readonly ultimoCriado = signal<RespostaFlashcardCriado | null>(null);
  readonly criadosNaSessao = computed(() => this.ultimoCriado()?.criadosNaSessao ?? null);
  readonly disciplinas = signal<DisciplinaArvore[]>([]);
  private readonly frenteValor = toSignal(this.form.controls.frente.valueChanges, {
    initialValue: '',
  });
  private readonly versoValor = toSignal(this.form.controls.verso.valueChanges, {
    initialValue: '',
  });
  readonly sujo = computed(() => {
    const f = this.frenteValor().trim();
    const v = this.versoValor().trim();
    const ed = this.editando();
    return ed ? f !== ed.frente.trim() || v !== ed.verso.trim() : f.length > 0 || v.length > 0;
  });
  readonly deckId = toSignal(this.form.controls.deckId.valueChanges, { initialValue: null });
  private readonly disciplinaSelecionada = toSignal(this.form.controls.disciplinaId.valueChanges, {
    initialValue: null,
  });
  readonly assuntos = computed(
    () => this.disciplinas().find((d) => d.id === this.disciplinaSelecionada())?.assuntos ?? [],
  );
  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe((v) => {
      const c = this.contexto();
      if (c) gravarRascunho(c.questaoId, { frente: v.frente ?? '', verso: v.verso ?? '' });
    });
  }
  abrir(contexto: ContextoQuestaoFlashcard | null): void {
    this.contexto.set(contexto);
    this.editando.set(null);
    this.resetarEstado();
    const rascunho = contexto ? lerRascunho(contexto.questaoId) : null;
    this.form.reset({
      frente: rascunho?.frente ?? '',
      verso: rascunho?.verso ?? '',
      deckId: null,
      disciplinaId: null,
      assuntoId: null,
      tags: '',
    });
    if (!contexto) this.carregarDisciplinas();
  }
  abrirEdicao(flashcard: FlashcardResumo): void {
    this.contexto.set(null);
    this.editando.set(flashcard);
    this.resetarEstado();
    this.form.reset({
      frente: flashcard.frente,
      verso: flashcard.verso,
      deckId: flashcard.deck?.id ?? null,
      disciplinaId: flashcard.disciplina?.id ?? null,
      assuntoId: flashcard.assunto?.id ?? null,
      tags: flashcard.tags.join(', '),
    });
    if (flashcard.origem === null) this.carregarDisciplinas();
  }
  aoTrocarDisciplina(): void {
    this.form.controls.assuntoId.setValue(null);
  }
  async salvarEFechar(): Promise<boolean> {
    return (await this.enviar()) !== null;
  }
  async salvarECriarOutro(): Promise<boolean> {
    const resposta = await this.enviar();
    if (!resposta) return false;
    this.form.patchValue({ frente: '', verso: '' });
    return true;
  }
  cancelar(): boolean {
    if (!this.sujo()) {
      this.descartarRascunho();
      return true;
    }
    this.confirmandoDescarte.set(true);
    return false;
  }
  continuarEditando(): void {
    this.confirmandoDescarte.set(false);
  }
  confirmarDescarte(): void {
    this.descartarRascunho();
  }
  private resetarEstado(): void {
    this.ultimoCriado.set(null);
    this.confirmandoDescarte.set(false);
    this.erroGeral.set(null);
    this.erroDeCampo.set({ frente: null, verso: null });
  }
  private descartarRascunho(): void {
    const c = this.contexto();
    if (c) limparRascunho(c.questaoId);
    this.confirmandoDescarte.set(false);
  }
  private corpo(erro: unknown): unknown {
    return erro && typeof erro === 'object' && 'error' in erro
      ? (erro as { error: unknown }).error
      : erro;
  }
  private async enviar(): Promise<RespostaFlashcardCriado | null> {
    if (!this.validar()) return null;
    this.enviando.set(true);
    try {
      const resposta = await this.chamarApi();
      this.ultimoCriado.set(resposta);
      this.erroGeral.set(null);
      this.erroDeCampo.set({ frente: null, verso: null });
      const contexto = this.contexto();
      if (contexto) limparRascunho(contexto.questaoId);
      return resposta;
    } catch (erro) {
      this.aplicarErro(erro);
      return null;
    } finally {
      this.enviando.set(false);
    }
  }
  private async chamarApi(): Promise<RespostaFlashcardCriado> {
    const editando = this.editando();
    if (!editando) return firstValueFrom(this.api.criar(this.dados()));
    return {
      flashcard: await firstValueFrom(this.api.atualizar(editando.id, this.dados())),
      criadosNaSessao: null,
    };
  }
  private validar(): boolean {
    const frente = this.form.controls.frente.value.trim();
    const verso = this.form.controls.verso.value.trim();
    const erros: ErroCampoFrenteVerso = {
      frente:
        frente.length < TAMANHO_MINIMO_TEXTO ? 'A frente deve ter ao menos 3 caracteres.' : null,
      verso: verso.length < TAMANHO_MINIMO_TEXTO ? 'O verso deve ter ao menos 3 caracteres.' : null,
    };
    this.erroDeCampo.set(erros);
    return erros.frente === null && erros.verso === null;
  }
  private dados(): RequisicaoFlashcard {
    const v = this.form.getRawValue();
    const contexto = this.contexto();
    const origem = this.editando()?.origem ?? null;
    const avulso = this.modoAvulso();
    return {
      frente: v.frente.trim(),
      verso: v.verso.trim(),
      deckId: v.deckId,
      disciplinaId: avulso ? v.disciplinaId : null,
      assuntoId: avulso ? v.assuntoId : null,
      tags: this.tagsDoTexto(v.tags),
      questaoOrigemId: origem?.questaoId ?? contexto?.questaoId ?? null,
      sessaoOrigemId: origem?.sessaoId ?? contexto?.sessaoId ?? null,
    };
  }
  private tagsDoTexto(texto: string): string[] {
    return texto
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)
      .slice(0, LIMITE_TAGS);
  }
  private aplicarErro(erro: unknown): void {
    const api = extrairErroApi(this.corpo(erro));
    const campos = api.fieldErrors ?? {};
    const doCampo = campos['frente'] || campos['verso'];
    this.erroDeCampo.set({ frente: campos['frente'] ?? null, verso: campos['verso'] ?? null });
    this.erroGeral.set(doCampo ? null : api.message);
  }
  private async carregarDisciplinas(): Promise<void> {
    try {
      this.disciplinas.set(await firstValueFrom(this.catalogo.disciplinas()));
    } catch {
      this.disciplinas.set([]);
    }
  }
}
