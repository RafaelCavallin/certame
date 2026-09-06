import { TestBed } from '@angular/core/testing';
import { ModoSessao, QuestaoDaSerie } from '../../../core/api/modelos-estudo';
import { QuestaoCartao } from './questao-cartao';

function questaoME(alteracoes: Partial<QuestaoDaSerie> = {}): QuestaoDaSerie {
  return {
    id: 'q1',
    ordem: 1,
    codigo: 'Q-1',
    tipo: 'ME',
    enunciado: 'Qual a capital do Brasil?',
    disciplina: 'Geografia',
    assunto: 'Capitais',
    banca: 'Cebraspe',
    orgao: 'TCU',
    ano: 2025,
    dificuldade: 'MEDIA',
    alternativas: [
      { letra: 'A', texto: 'São Paulo', ordem: 1 },
      { letra: 'B', texto: 'Brasília', ordem: 2 },
    ],
    favorita: false,
    erroRecorrente: false,
    ...alteracoes,
  };
}

function montar(questao: QuestaoDaSerie, marcada: string | null = null, modo?: ModoSessao) {
  const fixture = TestBed.createComponent(QuestaoCartao);
  fixture.componentRef.setInput('questao', questao);
  fixture.componentRef.setInput('marcada', marcada);
  if (modo) fixture.componentRef.setInput('modo', modo);
  fixture.detectChanges();
  return fixture;
}

describe('QuestaoCartao', () => {
  it('exibe as alternativas de uma questão de múltipla escolha', () => {
    // Arrange & Act
    const fixture = montar(questaoME());

    // Assert
    const botoes = fixture.nativeElement.querySelectorAll('[role="radio"]');
    expect(botoes).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain('Brasília');
  });

  it('exibe as opções Certo/Errado para questão certo-errado', () => {
    // Arrange & Act
    const fixture = montar(questaoME({ tipo: 'CE', alternativas: [] }));

    // Assert
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Certo');
    expect(texto).toContain('Errado');
  });

  it('emite selecionar com a letra da alternativa clicada', () => {
    // Arrange
    const fixture = montar(questaoME());
    const selecionar = vi.fn();
    fixture.componentInstance.selecionar.subscribe(selecionar);

    // Act
    (fixture.nativeElement.querySelectorAll('[role="radio"]')[1] as HTMLButtonElement).click();

    // Assert
    expect(selecionar).toHaveBeenCalledWith('B');
  });

  it('marca a alternativa correspondente à resposta marcada como selecionada', () => {
    // Arrange & Act
    const fixture = montar(questaoME(), 'B');

    // Assert
    const botoes = fixture.nativeElement.querySelectorAll('[role="radio"]');
    expect(botoes[1].getAttribute('aria-checked')).toBe('true');
    expect(botoes[0].getAttribute('aria-checked')).toBe('false');
  });

  it('exibe as tags de metadados quando o modo é CONTEXTO', () => {
    // Arrange & Act
    const fixture = montar(questaoME(), null, 'CONTEXTO');

    // Assert
    const texto = fixture.nativeElement.textContent as string;
    expect(fixture.nativeElement.querySelector('.metadados')).not.toBeNull();
    expect(texto).toContain('Geografia');
    expect(texto).toContain('Cebraspe');
    expect(texto).toContain('2025');
    expect(texto).toContain('TCU');
  });

  it('não exibe as tags de metadados quando o modo é FOCO', () => {
    // Arrange & Act
    const fixture = montar(questaoME(), null, 'FOCO');

    // Assert
    expect(fixture.nativeElement.querySelector('.metadados')).toBeNull();
  });

  it('emite marcar com "favorita" ao clicar em favoritar', () => {
    // Arrange
    const fixture = montar(questaoME());
    const marcar = vi.fn();
    fixture.componentInstance.marcar.subscribe(marcar);

    // Act
    (
      Array.from(fixture.nativeElement.querySelectorAll('button')).find(
        (b) => (b as HTMLButtonElement).textContent?.trim() === 'Favoritar',
      ) as HTMLButtonElement
    ).click();

    // Assert
    expect(marcar).toHaveBeenCalledWith('favorita');
  });

  it('emite marcar com "erroRecorrente" ao clicar em marcar erro recorrente', () => {
    // Arrange
    const fixture = montar(questaoME());
    const marcar = vi.fn();
    fixture.componentInstance.marcar.subscribe(marcar);

    // Act
    (
      Array.from(fixture.nativeElement.querySelectorAll('button')).find(
        (b) => (b as HTMLButtonElement).textContent?.trim() === 'Marcar erro recorrente',
      ) as HTMLButtonElement
    ).click();

    // Assert
    expect(marcar).toHaveBeenCalledWith('erroRecorrente');
  });

  it('emite novoFlashcard ao clicar em novo flashcard', () => {
    // Arrange
    const fixture = montar(questaoME());
    const novoFlashcard = vi.fn();
    fixture.componentInstance.novoFlashcard.subscribe(novoFlashcard);

    // Act
    (
      Array.from(fixture.nativeElement.querySelectorAll('button')).find((b) =>
        (b as HTMLButtonElement).textContent?.includes('Novo flashcard'),
      ) as HTMLButtonElement
    ).click();

    // Assert
    expect(novoFlashcard).toHaveBeenCalledOnce();
  });

  it('emite adicionarCaderno ao clicar em adicionar ao caderno', () => {
    // Arrange
    const fixture = montar(questaoME());
    const adicionarCaderno = vi.fn();
    fixture.componentInstance.adicionarCaderno.subscribe(adicionarCaderno);

    // Act
    (
      Array.from(fixture.nativeElement.querySelectorAll('button')).find((b) =>
        (b as HTMLButtonElement).textContent?.includes('Adicionar ao caderno'),
      ) as HTMLButtonElement
    ).click();

    // Assert
    expect(adicionarCaderno).toHaveBeenCalledOnce();
  });

  it('mostra o contador de flashcards criados na sessão quando informado', () => {
    // Arrange
    const fixture = montar(questaoME());
    fixture.componentRef.setInput('criadosNaSessao', 3);

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain('3');
  });

  it('não mostra contador de flashcards quando nulo', () => {
    // Arrange
    const fixture = montar(questaoME());
    fixture.componentRef.setInput('criadosNaSessao', null);

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.querySelector('.btn-novo-flashcard .tag')).toBeNull();
  });

  it('trava as alternativas e não emite seleção quando bloqueada', () => {
    // Arrange
    const fixture = TestBed.createComponent(QuestaoCartao);
    fixture.componentRef.setInput('questao', questaoME());
    fixture.componentRef.setInput('marcada', 'A');
    fixture.componentRef.setInput('bloqueada', true);
    const selecionar = vi.fn();
    fixture.componentInstance.selecionar.subscribe(selecionar);
    fixture.detectChanges();

    // Act
    const alternativas = Array.from(
      fixture.nativeElement.querySelectorAll('[role="radio"]'),
    ) as HTMLButtonElement[];
    alternativas[1].click();
    fixture.componentInstance.alternativa('B');

    // Assert
    expect(alternativas.every((botao) => botao.disabled)).toBe(true);
    expect(selecionar).not.toHaveBeenCalled();
  });
});
