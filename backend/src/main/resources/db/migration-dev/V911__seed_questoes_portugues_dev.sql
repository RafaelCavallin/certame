-- 10 questões reais de Língua Portuguesa, com assuntos diversos, para testes manuais em dev.
-- Diferente do seed sintético da V902, aqui enunciado, alternativas e comentário têm conteúdo de verdade.

-- Disciplina e assuntos ------------------------------------------------------
insert into disciplina (nome)
select 'Língua Portuguesa'
where not exists (select 1 from disciplina where lower(nome) = lower('Língua Portuguesa') and excluido_em is null);

insert into assunto (disciplina_id, nome)
select d.id, novos.nome
from disciplina d
cross join (values
    ('Concordância verbal'),
    ('Regência verbal'),
    ('Crase'),
    ('Acentuação gráfica'),
    ('Pontuação'),
    ('Interpretação de texto'),
    ('Morfologia - classes de palavras'),
    ('Colocação pronominal'),
    ('Semântica e significação das palavras'),
    ('Sintaxe do período composto')
) as novos(nome)
where d.nome = 'Língua Portuguesa' and d.excluido_em is null
  and not exists (
    select 1 from assunto a
    where a.disciplina_id = d.id and lower(a.nome) = lower(novos.nome) and a.excluido_em is null
  );

