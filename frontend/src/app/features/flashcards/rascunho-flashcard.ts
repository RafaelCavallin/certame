export interface RascunhoFlashcard {
  frente: string;
  verso: string;
}

const PREFIXO_CHAVE = 'fc-draft:';

export function gravarRascunho(questaoId: string, rascunho: RascunhoFlashcard): void {
  sessionStorage.setItem(PREFIXO_CHAVE + questaoId, JSON.stringify(rascunho));
}

export function lerRascunho(questaoId: string): RascunhoFlashcard | null {
  const bruto = sessionStorage.getItem(PREFIXO_CHAVE + questaoId);
  if (!bruto) return null;
  try {
    return JSON.parse(bruto) as RascunhoFlashcard;
  } catch {
    return null;
  }
}

export function limparRascunho(questaoId: string): void {
  sessionStorage.removeItem(PREFIXO_CHAVE + questaoId);
}
