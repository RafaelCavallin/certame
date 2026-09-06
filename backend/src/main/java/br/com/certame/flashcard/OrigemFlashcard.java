package br.com.certame.flashcard;

import br.com.certame.catalogo.Assunto;
import br.com.certame.catalogo.Disciplina;
import br.com.certame.estudo.SessaoEstudo;
import br.com.certame.questao.Questao;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Classificação e proveniência do flashcard: disciplina/assunto e, se aplicável, questão ou sessão de origem. */
@Embeddable
public class OrigemFlashcard {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disciplina_id")
    private Disciplina disciplina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assunto_id")
    private Assunto assunto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questao_origem_id")
    private Questao questaoOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_origem_id")
    private SessaoEstudo sessaoOrigem;

    public Disciplina getDisciplina() { return disciplina; }
    public Assunto getAssunto() { return assunto; }
    public Questao getQuestaoOrigem() { return questaoOrigem; }
    public SessaoEstudo getSessaoOrigem() { return sessaoOrigem; }

    public void setDisciplina(Disciplina disciplina) { this.disciplina = disciplina; }
    public void setAssunto(Assunto assunto) { this.assunto = assunto; }
    public void setQuestaoOrigem(Questao questaoOrigem) { this.questaoOrigem = questaoOrigem; }
    public void setSessaoOrigem(SessaoEstudo sessaoOrigem) { this.sessaoOrigem = sessaoOrigem; }
}
