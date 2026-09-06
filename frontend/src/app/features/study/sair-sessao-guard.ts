import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { map } from 'rxjs';
import { DialogService } from '../../shared/ui/dialog/dialog-service';
import { ConfirmacaoDialogo } from '../../shared/ui/confirmacao-dialog/confirmacao-dialogo';
import { Cronometro } from './cronometro';
import { EstudarPage } from './estudar-page/estudar-page';

/** Confirma a saída de uma sessão em curso e, ao confirmar, pausa o cronômetro (retomável depois). */
export const sairSessaoGuard: CanDeactivateFn<EstudarPage> = (componente) => {
  const cronometro = inject(Cronometro);
  if (!cronometro.ativo()) return true;
  const dialogService = inject(DialogService);
  const sessaoId = componente.store.sessao()?.sessaoId;
  return dialogService
    .abrir<boolean>(ConfirmacaoDialogo, {
      contexto: {
        titulo: 'Sair da sessão?',
        mensagem: 'Seu tempo será pausado e você poderá retomar de onde parou.',
        confirmarLabel: 'Sair',
      },
    })
    .pipe(
      map((confirmado) => {
        if (!confirmado) return false;
        cronometro.pausar();
        cronometro.parar(sessaoId);
        return true;
      }),
    );
};
