import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CatalogoApi } from '../../../../core/api/catalogo-api';
import { corpoErro, extrairErroApi } from '../../../../core/api/erro-api';
import { DisciplinaArvore, AssuntoArvore } from '../../../../core/api/modelos-questao';
import { ConfirmacaoDialog } from '../../../../shared/ui/confirmacao-dialog/confirmacao-dialog';
@Component({
  selector: 'app-materias-page',
  imports: [FormsModule, ConfirmacaoDialog],
  templateUrl: './materias-page.html',
  styleUrl: './materias-page.scss',
})
export class MateriasPage {
  private readonly api = inject(CatalogoApi);
  readonly arvore = signal<DisciplinaArvore[]>([]);
  readonly nome = signal('');
  readonly dialog = signal<{
    tipo: 'disciplina' | 'assunto';
    id?: string;
    disciplinaId?: string;
    excluir?: boolean;
    arquivar?: boolean;
  } | null>(null);
  readonly recusa = signal<string | null>(null);
  readonly erro = signal<string | null>(null);
  constructor() {
    this.carregar();
  }
  carregar(): void {
    this.api.disciplinas().subscribe({
      next: (v) => this.arvore.set(v),
      error: (e) => this.erro.set(extrairErroApi(corpoErro(e)).message),
    });
  }
  abrir(tipo: 'disciplina' | 'assunto', id?: string, disciplinaId?: string): void {
    this.nome.set(id ? (this.encontrar(id)?.nome ?? '') : '');
    this.dialog.set({ tipo, id, disciplinaId });
  }
  abrirExcluir(tipo: 'disciplina' | 'assunto', id: string): void {
    this.dialog.set({ tipo, id, excluir: true });
  }
  async salvar(): Promise<void> {
    const d = this.dialog();
    if (!d || !this.nome().trim()) return;
    const req = {
      nome: this.nome().trim(),
      ...(d.disciplinaId ? { disciplinaId: d.disciplinaId } : {}),
    };
    try {
      if (d.tipo === 'disciplina') {
        if (d.id) await firstValueFrom(this.api.renomearDisciplina(d.id!, req));
        else await firstValueFrom(this.api.criarDisciplina(req));
      } else if (d.id) await firstValueFrom(this.api.renomearAssunto(d.id!, req));
      else await firstValueFrom(this.api.criarAssunto(req));
      this.dialog.set(null);
      this.carregar();
    } catch (e) {
      this.erro.set(extrairErroApi(corpoErro(e)).message);
    }
  }
  async excluir(): Promise<void> {
    const d = this.dialog();
    if (!d?.id) return;
    try {
      if (d.tipo === 'assunto') await firstValueFrom(this.api.excluirAssunto(d.id!));
      else await firstValueFrom(this.api.excluirDisciplina(d.id!));
      this.dialog.set(null);
      this.carregar();
    } catch (e) {
      const erro = extrairErroApi(corpoErro(e));
      if (d.tipo === 'assunto' && erro.code === 'ASSUNTO_EM_USO') {
        this.recusa.set(erro.message);
        this.dialog.set({ ...d, excluir: false, arquivar: true });
      } else this.erro.set(erro.message);
    }
  }
  async arquivar(): Promise<void> {
    const id = this.dialog()?.id;
    if (id) {
      await firstValueFrom(this.api.arquivarAssunto(id));
      this.recusa.set(null);
      this.dialog.set(null);
      this.carregar();
    }
  }
  assunto(id: string): AssuntoArvore | undefined {
    for (const d of this.arvore()) {
      const a = d.assuntos.find((x) => x.id === id);
      if (a) return a;
    }
    return undefined;
  }
  private encontrar(id: string): { nome: string } | undefined {
    return this.arvore().find((d) => d.id === id) ?? this.assunto(id);
  }
}
