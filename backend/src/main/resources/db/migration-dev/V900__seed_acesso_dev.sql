-- Senha de todos os usuários: senhaDev123
insert into usuario (nome, email, senha_hash, perfil, status)
values
  ('Estudante Dev', 'estudante@dev.certame.local', '$2y$12$l7QTILrclN6HeK7YQvBP8ukITXmtv3hiFjb5cMd/WjD3kSGWqYSne', 'ESTUDANTE', 'ATIVO'),
  ('Professor Dev', 'professor@dev.certame.local', '$2y$12$l7QTILrclN6HeK7YQvBP8ukITXmtv3hiFjb5cMd/WjD3kSGWqYSne', 'PROFESSOR', 'ATIVO'),
  ('Administrador Dev', 'admin@dev.certame.local', '$2y$12$l7QTILrclN6HeK7YQvBP8ukITXmtv3hiFjb5cMd/WjD3kSGWqYSne', 'ADMIN', 'ATIVO'),
  ('Super Administrador Dev', 'superadmin@dev.certame.local', '$2y$12$l7QTILrclN6HeK7YQvBP8ukITXmtv3hiFjb5cMd/WjD3kSGWqYSne', 'SUPER_ADMIN', 'ATIVO')
on conflict (email) where excluido_em is null do nothing;
