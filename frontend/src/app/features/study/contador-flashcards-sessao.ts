const PREFIXO_CHAVE = 'fc-contador:';

export function gravarContadorFlashcards(sessaoId: string, contador: number): void {
  sessionStorage.setItem(PREFIXO_CHAVE + sessaoId, String(contador));
}

export function lerContadorFlashcards(sessaoId: string): number | null {
  const bruto = sessionStorage.getItem(PREFIXO_CHAVE + sessaoId);
  if (!bruto) return null;
  const numero = Number(bruto);
  return Number.isFinite(numero) ? numero : null;
}
