import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, firstValueFrom } from 'rxjs';

import { RevisaoApi } from '../../core/api/revisao-api';
import { AuthStore } from '../../core/auth/auth-store';
import { Cronometro } from '../../features/study/cronometro';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class ShellPage {
  readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly revisaoApi = inject(RevisaoApi);
  readonly cronometro = inject(Cronometro);
  readonly emEstudo = signal(this.router.url.startsWith('/estudar'));
  /** O cronômetro só existe dentro de uma sessão — não no painel de filtros. */
  readonly emSessao = computed(() => this.emEstudo() && this.cronometro.ativo());
  readonly flashcardsVencendoHoje = signal(0);
  private static readonly TITULOS: Record<string, [string, string]> = {
    inicio: ['Início', 'Continue seus estudos'],
    cadernos: ['Cadernos', 'Organize suas questões'],
    editais: ['Editais', 'Editais que você acompanha'],
    desempenho: ['Desempenho', 'Acompanhe sua evolução'],
    simulados: ['Simulados', 'Provas completas e resultados'],
    flashcards: ['Flashcards', 'Revise no seu ritmo'],
    admin: ['Administração', 'Gestão de conteúdo'],
    super: ['Usuários', 'Gestão de contas'],
  };
  readonly rotaAtual = signal(this.router.url);
  readonly titulo = computed(() => this.tituloPara(this.rotaAtual())[0]);
  readonly subtitulo = computed(() => this.tituloPara(this.rotaAtual())[1]);
  constructor() {
    this.router.events
      .pipe(filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd))
      .subscribe((evento) => {
        this.emEstudo.set(evento.urlAfterRedirects.startsWith('/estudar'));
        this.rotaAtual.set(evento.urlAfterRedirects);
      });
    void firstValueFrom(this.revisaoApi.resumo())
      .then((resumo) => this.flashcardsVencendoHoje.set(resumo.vencendoHoje))
      .catch(() => this.flashcardsVencendoHoje.set(0));
  }
  private tituloPara(url: string): [string, string] {
    const segmento = url.split('/').filter(Boolean)[0] ?? 'inicio';
    return ShellPage.TITULOS[segmento] ?? ShellPage.TITULOS['inicio'];
  }

  tempo(): string {
    const segundos = this.cronometro.segundos();
    return `${String(Math.floor(segundos / 60)).padStart(2, '0')}:${String(segundos % 60).padStart(2, '0')}`;
  }

  /** Leitura por extenso do relógio, para quem navega por leitor de tela. */
  rotuloTempo(): string {
    const segundos = this.cronometro.segundos();
    const estado = this.cronometro.pausado() ? 'Cronômetro pausado' : 'Tempo de sessão';
    return `${estado}: ${Math.floor(segundos / 60)} minutos e ${segundos % 60} segundos`;
  }

  async sair(): Promise<void> {
    await this.auth.sair();
    await this.router.navigateByUrl('/login');
  }

  podeAdministrar(): boolean {
    const perfil = this.auth.perfil();
    return perfil === 'PROFESSOR' || perfil === 'ADMIN' || perfil === 'SUPER_ADMIN';
  }
  podeCatalogo(): boolean {
    const perfil = this.auth.perfil();
    return perfil === 'ADMIN' || perfil === 'SUPER_ADMIN';
  }
  podeGerenciarEditais(): boolean {
    return this.podeCatalogo();
  }
  podeSuper(): boolean {
    return this.auth.perfil() === 'SUPER_ADMIN';
  }
}
