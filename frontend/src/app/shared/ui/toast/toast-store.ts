import { Injectable, signal } from '@angular/core';

const DURACAO_PADRAO_MS = 2800;

@Injectable({ providedIn: 'root' })
export class ToastStore {
  readonly mensagem = signal<string | null>(null);
  private temporizador: ReturnType<typeof setTimeout> | null = null;

  publicar(texto: string, duracaoMs = DURACAO_PADRAO_MS): void {
    if (this.temporizador) clearTimeout(this.temporizador);
    this.mensagem.set(texto);
    this.temporizador = setTimeout(() => this.limpar(), duracaoMs);
  }

  limpar(): void {
    if (this.temporizador) clearTimeout(this.temporizador);
    this.temporizador = null;
    this.mensagem.set(null);
  }
}
