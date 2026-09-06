import { inject, Injectable } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { EstadoFlashcard, FiltroFlashcards } from '../../core/api/modelos-flashcard';
import { FlashcardsStore } from './flashcards-store';

const ATRASO_ROTA_MS = 350;

@Injectable()
export class FlashcardsFiltros {
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly store = inject(FlashcardsStore);
  private suprimirProximaRota = false;

  constructor() {
    this.rota.queryParamMap.pipe(takeUntilDestroyed()).subscribe((p) => this.restaurarDeFora(p));
  }

  buscar(termo: string): void {
    this.aplicar({ q: termo || undefined }, true);
  }
  filtrarDeck(deck: string): void {
    this.aplicar({ deck: deck || undefined }, false);
  }
  filtrarDisciplina(disciplinaId: string): void {
    this.aplicar({ disciplina: disciplinaId || undefined }, false);
  }
  filtrarEstado(estado: string): void {
    this.aplicar({ estado: (estado || undefined) as EstadoFlashcard | undefined }, false);
  }
  alternarArquivados(arquivados: boolean): void {
    this.aplicar({ arquivados }, false);
  }
  ordenar(sort: string): void {
    this.aplicar({ sort }, false);
  }

  private aplicar(alteracoes: Partial<FiltroFlashcards>, comAtraso: boolean): void {
    this.store.filtrar(alteracoes, comAtraso);
    const navegar = () => {
      this.suprimirProximaRota = true;
      void this.router.navigate([], {
        relativeTo: this.rota,
        queryParams: this.parametrosRota({ ...this.store.filtro(), ...alteracoes }),
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    };
    if (comAtraso) setTimeout(navegar, ATRASO_ROTA_MS);
    else navegar();
  }

  private restaurarDeFora(p: ParamMap): void {
    if (this.suprimirProximaRota) {
      this.suprimirProximaRota = false;
      return;
    }
    this.store.filtro.set(this.filtroDaRota(p));
    void this.store.carregar();
  }

  private filtroDaRota(p: ParamMap): FiltroFlashcards {
    return {
      q: p.get('q') ?? undefined,
      deck: p.get('deck') ?? undefined,
      disciplina: p.get('disciplina') ?? undefined,
      estado: (p.get('estado') as EstadoFlashcard | null) ?? undefined,
      arquivados: p.get('arquivados') === 'true',
      page: 0,
      size: 20,
      sort: p.get('sort') ?? 'criadoEm,desc',
    };
  }

  private parametrosRota(f: FiltroFlashcards): Record<string, string | null> {
    return {
      q: f.q || null,
      deck: f.deck || null,
      disciplina: f.disciplina || null,
      estado: f.estado || null,
      arquivados: f.arquivados ? 'true' : null,
      sort: f.sort && f.sort !== 'criadoEm,desc' ? f.sort : null,
    };
  }
}
