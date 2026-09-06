import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EditaisStore } from '../editais-store';
import { EditalCard } from '../edital-card/edital-card';
import { EstadoVazio } from '../../../shared/ui/estado-vazio/estado-vazio';
import { EditalResumo } from '../../../core/api/modelos-edital';

interface SecaoEditais {
  titulo: string;
  editais: EditalResumo[];
  mensagemVazia: string;
}

@Component({
  selector: 'app-editais-page',
  imports: [EditalCard, EstadoVazio],
  templateUrl: './editais-page.html',
  styleUrl: './editais-page.scss',
})
export class EditaisPage {
  protected readonly store = inject(EditaisStore);
  private readonly rota = inject(ActivatedRoute);
  protected readonly secoes = computed<SecaoEditais[]>(() => [
    {
      titulo: 'Abertos',
      editais: this.store.agrupados()?.abertos ?? [],
      mensagemVazia: 'Nenhum edital aberto no momento.',
    },
    {
      titulo: 'Previstos',
      editais: this.store.agrupados()?.previstos ?? [],
      mensagemVazia: 'Nenhum edital previsto no momento.',
    },
    {
      titulo: 'Encerrados',
      editais: this.store.agrupados()?.encerrados ?? [],
      mensagemVazia: 'Nenhum edital encerrado no momento.',
    },
  ]);

  constructor() {
    void this.carregar();
  }

  private async carregar(): Promise<void> {
    await this.store.carregar();
    const id = this.rota.snapshot.paramMap.get('id');
    if (id) this.store.selecionar(id);
  }
}
