import { computed, inject, Injectable, signal } from '@angular/core';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { CatalogoApi } from '../../../core/api/catalogo-api';
import { aplicarErrosDeCampo, ErroApi, extrairErroApi } from '../../../core/api/erro-api';
import { EditalApi } from '../../../core/api/edital-api';
import {
  DisciplinaExigida,
  EditalAdminResumo,
  EditalDetalhe,
  ItemCatalogoResumo,
  RequisicaoEdital,
  StatusEdital,
} from '../../../core/api/modelos-edital';

const PRAZO_OBRIGATORIO = 'Informe o prazo de inscrição para um edital aberto.';

@Injectable()
export class EditaisAdminStore {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(EditalApi);
  private readonly catalogo = inject(CatalogoApi);

  readonly editais = signal<EditalAdminResumo[]>([]);
  readonly carregandoLista = signal(false);
  readonly erroLista = signal<string | null>(null);

  readonly editalId = signal<string | null>(null);
  readonly disciplinasDisponiveis = signal<DisciplinaExigida[]>([]);
  readonly bancas = signal<ItemCatalogoResumo[]>([]);
  readonly orgaos = signal<ItemCatalogoResumo[]>([]);
  readonly carregandoForm = signal(false);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);

  readonly formulario = this.fb.group({
    nome: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(160),
    ]),
    status: this.fb.nonNullable.control<StatusEdital>('ABERTO'),
    vagas: this.fb.control<number | null>(null),
    bancaId: this.fb.control<string | null>(null),
    orgaoId: this.fb.control<string | null>(null),
    prazoInscricao: this.fb.control<string | null>(null),
    urlOficial: this.fb.control<string | null>(null),
    disciplinaIds: this.fb.nonNullable.control<string[]>([]),
  });

  private readonly statusAtual = toSignal(this.formulario.controls.status.valueChanges, {
    initialValue: this.formulario.controls.status.value,
  });
  private readonly prazoAtual = toSignal(this.formulario.controls.prazoInscricao.valueChanges, {
    initialValue: this.formulario.controls.prazoInscricao.value,
  });
  readonly disciplinasSelecionadas = toSignal(this.formulario.controls.disciplinaIds.valueChanges, {
    initialValue: this.formulario.controls.disciplinaIds.value,
  });
  readonly exigePrazo = computed(() => this.statusAtual() === 'ABERTO');
  readonly avisoPrazoAusente = computed(() => this.exigePrazo() && !this.prazoAtual());

  async carregarLista(): Promise<void> {
    this.carregandoLista.set(true);
    this.erroLista.set(null);
    try {
      this.editais.set(await firstValueFrom(this.api.listarParaAdministracao()));
    } catch (e) {
      this.erroLista.set(extrairErroApi(this.corpo(e)).message);
    } finally {
      this.carregandoLista.set(false);
    }
  }

  async excluir(id: string): Promise<void> {
    await firstValueFrom(this.api.excluir(id));
    await this.carregarLista();
  }

  async carregarForm(id: string | null): Promise<void> {
    this.carregandoForm.set(true);
    this.erro.set(null);
    this.editalId.set(null);
    this.formulario.reset({
      nome: '',
      status: 'ABERTO',
      vagas: null,
      bancaId: null,
      orgaoId: null,
      prazoInscricao: null,
      urlOficial: null,
      disciplinaIds: [],
    });
    try {
      const [disciplinas, bancas, orgaos, detalhe] = await Promise.all([
        firstValueFrom(this.catalogo.disciplinas()),
        firstValueFrom(this.catalogo.bancas()),
        firstValueFrom(this.catalogo.orgaos()),
        id ? firstValueFrom(this.api.buscar(id)) : Promise.resolve(null),
      ]);
      this.disciplinasDisponiveis.set(disciplinas);
      this.bancas.set(bancas);
      this.orgaos.set(orgaos);
      if (detalhe) this.aplicarDetalhe(detalhe);
    } catch (e) {
      this.erro.set(extrairErroApi(this.corpo(e)).message);
    } finally {
      this.carregandoForm.set(false);
    }
  }

  async salvar(): Promise<boolean> {
    this.erro.set(null);
    if (this.exigePrazo() && !this.formulario.controls.prazoInscricao.value) {
      this.formulario.controls.prazoInscricao.setErrors({ api: PRAZO_OBRIGATORIO });
      return false;
    }
    this.salvando.set(true);
    try {
      const dados = this.dados();
      const detalhe = this.editalId()
        ? await firstValueFrom(this.api.atualizar(this.editalId()!, dados))
        : await firstValueFrom(this.api.criar(dados));
      this.aplicarDetalhe(detalhe);
      return true;
    } catch (e) {
      this.tratarErro(extrairErroApi(this.corpo(e)));
      return false;
    } finally {
      this.salvando.set(false);
    }
  }

  private tratarErro(erro: ErroApi): void {
    if (erro.code === 'NOME_EM_USO') {
      this.formulario.controls.nome.setErrors({ api: erro.message });
      return;
    }
    if (erro.fieldErrors) {
      aplicarErrosDeCampo(erro.fieldErrors, this.controles());
      return;
    }
    this.erro.set(erro.message);
  }

  private aplicarDetalhe(d: EditalDetalhe): void {
    this.editalId.set(d.id);
    this.formulario.patchValue({
      nome: d.nome,
      status: d.status,
      vagas: d.vagas,
      bancaId: d.banca?.id ?? null,
      orgaoId: d.orgao?.id ?? null,
      prazoInscricao: d.prazoInscricao,
      urlOficial: d.urlOficial,
      disciplinaIds: d.disciplinas.map((disciplina) => disciplina.id),
    });
  }

  private dados(): RequisicaoEdital {
    const v = this.formulario.getRawValue();
    return { ...v, prazoInscricao: v.prazoInscricao || null, urlOficial: v.urlOficial || null };
  }
  private controles(): Record<string, FormControl> {
    return this.formulario.controls as unknown as Record<string, FormControl>;
  }
  private corpo(e: unknown): unknown {
    return e && typeof e === 'object' && 'error' in e ? (e as { error: unknown }).error : e;
  }
}
