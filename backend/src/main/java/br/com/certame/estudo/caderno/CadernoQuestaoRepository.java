package br.com.certame.estudo.caderno;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CadernoQuestaoRepository extends JpaRepository<CadernoQuestao, CadernoQuestaoId> {
    List<CadernoQuestao> findByIdCadernoIdOrderByAdicionadaEmAsc(UUID cadernoId);
    long countByIdCadernoId(UUID cadernoId);
    @Modifying
    @Query(value = "insert into caderno_questao (caderno_id, questao_id) values (:cadernoId, :questaoId) on conflict do nothing", nativeQuery = true)
    void adicionar(@Param("cadernoId") UUID cadernoId, @Param("questaoId") UUID questaoId);
    @Modifying
    @Query("delete from CadernoQuestao cq where cq.id.cadernoId = :cadernoId and cq.id.questaoId = :questaoId")
    int remover(@Param("cadernoId") UUID cadernoId, @Param("questaoId") UUID questaoId);
}
