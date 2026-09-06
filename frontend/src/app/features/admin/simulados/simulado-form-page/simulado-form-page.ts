import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthStore } from '../../../../core/auth/auth-store';
import { PerfilUsuario } from '../../../../core/auth/modelos';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { aplicarErrosDeCampo, ErroApi, extrairErroApi } from '../../../../core/api/erro-api';
import { SimuladoApi } from '../../../../core/api/simulado-api';
import { ResumoComposicao, SimuladoDetalhe } from '../../../../core/models/modelos-simulado';
import { ComposicaoDisciplinas } from '../composicao-disciplinas/composicao-disciplinas';
import { ConfirmacaoDialog } from '../../../../shared/ui/confirmacao-dialog/confirmacao-dialog';

@Component({
  selector: 'app-simulado-form-page',
  imports: [ReactiveFormsModule, RouterLink, ComposicaoDisciplinas, ConfirmacaoDialog],
  templateUrl: './simulado-form-page.html',
  styleUrl: './simulado-form-page.scss',
})
export class SimuladoFormPage {
  protected readonly api = inject(SimuladoApi);
  protected readonly catalogo = inject(CatalogoApi);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthStore);
  /** A interface esconde a ação; quem autoriza de verdade é o @PreAuthorize do servidor. */
  protected readonly podePublicar = computed(
    () =>
      this.auth.perfil() === PerfilUsuario.ADMIN ||
      this.auth.perfil() === PerfilUsuario.SUPER_ADMIN,
  );
  protected readonly bancas = signal<{ id: string; nome: string }[]>([]);
  protected readonly detalhe = signal<SimuladoDetalhe | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly fieldErrors = signal<Record<string, string>>({});
  protected readonly carregando = signal(true);
  protected readonly confirmacaoExclusao = signal(false);
  protected readonly formulario = new FormGroup({
    nome: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3)],
    }),
    descricao: new FormControl('', { nonNullable: true }),
    bancaId: new FormControl<string | null>(null),
    numeroQuestoes: new FormControl(10, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(10), Validators.max(200)],
    }),
    duracaoSegundos: new FormControl(600, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(600), Validators.max(21600)],
    }),
    janelaAbertura: new FormControl(''),
    janelaEncerramento: new FormControl(''),
  });
  constructor() {
    void this.carregar();
    this.catalogo.bancas().subscribe((bancas) => this.bancas.set(bancas));
  }
  async carregar(): Promise<void> {
    const id = this.rota.snapshot.paramMap.get('id');
    if (!id) {
      this.carregando.set(false);
      return;
    }
    try {
      this.aplicar(await firstValueFrom(this.api.obter(id)));
    } catch (e) {
      this.erro.set(extrairErroApi(this.corpo(e)).message);
    } finally {
      this.carregando.set(false);
    }
  }
  async salvar(): Promise<void> {
    this.formulario.markAllAsTouched();
    if (this.formulario.invalid) return;
    try {
      const id = this.detalhe()?.id;
      const dados = this.dados();
      this.aplicar(
        await firstValueFrom(id ? this.api.atualizar(id, dados) : this.api.criar(dados)),
      );
      this.erro.set(null);
      if (!id) await this.router.navigate(['/admin/simulados', this.detalhe()?.id]);
    } catch (e) {
      this.tratarErro(e);
    }
  }
  async publicar(): Promise<void> {
    const id = this.detalhe()?.id;
    if (!id || !this.detalhe()?.composicao.fechada) return;
    try {
      this.aplicar(await firstValueFrom(this.api.publicar(id)));
    } catch (e) {
      this.tratarErro(e);
    }
  }
  async despublicar(): Promise<void> {
    const id = this.detalhe()?.id;
    if (!id) return;
    try {
      this.aplicar(await firstValueFrom(this.api.despublicar(id)));
    } catch (e) {
      this.tratarErro(e);
    }
  }
  excluir(): void {
    if (this.detalhe()?.id) this.confirmacaoExclusao.set(true);
  }
  cancelarExclusao(): void {
    this.confirmacaoExclusao.set(false);
  }
  async confirmarExclusao(): Promise<void> {
    this.confirmacaoExclusao.set(false);
    const id = this.detalhe()?.id;
    if (!id) return;
    try {
      await firstValueFrom(this.api.excluir(id));
      await this.router.navigate(['/admin/simulados']);
    } catch (e) {
      this.tratarErro(e);
    }
  }
  atualizouComposicao(resumo: ResumoComposicao): void {
    const atual = this.detalhe();
    if (atual) this.detalhe.set({ ...atual, composicao: resumo });
  }
  mensagem(nome: string): string | null {
    return (
      this.fieldErrors()[nome] ??
      (this.formulario.get(nome)?.touched && this.formulario.get(nome)?.hasError('required')
        ? 'Preencha este campo.'
        : null)
    );
  }
  private dados() {
    const valor = this.formulario.getRawValue();
    return {
      ...valor,
      descricao: valor.descricao || null,
      janelaAbertura: this.iso(valor.janelaAbertura),
      janelaEncerramento: this.iso(valor.janelaEncerramento),
    };
  }
  private iso(valor: string | null): string | null {
    return valor ? new Date(valor).toISOString() : null;
  }
  private aplicar(detalhe: SimuladoDetalhe): void {
    this.detalhe.set(detalhe);
    this.formulario.patchValue({
      nome: detalhe.nome,
      descricao: detalhe.descricao ?? '',
      bancaId: detalhe.bancaId,
      numeroQuestoes: detalhe.numeroQuestoes,
      duracaoSegundos: detalhe.duracaoSegundos,
      janelaAbertura: this.local(detalhe.janelaAbertura),
      janelaEncerramento: this.local(detalhe.janelaEncerramento),
    });
  }
  private local(valor: string | null): string {
    if (!valor) return '';
    const data = new Date(valor);
    const preencher = (n: number) => String(n).padStart(2, '0');
    return `${data.getFullYear()}-${preencher(data.getMonth() + 1)}-${preencher(data.getDate())}T${preencher(data.getHours())}:${preencher(data.getMinutes())}`;
  }
  private tratarErro(erro: unknown): void {
    const api: ErroApi = extrairErroApi(this.corpo(erro));
    this.erro.set(api.message);
    this.fieldErrors.set(api.fieldErrors ?? {});
    aplicarErrosDeCampo(api.fieldErrors, this.formulario.controls);
  }
  private corpo(erro: unknown): unknown {
    return erro && typeof erro === 'object' && 'error' in erro
      ? (erro as { error: unknown }).error
      : erro;
  }
}
