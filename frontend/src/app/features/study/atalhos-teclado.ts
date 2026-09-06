import { Directive, HostListener, output } from '@angular/core';

@Directive({ selector: '[appAtalhosTeclado]', standalone: true })
export class AtalhosTeclado {
  readonly alternativa = output<string>();
  readonly acao = output<'confirmar' | 'anterior' | 'proxima' | 'marcar'>();
  @HostListener('document:keydown', ['$event']) tecla(evento: KeyboardEvent): void {
    const tecla = evento.key.toUpperCase();
    if ('ABCDE'.includes(tecla) || tecla === 'C' || tecla === 'E') {
      evento.preventDefault();
      this.alternativa.emit(
        tecla === 'C' || tecla === 'E' ? (tecla === 'C' ? 'CERTO' : 'ERRADO') : tecla,
      );
      return;
    }
    const acoes: Record<string, 'confirmar' | 'anterior' | 'proxima' | 'marcar'> = {
      Enter: 'confirmar',
      ArrowLeft: 'anterior',
      ArrowRight: 'proxima',
      F: 'marcar',
    };
    if (acoes[evento.key]) {
      evento.preventDefault();
      this.acao.emit(acoes[evento.key]);
    }
  }
}
