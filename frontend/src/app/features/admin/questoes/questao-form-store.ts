import { computed, inject, Injectable, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { aplicarErrosDeCampo, extrairErroApi } from '../../../core/api/erro-api';
import { CatalogoApi } from '../../../core/api/catalogo-api';
import {
  Dificuldade,
  QuestaoDetalhe,
  RequisicaoQuestao,
  TipoQuestao,
} from '../../../core/api/modelos-questao';
import { QuestaoApi } from '../../../core/api/questao-api';

@Injectable()
export class QuestaoFormStore {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(QuestaoApi);
  private readonly catalogo = inject(CatalogoApi);
  readonly carregando = signal(false);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly pendenciasPublicacao = signal<Record<string, string> | null>(null);
  readonly questao = signal<QuestaoDetalhe | null>(null);
  readonly disciplinas = signal<import('../../../core/api/modelos-questao').DisciplinaArvore[]>([]);
  readonly bancas = signal<import('../../../core/api/modelos-questao').ItemCatalogo[]>([]);
  readonly orgaos = signal<import('../../../core/api/modelos-questao').ItemCatalogo[]>([]);
  readonly formulario = this.fb.group({
    tipo: this.fb.nonNullable.control<TipoQuestao>('ME'),
    enunciado: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.minLength(10),
      Validators.maxLength(20000),
    ]),
    disciplinaId: this.fb.control<string | null>(null),
    assuntoId: this.fb.control<string | null>(null),
    bancaId: this.fb.control<string | null>(null),
    orgaoId: this.fb.control<string | null>(null),
    ano: this.fb.control<number | null>(null),
    dificuldade: this.fb.control<Dificuldade | null>('MEDIA'),
    gabarito: this.fb.control<string | null>(null),
    comentario: this.fb.control<string | null>(null),
    alternativas: this.fb.array<FormGroup<{ texto: FormControl<string> }>>([]),
  });
  readonly alternativas = computed(() => this.formulario.controls.alternativas.controls);
  readonly editando = computed(() => this.questao() !== null);
  constructor() {
    this.adicionarAlternativa();
    this.adicionarAlternativa();
  }
  async carregar(id: string | null): Promise<void> {
    this.carregando.set(true);
    this.erro.set(null);
    this.pendenciasPublicacao.set(null);
    try {
      const [disciplinas, bancas, orgaos] = await Promise.all([
        firstValueFrom(this.catalogo.disciplinas()),
        firstValueFrom(this.catalogo.bancas()),
        firstValueFrom(this.catalogo.orgaos()),
      ]);
      this.disciplinas.set(disciplinas);
      this.bancas.set(bancas);
      this.orgaos.set(orgaos);
      if (id) this.aplicarDetalhe(await firstValueFrom(this.api.obter(id)));
    } catch (e) {
      this.erro.set(extrairErroApi(this.corpo(e)).message);
    } finally {
      this.carregando.set(false);
    }
  }
  adicionarAlternativa(): void {
    if (this.formulario.controls.alternativas.length < 5)
      this.formulario.controls.alternativas.push(this.novaAlternativa());
  }
  removerAlternativa(i: number): void {
    if (this.formulario.controls.alternativas.length > 2)
      this.formulario.controls.alternativas.removeAt(i);
  }
  trocarTipo(tipo: TipoQuestao): void {
    this.formulario.controls.tipo.setValue(tipo);
    this.formulario.controls.gabarito.setValue(null);
    if (tipo === 'CE') this.formulario.controls.alternativas.clear();
    else if (!this.formulario.controls.alternativas.length) {
      this.adicionarAlternativa();
      this.adicionarAlternativa();
    }
  }
  async salvarRascunho(): Promise<void> {
    await this.salvar();
  }
  async enviarParaRevisao(): Promise<void> {
    const id = this.questao()?.id;
    if (id) await this.transicao(() => this.api.enviarParaRevisao(id));
  }
  async publicar(): Promise<void> {
    const id = this.questao()?.id;
    if (id) await this.transicao(() => this.api.publicar(id), true);
  }
  async despublicar(): Promise<void> {
    const id = this.questao()?.id;
    if (id) await this.transicao(() => this.api.despublicar(id));
  }
  async salvar(): Promise<void> {
    this.salvando.set(true);
    this.erro.set(null);
    this.pendenciasPublicacao.set(null);
    try {
      const dados = this.dados();
      const resposta = this.questao()
        ? await firstValueFrom(this.api.atualizar(this.questao()!.id, dados))
        : await firstValueFrom(this.api.criar(dados));
      this.aplicarDetalhe(resposta);
    } catch (e) {
      this.erro.set(extrairErroApi(this.corpo(e)).message);
    } finally {
      this.salvando.set(false);
    }
  }
  private async transicao(
    requisicao: () => import('rxjs').Observable<QuestaoDetalhe>,
    publicacao = false,
  ): Promise<void> {
    try {
      this.pendenciasPublicacao.set(null);
      this.aplicarDetalhe(await firstValueFrom(requisicao()));
    } catch (e) {
      const erro = extrairErroApi(this.corpo(e));
      if (publicacao && erro.code === 'PUBLICACAO_INVALIDA') {
        this.pendenciasPublicacao.set(erro.fieldErrors);
        aplicarErrosDeCampo(erro.fieldErrors, this.controles());
      } else this.erro.set(erro.message);
    }
  }
  /**
   * Alternativas ainda em branco não vão para a API: o rascunho de CA-05 é salvo só com o
   * enunciado, e o formulário nasce com duas alternativas vazias que o `@Size(min = 1)` do
   * AlternativaDto recusaria. A exigência de duas alternativas é da publicação (CA-06).
   */
  private dados(): RequisicaoQuestao {
    const v = this.formulario.getRawValue();
    return {
      ...v,
      tipo: v.tipo,
      alternativas:
        v.tipo === 'CE'
          ? []
          : v.alternativas
              .filter((a) => a.texto.trim().length)
              .map((a, i) => ({ letra: null, texto: a.texto.trim(), ordem: i + 1 })),
      gabarito: v.gabarito,
      comentario: v.comentario,
    };
  }
  private aplicarDetalhe(d: QuestaoDetalhe): void {
    this.questao.set(d);
    this.formulario.patchValue({
      tipo: d.tipo,
      enunciado: d.enunciado,
      disciplinaId: d.disciplinaId,
      assuntoId: d.assuntoId,
      bancaId: d.bancaId,
      orgaoId: d.orgaoId,
      ano: d.ano,
      dificuldade: d.dificuldade,
      gabarito: d.gabarito,
      comentario: d.comentario,
    });
    this.formulario.controls.alternativas.clear();
    d.alternativas.forEach((a) =>
      this.formulario.controls.alternativas.push(
        this.fb.group({ texto: this.fb.nonNullable.control(a.texto, Validators.maxLength(2000)) }),
      ),
    );
    if (d.tipo === 'ME' && !d.alternativas.length) {
      this.adicionarAlternativa();
      this.adicionarAlternativa();
    }
  }
  private novaAlternativa(texto = ''): FormGroup<{ texto: FormControl<string> }> {
    return this.fb.group({
      texto: this.fb.nonNullable.control(texto, [Validators.maxLength(2000)]),
    });
  }
  private controles(): Record<string, import('@angular/forms').FormControl> {
    return this.formulario.controls as unknown as Record<
      string,
      import('@angular/forms').FormControl
    >;
  }
  private corpo(e: unknown): unknown {
    return e && typeof e === 'object' && 'error' in e ? (e as { error: unknown }).error : e;
  }
}
