Quero que adicione a essa aplicação a possibilidade de chamar uma api de questões de concurso. 
Existem mais de uma opção para isso, então crie uma interface para que se necessário possamos usar mais de uma api ao mesmo tempo. 
Ao chamar essa API, a questão deve ser cadastrada no banco de dados da aplicação.
Antes de salvar precisamos verificar se a entidade questão criada a partir da chamada é totalmente compatível com as questões que existem na aplicação. 
Essas chamadas não serão realizadas por alunos, mas sim por adm e super adm. 
Essa funcionalidade servirá para não precisarmos cadastrar todas as questões à mão.
Verificar se a banca, orgão... já estão cadastrados, caso ainda não, antes de cadastrar a questão é necessário cadastrar o orgão/banca
O adm/superadm deve fazer uma solicitação/ um pedido, por exemplo, buscar 10 questãoes de português difíceis. Ao retornar essas 10 questões elas ficam disponíveis para a validação. A cada pedido precisamos trazer questões que ainda não existam no banco de dados da app certame.
Ao fazer um pedido para a API externa, validar se alguma das questões que retornou, já não exista no banco de dados, se já existir ela deve ser ignorada e outra no lugar deve ser buscada na API 