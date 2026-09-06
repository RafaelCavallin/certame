package br.com.certame.questao;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlternativaRepository extends JpaRepository<Alternativa, UUID> {
    List<Alternativa> findByQuestaoIdOrderByOrdemAsc(UUID questaoId);
}
