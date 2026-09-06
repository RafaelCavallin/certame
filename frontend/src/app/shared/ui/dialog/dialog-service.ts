import { inject, Injectable, InjectionToken, Injector, signal, Type } from '@angular/core';
import { finalize, Observable, Subject } from 'rxjs';

export const DIALOG_DATA = new InjectionToken<unknown>('DIALOG_DATA');

export class DialogRef {
  private readonly fechado = new Subject<unknown>();
  private readonly cancelamento = new Subject<void>();
  readonly fechado$ = this.fechado.asObservable();
  readonly cancelamentoSolicitado$ = this.cancelamento.asObservable();
  fechar(resultado?: unknown): void {
    this.fechado.next(resultado);
    this.fechado.complete();
  }
  solicitarCancelamento(): void {
    this.cancelamento.next();
  }
}

export interface OpcoesDialogo<TContexto = unknown> {
  readonly contexto?: TContexto;
  readonly focoInicial?: string;
  readonly devolverFocoPara?: HTMLElement | null;
}

export interface InstanciaDialogo {
  readonly id: number;
  readonly componente: Type<unknown>;
  readonly injetor: Injector;
  readonly focoInicial: string | null;
  readonly dialogRef: DialogRef;
}

@Injectable({ providedIn: 'root' })
export class DialogService {
  private readonly injetorPai = inject(Injector);
  private readonly pilha = signal<InstanciaDialogo[]>([]);
  private sequencia = 0;
  readonly abertos = this.pilha.asReadonly();

  abrir<TResultado = void, TContexto = unknown>(
    componente: Type<unknown>,
    opcoes: OpcoesDialogo<TContexto> = {},
  ): Observable<TResultado | undefined> {
    const dialogRef = new DialogRef();
    const devolverFocoPara =
      opcoes.devolverFocoPara ?? (document.activeElement as HTMLElement | null);
    const injetor = Injector.create({
      parent: this.injetorPai,
      providers: [
        { provide: DialogRef, useValue: dialogRef },
        { provide: DIALOG_DATA, useValue: opcoes.contexto },
      ],
    });
    const instancia: InstanciaDialogo = {
      id: ++this.sequencia,
      componente,
      injetor,
      focoInicial: opcoes.focoInicial ?? null,
      dialogRef,
    };
    this.pilha.update((atuais) => [...atuais, instancia]);
    const fechado$ = dialogRef.fechado$ as Observable<TResultado | undefined>;
    return fechado$.pipe(
      finalize(() => {
        this.pilha.update((atuais) => atuais.filter((item) => item !== instancia));
        devolverFocoPara?.focus();
      }),
    );
  }
}
