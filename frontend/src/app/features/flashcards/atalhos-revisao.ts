import { Directive, HostListener, input, output } from '@angular/core';
import { NotaRevisao } from '../../core/api/modelos-revisao';

const NOTAS_POR_TECLA: Record<string, NotaRevisao> = {
  '1': 'ERREI',
  '2': 'DIFICIL',
  '3': 'BOM',
  '4': 'FACIL',
};
const CAMPOS_DE_TEXTO = ['INPUT', 'TEXTAREA', 'SELECT'];

@Directive({ selector: '[appAtalhosRevisao]', standalone: true })
export class AtalhosRevisao {
  readonly revelado = input.required<boolean>();
  readonly revelar = output<void>();
  readonly classificar = output<NotaRevisao>();

  @HostListener('document:keydown', ['$event'])
  tecla(evento: KeyboardEvent): void {
    const alvo = evento.target as HTMLElement | null;
    if (CAMPOS_DE_TEXTO.includes(alvo?.tagName ?? '')) return;
    if (evento.code === 'Space' || evento.key === ' ') {
      evento.preventDefault();
      if (!this.revelado()) this.revelar.emit();
      return;
    }
    if (!this.revelado()) return;
    const nota = NOTAS_POR_TECLA[evento.key];
    if (nota) this.classificar.emit(nota);
  }
}
