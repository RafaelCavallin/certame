import { Component, inject } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Dialog } from './dialog';
import { DIALOG_DATA, DialogRef, DialogService } from './dialog-service';

@Component({
  selector: 'app-conteudo-teste',
  template: `<h2 class="dialog-title">{{ dados }}</h2>
    <input class="frente" />
    <button class="cancelar" type="button" (click)="dialogRef.solicitarCancelamento()">
      Cancelar
    </button>
    <button class="salvar" type="button" (click)="dialogRef.fechar('resultado')">Salvar</button>`,
})
class ConteudoTeste {
  protected readonly dados = inject(DIALOG_DATA);
  protected readonly dialogRef = inject(DialogRef);
}

function raiz(fixture: ComponentFixture<Dialog>): HTMLElement {
  return fixture.nativeElement as HTMLElement;
}

function montarOrigem(): HTMLButtonElement {
  const origem = document.createElement('button');
  document.body.appendChild(origem);
  origem.focus();
  return origem;
}

describe('DialogService', () => {
  it('foca a frente ao abrir e devolve o foco ao elemento indicado no fechamento', async () => {
    const fixture = TestBed.createComponent(Dialog);
    fixture.detectChanges();
    const servico = TestBed.inject(DialogService);
    const origem = montarOrigem();

    const promessa = firstValueFrom(
      servico.abrir(ConteudoTeste, {
        contexto: 'Novo flashcard',
        focoInicial: '.frente',
        devolverFocoPara: origem,
      }),
    );
    fixture.detectChanges();

    const campoFrente = raiz(fixture).querySelector<HTMLInputElement>('.frente');
    expect(document.activeElement).toBe(campoFrente);

    raiz(fixture).querySelector<HTMLButtonElement>('.salvar')!.click();
    const resultado = await promessa;
    fixture.detectChanges();

    expect(resultado).toBe('resultado');
    expect(document.activeElement).toBe(origem);
    origem.remove();
  });

  it('confina o Tab entre os controles do diálogo sem alcançar a tela ao fundo', () => {
    const fixture = TestBed.createComponent(Dialog);
    fixture.detectChanges();
    const servico = TestBed.inject(DialogService);
    const botaoDeFundo = document.createElement('button');
    document.body.appendChild(botaoDeFundo);

    servico.abrir(ConteudoTeste, { focoInicial: '.frente' });
    fixture.detectChanges();

    const secao = raiz(fixture).querySelector<HTMLElement>('.dialog')!;
    const salvar = raiz(fixture).querySelector<HTMLButtonElement>('.salvar')!;
    const frente = raiz(fixture).querySelector<HTMLInputElement>('.frente')!;

    salvar.focus();
    secao.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true }),
    );
    expect(document.activeElement).toBe(frente);

    frente.focus();
    secao.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true }),
    );
    expect(document.activeElement).toBe(salvar);
    expect(document.activeElement).not.toBe(botaoDeFundo);

    botaoDeFundo.focus();
    secao.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true }),
    );
    expect(document.activeElement).toBe(frente);
    botaoDeFundo.remove();
  });

  it('esc solicita o cancelamento em vez de fechar diretamente', () => {
    const fixture = TestBed.createComponent(Dialog);
    fixture.detectChanges();
    const servico = TestBed.inject(DialogService);
    const emitido = vi.fn();

    servico.abrir(ConteudoTeste).subscribe(emitido);
    fixture.detectChanges();
    const secao = raiz(fixture).querySelector<HTMLElement>('.dialog')!;
    secao.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }),
    );

    expect(emitido).not.toHaveBeenCalled();
  });
});
