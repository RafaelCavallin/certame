import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SimuladoApi } from '../../../core/api/simulado-api';
import { RespostaResultado } from '../../../core/models/modelos-simulado';

@Component({
  selector: 'app-simulado-resultado-page',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './simulado-resultado-page.html',
  styleUrl: './simulado-resultado-page.scss',
})
export class SimuladoResultadoPage {
  protected readonly resultado = signal<RespostaResultado | null>(null);
  protected readonly erro = signal<string | null>(null);
  private readonly api = inject(SimuladoApi);
  private readonly rota = inject(ActivatedRoute);
  constructor() {
    void this.carregar();
  }
  async carregar(): Promise<void> {
    const simuladoId = this.rota.snapshot.paramMap.get('id');
    const tentativaId = this.rota.snapshot.queryParamMap.get('tentativa');
    if (!simuladoId || !tentativaId) {
      this.erro.set('Resultado não encontrado.');
      return;
    }
    try {
      this.resultado.set(await firstValueFrom(this.api.resultado(simuladoId, tentativaId)));
    } catch {
      this.erro.set('Não foi possível carregar o resultado.');
    }
  }
  tempo(segundos: number): string {
    const minutos = Math.floor(segundos / 60);
    return minutos >= 60 ? `${Math.floor(minutos / 60)}h ${minutos % 60}min` : `${minutos} min`;
  }
}