-- Questões -------------------------------------------------------------------
insert into questao (tipo, enunciado, disciplina_id, assunto_id, banca_id, orgao_id, ano, dificuldade, gabarito, comentario, status, autor_id, publicado_em)
select q.tipo, q.enunciado, d.id, a.id, b.id, o.id, q.ano, q.dificuldade, q.gabarito, q.comentario, 'PUBLICADA', u.id, now() - (q.dias || ' days')::interval
from (values
    (
      'PT-01', 'ME',
      'Assinale a alternativa em que a concordância verbal está de acordo com a norma-padrão da língua portuguesa.',
      'Concordância verbal', 'FCC', 'RFB', 2023, 'MEDIA', 'C',
      'A expressão "mais de um" concorda com o verbo no singular: "Mais de um candidato se inscreveu". Nas demais, há erro: "Fazem dois anos" (o verbo fazer indicando tempo decorrido é impessoal), "Haviam muitas pessoas" (haver no sentido de existir é impessoal) e "Vende-se casas" (sujeito paciente no plural exige "Vendem-se casas").',
      8
    ),
    (
      'PT-02', 'ME',
      'Considerando a regência verbal na norma-padrão, assinale a alternativa correta.',
      'Regência verbal', 'CEBRASPE', 'TCU', 2024, 'DIFICIL', 'B',
      'O verbo "aspirar", no sentido de almejar, é transitivo indireto e exige a preposição "a": "aspirava ao cargo de diretor". Note que, por ser objeto indireto preposicionado, não admite o pronome "lhe" substituído por "o".',
      12
    ),
    (
      'PT-03', 'ME',
      'Assinale a alternativa em que o emprego do sinal indicativo de crase está correto.',
      'Crase', 'FGV', 'SF', 2024, 'MEDIA', 'A',
      'Ocorre crase na locução adverbial feminina "às pressas". Não há crase antes de palavra masculina ("a cavalo"), antes de verbo no infinitivo ("a fim de responder") nem diante de pronome de tratamento como "Vossa Senhoria".',
      21
    ),
    (
      'PT-04', 'ME',
      'Assinale a alternativa em que todas as palavras estão corretamente acentuadas segundo o Acordo Ortográfico vigente.',
      'Acentuação gráfica', 'VUNESP', 'TJSP', 2023, 'FACIL', 'D',
      'Após o Acordo Ortográfico, perderam o acento os ditongos abertos "ei" e "oi" das paroxítonas (ideia, heroico, jiboia) e o hiato "oo" (voo, enjoo). Permanece o acento em oxítonas terminadas em ditongo aberto, como "herói" e "chapéu".',
      30
    ),
    (
      'PT-05', 'ME',
      'Assinale a alternativa cuja pontuação está de acordo com a norma-padrão.',
      'Pontuação', 'CESGRANRIO', 'CD', 2025, 'MEDIA', 'B',
      'A oração adjetiva explicativa deve ser isolada por vírgulas: "Os servidores, que participaram do curso, receberam o certificado" só é aceitável quando todos participaram. Já a vírgula entre sujeito e predicado, entre verbo e objeto direto, ou antes do "que" restritivo é vedada.',
      3
    ),
    (
      'PT-06', 'ME',
      'Leia o trecho: "Não fosse a atuação célere da equipe, o prejuízo teria sido irreparável." O trecho destacado estabelece com a oração seguinte uma relação de',
      'Interpretação de texto', 'IBFC', 'INSS', 2024, 'MEDIA', 'C',
      'A construção "Não fosse X" equivale a "Se não fosse X", introduzindo uma hipótese: trata-se de oração subordinada adverbial condicional. O verbo no futuro do pretérito ("teria sido") na oração principal reforça a leitura condicional.',
      15
    ),
    (
      'PT-07', 'ME',
      'Na frase "Ele chegou bastante cansado, mas resolveu bastante problemas naquela tarde", as palavras destacadas classificam-se, respectivamente, como',
      'Morfologia - classes de palavras', 'QUADRIX', 'MPU', 2023, 'DIFICIL', 'A',
      '"Bastante" é advérbio quando intensifica adjetivo ou verbo (invariável: "bastante cansado") e é pronome indefinido adjetivo quando acompanha substantivo, flexionando em número ("bastantes problemas" — a frase do enunciado, portanto, contém desvio de norma nesse segundo uso).',
      45
    ),
    (
      'PT-08', 'ME',
      'Assinale a alternativa em que a colocação pronominal atende à norma-padrão.',
      'Colocação pronominal', 'AOCP', 'PRF', 2025, 'MEDIA', 'B',
      'Palavras atrativas — advérbios, pronomes indefinidos, relativos, conjunções subordinativas e palavras negativas — exigem próclise: "Nunca me disseram a verdade". A ênclise após futuro do presente é vedada, cabendo a mesóclise ("dir-lhe-ei").',
      6
    ),
    (
      'PT-09', 'CE',
      'Julgue o item a seguir: em "O réu foi absolvido, pois as provas eram frágeis", a conjunção "pois" exprime explicação e poderia ser substituída, sem alteração de sentido, por "porquanto".',
      'Semântica e significação das palavras', 'CEBRASPE', 'PF', 2024, 'FACIL', 'CERTO',
      'Item certo. Posposto à oração principal e precedido de vírgula, "pois" tem valor explicativo/causal, sendo equivalente a "porquanto", "porque" e "já que". Só teria valor conclusivo se viesse deslocado após o verbo ("Estava tarde; saímos, pois, mais cedo").',
      10
    ),
    (
      'PT-10', 'CE',
      'Julgue o item a seguir: na frase "Quem avisa amigo é", o termo "quem" funciona como pronome relativo com antecedente expresso.',
      'Sintaxe do período composto', 'CEBRASPE', 'CGU', 2025, 'DIFICIL', 'ERRADO',
      'Item errado. Em "Quem avisa amigo é", o pronome "quem" é indefinido e não possui antecedente expresso — trata-se de pronome relativo indefinido, que introduz uma oração subordinada substantiva subjetiva. Só seria relativo com antecedente em construções como "O colega a quem avisei".',
      2
    )
) as q(chave, tipo, enunciado, assunto, banca_sigla, orgao_sigla, ano, dificuldade, gabarito, comentario, dias)
join disciplina d on d.nome = 'Língua Portuguesa' and d.excluido_em is null
join assunto a on a.disciplina_id = d.id and a.nome = q.assunto and a.excluido_em is null
join banca b on b.sigla = q.banca_sigla
left join orgao o on o.sigla = q.orgao_sigla
join usuario u on u.email = 'professor@dev.certame.local'
where not exists (select 1 from questao ex where ex.enunciado = q.enunciado);

