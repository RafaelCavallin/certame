import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CatalogoApi } from '../../../core/api/catalogo-api';
import { FlashcardApi } from '../../../core/api/flashcard-api';
import {
  DeckResumo,
  EstadoFlashcard,
  FlashcardResumo,
  decksComId,
} from '../../../core/api/modelos-flashcard';
import { DisciplinaArvore } from '../../../core/api/modelos-questao';
import { ConfirmacaoDialog } from '../../../shared/ui/confirmacao-dialog/confirmacao-dialog';
import { DialogService } from '../../../shared/ui/dialog/dialog-service';
import { FlashcardDialog } from '../flashcard-dialog/flashcard-dialog';
import { FlashcardsFiltros } from '../flashcards-filtros';
import { FlashcardsIndicadores } from '../flashcards-indicadores';
import { FlashcardsStore } from '../flashcards-store';
import { MoverParaDeck } from '../mover-para-deck/mover-para-deck';

const ROTULOS_ESTADO: Record<EstadoFlashcard, string> = {
  NOVO: 'Novo',
  APRENDENDO: 'Aprendendo',
  REVISAO: 'Em revisão',
  REAPRENDENDO: 'Reaprendendo',
};
const TAG_ESTADO: Record<EstadoFlashcard, string> = {
  NOVO: 'tag tag-outline',
  APRENDENDO: 'tag tag-accent-2',
  REAPRENDENDO: 'tag tag-neutral',
  REVISAO: 'tag tag-accent',
};
const LIMITE_VERSO = 96;

@Component({
  selector: 'app-flashcards-page',
  imports: [DatePipe, RouterLink, ConfirmacaoDialog],
  providers: [FlashcardsStore, FlashcardsFiltros, FlashcardsIndicadores, MoverParaDeck],
  templateUrl: './flashcards-page.html',
  styleUrl: './flashcards-page.scss',
})
export class FlashcardsPage {
  private readonly dialogService = inject(DialogService);
  private readonly catalogo = inject(CatalogoApi);
  private readonly api = inject(FlashcardApi);
  private readonly rota = inject(ActivatedRoute);
  readonly store = inject(FlashcardsStore);
  readonly filtros = inject(FlashcardsFiltros);
  readonly indicadores = inject(FlashcardsIndicadores);
  readonly moverParaDeck = inject(MoverParaDeck);
  readonly disciplinas = signal<DisciplinaArvore[]>([]);
  readonly decksFiltro = signal<DeckResumo[]>([]);
  readonly excluindo = signal<FlashcardResumo | null>(null);

  constructor() {
    void firstValueFrom(this.catalogo.disciplinas())
      .then((d) => this.disciplinas.set(d))
      .catch(() => this.disciplinas.set([]));
    void firstValueFrom(this.api.listarDecks())
      .then((d) => this.decksFiltro.set(decksComId(d)))
      .catch(() => this.decksFiltro.set([]));
    const id = this.rota.snapshot.paramMap.get('id');
    if (id)
      void firstValueFrom(this.api.buscar(id))
        .then((card) => this.editar(card))
        .catch(() => undefined);
  }

  rotuloEstado(estado: EstadoFlashcard): string {
    return ROTULOS_ESTADO[estado];
  }
  tagEstado(estado: EstadoFlashcard): string {
    return TAG_ESTADO[estado];
  }
  versoResumido(verso: string): string {
    return verso.length > LIMITE_VERSO ? `${verso.slice(0, LIMITE_VERSO)}…` : verso;
  }

  abrirNovo(): void {
    this.dialogService
      .abrir(FlashcardDialog, { contexto: null })
      .subscribe(() => void this.store.carregar());
  }
  editar(card: FlashcardResumo): void {
    this.dialogService
      .abrir(FlashcardDialog, { contexto: { flashcardParaEditar: card } })
      .subscribe(() => void this.store.carregar());
  }
  arquivar(id: string): void {
    void this.store.arquivar(id);
  }
  restaurar(id: string): void {
    void this.store.restaurar(id);
  }
  pedirExclusao(card: FlashcardResumo): void {
    this.excluindo.set(card);
  }
  confirmarExclusao(): void {
    const card = this.excluindo();
    if (!card) return;
    this.excluindo.set(null);
    void this.store.excluir(card.id);
  }
}
