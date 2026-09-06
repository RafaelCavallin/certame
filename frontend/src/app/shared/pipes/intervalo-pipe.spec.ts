import { IntervaloPipe } from './intervalo-pipe';

describe('IntervaloPipe', () => {
  const pipe = new IntervaloPipe();

  it('formata minutos abaixo de uma hora', () => {
    expect(pipe.transform(1)).toBe('1 min');
    expect(pipe.transform(10)).toBe('10 min');
  });

  it('formata dias, incluindo o singular', () => {
    expect(pipe.transform(1440)).toBe('1 dia');
    expect(pipe.transform(12960)).toBe('9 dias');
  });

  it('formata horas entre uma hora e um dia', () => {
    expect(pipe.transform(180)).toBe('3 h');
  });
});
