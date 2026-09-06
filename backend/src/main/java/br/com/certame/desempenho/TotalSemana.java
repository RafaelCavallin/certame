package br.com.certame.desempenho;

public record TotalSemana(int volume, int acertos) {
    public int pctAcerto() {
        return volume == 0 ? 0 : Math.round((float) acertos * 100 / volume);
    }
}
