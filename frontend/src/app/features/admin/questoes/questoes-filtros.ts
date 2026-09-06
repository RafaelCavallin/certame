import { ActivatedRoute, Router } from '@angular/router';
import { inject, Injectable, signal } from '@angular/core';
import { StatusQuestao } from '../../../core/api/modelos-questao';

@Injectable()
export class QuestoesFiltros {
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly termo = signal('');
  readonly disciplinaId = signal('');
  readonly bancaId = signal('');
  readonly status = signal<StatusQuestao | ''>('');
  readonly pagina = signal(0);
  readonly tamanho = signal(20);
  constructor() {
    this.restaurar(this.rota.snapshot.queryParamMap);
    this.rota.queryParamMap.subscribe((p) => this.restaurar(p));
  }
  aplicar(): void {
    this.pagina.set(0);
    void this.router.navigate([], {
      relativeTo: this.rota,
      queryParams: this.parametros(),
      queryParamsHandling: 'merge',
    });
  }
  limpar(): void {
    this.termo.set('');
    this.disciplinaId.set('');
    this.bancaId.set('');
    this.status.set('');
    this.pagina.set(0);
    this.aplicar();
  }
  irPara(pagina: number): void {
    this.pagina.set(Math.max(0, pagina));
    this.aplicar();
  }
  parametros(): Record<string, string | number | null> {
    return {
      q: this.termo() || null,
      disciplina: this.disciplinaId() || null,
      banca: this.bancaId() || null,
      status: this.status() || null,
      page: this.pagina(),
      size: this.tamanho(),
    };
  }
  private restaurar(p: import('@angular/router').ParamMap): void {
    this.termo.set(p.get('q') ?? '');
    this.disciplinaId.set(p.get('disciplina') ?? '');
    this.bancaId.set(p.get('banca') ?? '');
    this.status.set((p.get('status') as StatusQuestao | '') ?? '');
    this.pagina.set(Number(p.get('page') ?? 0) || 0);
    this.tamanho.set(Number(p.get('size') ?? 20) || 20);
  }
}
