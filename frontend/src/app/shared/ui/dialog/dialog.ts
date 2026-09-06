import {
  AfterViewInit,
  Component,
  Directive,
  ElementRef,
  HostListener,
  inject,
  input,
} from '@angular/core';
import { NgComponentOutlet } from '@angular/common';
import { DialogService, InstanciaDialogo } from './dialog-service';

const SELETOR_FOCAVEIS =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

@Directive({ selector: '[appDialogPainel]' })
export class DialogPainel implements AfterViewInit {
  readonly instancia = input.required<InstanciaDialogo>({ alias: 'appDialogPainel' });
  private readonly secao: HTMLElement = inject(ElementRef).nativeElement;

  ngAfterViewInit(): void {
    this.rotular();
    this.focarInicial();
  }

  @HostListener('keydown', ['$event'])
  protected aoTeclar(evento: KeyboardEvent): void {
    if (evento.key === 'Escape') {
      evento.preventDefault();
      this.instancia().dialogRef.solicitarCancelamento();
      return;
    }
    if (evento.key === 'Tab') this.confinarFoco(evento);
  }

  private rotular(): void {
    const titulo = this.secao.querySelector<HTMLElement>('.dialog-title');
    if (!titulo) return;
    titulo.id = `dialog-titulo-${this.instancia().id}`;
    this.secao.setAttribute('aria-labelledby', titulo.id);
  }

  private focarInicial(): void {
    const seletor = this.instancia().focoInicial;
    const alvo =
      (seletor ? this.secao.querySelector<HTMLElement>(seletor) : null) ?? this.focaveis()[0];
    alvo?.focus();
  }

  private confinarFoco(evento: KeyboardEvent): void {
    const foco = this.focaveis();
    if (foco.length === 0) {
      evento.preventDefault();
      return;
    }
    const primeiro = foco[0];
    const ultimo = foco[foco.length - 1];
    const atual = this.secao.ownerDocument.activeElement;
    if (evento.shiftKey && atual === primeiro) {
      evento.preventDefault();
      ultimo.focus();
      return;
    }
    if (!evento.shiftKey && atual === ultimo) {
      evento.preventDefault();
      primeiro.focus();
      return;
    }
    if (!this.secao.contains(atual)) {
      evento.preventDefault();
      primeiro.focus();
    }
  }

  private focaveis(): HTMLElement[] {
    return Array.from(this.secao.querySelectorAll<HTMLElement>(SELETOR_FOCAVEIS));
  }
}

@Component({
  selector: 'app-dialog',
  imports: [NgComponentOutlet, DialogPainel],
  templateUrl: './dialog.html',
  styleUrl: './dialog.scss',
})
export class Dialog {
  protected readonly servico = inject(DialogService);
}
