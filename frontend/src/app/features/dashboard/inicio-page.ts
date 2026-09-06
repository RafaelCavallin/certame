import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { EstudoApi } from '../../core/api/estudo-api';
import { ResumoInicio, SessaoRetomavel } from '../../core/api/modelos-estudo';

@Component({
  selector: 'app-inicio-page',
  templateUrl: './inicio-page.html',
  styleUrl: './inicio-page.scss',
})
export class InicioPage {
  readonly resumo = signal<ResumoInicio | null>(null);
  readonly erro = signal<string | null>(null);
  private readonly api = inject(EstudoApi);
  private readonly router = inject(Router);
  constructor() {
    this.api.inicio().subscribe({
      next: (resumo) => this.resumo.set(resumo),
      error: () => this.erro.set('Não foi possível carregar seu resumo.'),
    });
  }
  iniciar(): void {
    void this.router.navigateByUrl('/estudar');
  }
  continuar(sessaoId: string): void {
    void this.router.navigate(['/estudar'], { queryParams: { sessao: sessaoId } });
  }
  tempo(segundos: number): string {
    return `${Math.floor(segundos / 60)} min`;
  }
  /** indiceAtual aponta para a 1ª questão sem resposta; se todas já foram respondidas, mostra a última. */
  questaoAtual(sessao: SessaoRetomavel): number {
    return Math.min(sessao.indiceAtual + 1, sessao.total);
  }
}
