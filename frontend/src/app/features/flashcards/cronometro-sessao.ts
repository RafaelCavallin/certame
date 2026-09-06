import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class CronometroSessao {
  readonly segundos = signal(0);
  private intervalo: ReturnType<typeof setInterval> | null = null;

  iniciar(): void {
    this.parar();
    this.segundos.set(0);
    this.intervalo = setInterval(() => {
      if (!document.hidden) this.segundos.update((valor) => valor + 1);
    }, 1000);
  }

  parar(): void {
    if (this.intervalo) clearInterval(this.intervalo);
    this.intervalo = null;
  }
}
