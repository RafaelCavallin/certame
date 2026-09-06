package br.com.certame.catalogo.edital.dto;

import br.com.certame.catalogo.edital.Edital;
import br.com.certame.catalogo.edital.StatusEdital;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.hibernate.validator.constraints.URL;

public record RequisicaoEdital(
        @NotBlank(message = "Informe o nome do edital.")
                @Size(
                        min = 3,
                        max = Edital.NOME_TAMANHO_MAXIMO,
                        message = "O nome deve ter entre 3 e 160 caracteres.")
                String nome,
        @NotNull(message = "Informe o status do edital.") StatusEdital status,
        @Min(value = 0, message = "O número de vagas não pode ser negativo.")
                @Max(value = Edital.VAGAS_MAXIMO, message = "O número de vagas é muito alto.")
                Integer vagas,
        UUID bancaId,
        UUID orgaoId,
        LocalDate prazoInscricao,
        @URL(message = "Informe uma URL válida, iniciando com http:// ou https://.")
                @Size(
                        max = Edital.URL_OFICIAL_TAMANHO_MAXIMO,
                        message = "A URL oficial deve ter no máximo 500 caracteres.")
                String urlOficial,
        @NotNull(message = "Informe as disciplinas exigidas.") Set<UUID> disciplinaIds) {}
