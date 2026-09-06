import { CardRevisao, RespostaRevisao } from '../../core/api/modelos-revisao';

export function proximaFila(
  restante: CardRevisao[],
  resposta: RespostaRevisao,
  prioridades: Map<string, string>,
): CardRevisao[] {
  const card = resposta.card;
  if (!resposta.reenfileirar || !card) return restante;
  prioridades.set(card.id, resposta.proximaRevisaoEm);
  const indice = restante.findIndex((c) => {
    const prioridade = prioridades.get(c.id);
    return prioridade !== undefined && prioridade > resposta.proximaRevisaoEm;
  });
  if (indice === -1) return [...restante, card];
  return [...restante.slice(0, indice), card, ...restante.slice(indice)];
}
