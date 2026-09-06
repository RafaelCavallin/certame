import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { ItemCatalogo } from '../../../../core/api/modelos-questao';
import { ConfirmacaoDialog } from '../../../../shared/ui/confirmacao-dialog/confirmacao-dialog';
@Component({
  selector: 'app-catalogo-page',
  imports: [FormsModule, ConfirmacaoDialog],
  templateUrl: './catalogo-page.html',
  styleUrl: './catalogo-page.scss',
})
export class CatalogoPage {
  private readonly api = inject(CatalogoApi);
  readonly aba = signal<'banca' | 'orgao'>('banca');
  readonly itens = signal<ItemCatalogo[]>([]);
  readonly nome = signal('');
  readonly sigla = signal('');
  readonly editando = signal<string | null>(null);
  readonly formAberto = signal(false);
  readonly dialog = signal<{ id: string } | null>(null);
  readonly erro = signal<string | null>(null);
  constructor() {
    this.carregar();
  }
  carregar(): void {
    const request = this.aba() === 'banca' ? this.api.bancas() : this.api.orgaos();
    request.subscribe({
      next: (v) => this.itens.set(v),
      error: (e) => this.erro.set(this.mensagem(e)),
    });
  }
  trocar(aba: 'banca' | 'orgao'): void {
    this.aba.set(aba);
    this.limpar();
    this.carregar();
  }
  abrir(item?: ItemCatalogo): void {
    this.editando.set(item?.id ?? null);
    this.nome.set(item?.nome ?? '');
    this.sigla.set(item?.sigla ?? '');
    this.formAberto.set(true);
  }
  async salvar(): Promise<void> {
    if (this.nome().trim().length < 2) {
      this.erro.set('Informe um nome com ao menos 2 caracteres.');
      return;
    }
    const dados = { nome: this.nome().trim(), sigla: this.sigla().trim() || null };
    try {
      const id = this.editando();
      if (this.aba() === 'banca')
        await firstValueFrom(id ? this.api.atualizarBanca(id, dados) : this.api.criarBanca(dados));
      else
        await firstValueFrom(id ? this.api.atualizarOrgao(id, dados) : this.api.criarOrgao(dados));
      this.limpar();
      this.carregar();
    } catch (e) {
      this.erro.set(this.mensagem(e));
    }
  }
  excluir(id: string): void {
    this.dialog.set({ id });
  }
  async confirmarExclusao(): Promise<void> {
    const id = this.dialog()?.id;
    if (!id) return;
    try {
      if (this.aba() === 'banca') await firstValueFrom(this.api.excluirBanca(id));
      else await firstValueFrom(this.api.excluirOrgao(id));
      this.dialog.set(null);
      this.carregar();
    } catch (e) {
      this.dialog.set(null);
      this.erro.set(this.mensagem(e));
    }
  }
  limpar(): void {
    this.editando.set(null);
    this.nome.set('');
    this.sigla.set('');
    this.formAberto.set(false);
  }
  private mensagem(e: unknown): string {
    return e && typeof e === 'object' && 'error' in e
      ? (((e as { error: { message?: string } }).error ?? {}).message ??
          'Não foi possível concluir a operação.')
      : 'Não foi possível concluir a operação.';
  }
}