-- Alternativas das questões de múltipla escolha -------------------------------
insert into alternativa (questao_id, letra, texto, ordem)
select qq.id, alt.letra, alt.texto, alt.ordem
from (values
    ('PT-01', 'A', 'Fazem dois anos que o edital foi publicado.', 1),
    ('PT-01', 'B', 'Haviam muitas pessoas na sala de provas.', 2),
    ('PT-01', 'C', 'Mais de um candidato se inscreveu na mesma vaga.', 3),
    ('PT-01', 'D', 'Vende-se casas populares no bairro novo.', 4),
    ('PT-01', 'E', 'Existe, no processo, várias irregularidades formais.', 5),

    ('PT-02', 'A', 'O relator preferiu mais o voto do colega do que o seu próprio.', 1),
    ('PT-02', 'B', 'O servidor aspirava ao cargo de diretor há muitos anos.', 2),
    ('PT-02', 'C', 'Assistimos o julgamento pela televisão.', 3),
    ('PT-02', 'D', 'Obedeça o regulamento interno do órgão.', 4),
    ('PT-02', 'E', 'Todos simpatizaram-se com a nova coordenadora.', 5),

    ('PT-03', 'A', 'Os fiscais saíram às pressas depois do alerta.', 1),
    ('PT-03', 'B', 'O documento foi enviado à Vossa Senhoria ontem.', 2),
    ('PT-03', 'C', 'Ele viajou à cavalo até o distrito vizinho.', 3),
    ('PT-03', 'D', 'Estamos prontos à responder o recurso.', 4),
    ('PT-03', 'E', 'Refiro-me à qualquer servidor do quadro efetivo.', 5),

    ('PT-04', 'A', 'idéia, heróico, vôo', 1),
    ('PT-04', 'B', 'ideia, heroico, vôo', 2),
    ('PT-04', 'C', 'idéia, heroico, voo', 3),
    ('PT-04', 'D', 'ideia, heroico, voo', 4),
    ('PT-04', 'E', 'ideía, heróico, voo', 5),

    ('PT-05', 'A', 'Os servidores do setor de protocolo, receberam o comunicado.', 1),
    ('PT-05', 'B', 'O parecer, que foi assinado pelo relator, será publicado amanhã.', 2),
    ('PT-05', 'C', 'A comissão decidiu, que o prazo seria prorrogado.', 3),
    ('PT-05', 'D', 'Entregamos ao candidato, o cartão de confirmação.', 4),
    ('PT-05', 'E', 'Durante a sessão o presidente, suspendeu os trabalhos.', 5),

    ('PT-06', 'A', 'concessão', 1),
    ('PT-06', 'B', 'finalidade', 2),
    ('PT-06', 'C', 'condição', 3),
    ('PT-06', 'D', 'conformidade', 4),
    ('PT-06', 'E', 'proporção', 5),

    ('PT-07', 'A', 'advérbio e pronome indefinido', 1),
    ('PT-07', 'B', 'pronome indefinido e advérbio', 2),
    ('PT-07', 'C', 'adjetivo e advérbio', 3),
    ('PT-07', 'D', 'advérbio e adjetivo', 4),
    ('PT-07', 'E', 'pronome indefinido e adjetivo', 5),

    ('PT-08', 'A', 'Nunca disseram-me a verdade sobre o caso.', 1),
    ('PT-08', 'B', 'Nunca me disseram a verdade sobre o caso.', 2),
    ('PT-08', 'C', 'Dir-me-á o resultado assim que puder? Sim, direi-lhe.', 3),
    ('PT-08', 'D', 'Que Deus abençoe-o em sua nova função.', 4),
    ('PT-08', 'E', 'Me parece que o recurso foi indeferido.', 5)
) as alt(chave, letra, texto, ordem)
join (values
    ('PT-01', 'Assinale a alternativa em que a concordância verbal está de acordo com a norma-padrão da língua portuguesa.'),
    ('PT-02', 'Considerando a regência verbal na norma-padrão, assinale a alternativa correta.'),
    ('PT-03', 'Assinale a alternativa em que o emprego do sinal indicativo de crase está correto.'),
    ('PT-04', 'Assinale a alternativa em que todas as palavras estão corretamente acentuadas segundo o Acordo Ortográfico vigente.'),
    ('PT-05', 'Assinale a alternativa cuja pontuação está de acordo com a norma-padrão.'),
    ('PT-06', 'Leia o trecho: "Não fosse a atuação célere da equipe, o prejuízo teria sido irreparável." O trecho destacado estabelece com a oração seguinte uma relação de'),
    ('PT-07', 'Na frase "Ele chegou bastante cansado, mas resolveu bastante problemas naquela tarde", as palavras destacadas classificam-se, respectivamente, como'),
    ('PT-08', 'Assinale a alternativa em que a colocação pronominal atende à norma-padrão.')
) as mapa(chave, enunciado) on mapa.chave = alt.chave
join questao qq on qq.enunciado = mapa.enunciado
where not exists (select 1 from alternativa ja where ja.questao_id = qq.id and ja.letra = alt.letra);
