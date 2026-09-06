import { Injectable, inject, signal } from '@angular/core';
import { EstudoApi } from '../../core/api/estudo-api';
import { AuthStore } from '../../core/auth/auth-store';

@Injectable({ providedIn: 'root' })
export class Cronometro {
  private readonly api = inject(EstudoApi);
  private readonly auth = inject(AuthStore);
  readonly segundos = signal(0);
  readonly pausado = signal(false);
  /** Marca a sessão em curso: só com ela o cronômetro aparece no cabeçalho. */
  readonly ativo = signal(false);
  readonly pausadoMs = signal(0);
  private readonly sessaoId = signal<string | null>(null);
  private iniciadoEm = 0;
  private intervalo: ReturnType<typeof setInterval> | null = null;
  private iniciouPausaEm = 0;
  /** true quando o próprio usuário pediu a pausa (botão) — não desfeita ao voltar para a aba. */
  private pausaManual = false;
  private visibilidadeObservada = false;
  private fechamentoObservado = false;
  /** Inicia (ou reinicia) o relógio para a sessão informada; sempre zera o contador. */
  iniciar(sessaoId: string, segundos = 0): void {
    this.pararInterno();
    this.sessaoId.set(sessaoId);
    this.segundos.set(segundos);
    this.pausado.set(false);
    this.pausaManual = false;
    this.ativo.set(true);
    this.pausadoMs.set(0);
    this.iniciadoEm = Date.now();
    this.observarVisibilidade();
    this.observarFechamento();
    this.intervalo = setInterval(() => this.atualizar(), 1000);
  }
  /**
   * Para o relógio. Se `sessaoId` for informado e não for o dono atual, ignora —
   * evita que uma página destruída desligue o relógio de uma sessão mais nova.
   */
  parar(sessaoId?: string): void {
    if (sessaoId && sessaoId !== this.sessaoId()) return;
    this.pararInterno();
  }
  private pararInterno(): void {
    if (this.intervalo) clearInterval(this.intervalo);
    this.intervalo = null;
    this.ativo.set(false);
    this.finalizarPausa();
  }
  alternarPausa(): void {
    this.pausado() ? this.retomar() : this.pausar();
  }
  pausar(): void {
    this.pausaManual = true;
    this.pausarInterno();
  }
  retomar(): void {
    this.pausaManual = false;
    if (!this.pausado()) return;
    this.finalizarPausa();
    this.pausado.set(false);
    this.iniciadoEm = Date.now() - this.segundos() * 1000;
    const sessaoId = this.sessaoId();
    if (sessaoId) this.api.retomarSessao(sessaoId).subscribe({ error: () => undefined });
  }
  private pausarInterno(): void {
    if (this.pausado()) return;
    this.atualizar();
    this.pausado.set(true);
    this.iniciouPausaEm = Date.now();
    const sessaoId = this.sessaoId();
    if (sessaoId) this.api.pausar(sessaoId).subscribe({ error: () => undefined });
  }
  private atualizar(): void {
    if (!this.pausado()) this.segundos.set(this.segundos() + 1);
  }
  private finalizarPausa(): void {
    if (this.iniciouPausaEm) {
      this.pausadoMs.update((valor) => valor + Date.now() - this.iniciouPausaEm);
      this.iniciouPausaEm = 0;
    }
  }
  private observarVisibilidade(): void {
    if (this.visibilidadeObservada) return;
    document.addEventListener('visibilitychange', this.visibilidade);
    this.visibilidadeObservada = true;
  }
  /** Ao voltar à aba, só retoma automaticamente se a pausa não foi pedida pelo usuário. */
  private readonly visibilidade = (): void => {
    if (document.hidden) this.pausarInterno();
    else if (!this.pausaManual) this.retomar();
  };
  private observarFechamento(): void {
    if (this.fechamentoObservado) return;
    window.addEventListener('pagehide', this.fechamento);
    this.fechamentoObservado = true;
  }
  /**
   * Ao fechar a aba/navegador, o listener de visibilitychange pausa antes,
   * mas o observable do EstudoApi é cancelado junto com a página — por isso o
   * envio aqui é feito com fetch + keepalive, que sobrevive ao descarregamento.
   */
  private readonly fechamento = (): void => {
    if (this.pausado()) return;
    const sessaoId = this.sessaoId();
    const token = this.auth.accessToken();
    if (!sessaoId || !token) return;
    fetch(`/api/v1/estudo/sessoes/${sessaoId}/pausar`, {
      method: 'POST',
      keepalive: true,
      headers: { Authorization: `Bearer ${token}` },
    }).catch(() => undefined);
  };
}
