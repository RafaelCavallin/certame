import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Dialog } from './shared/ui/dialog/dialog';
import { Toast } from './shared/ui/toast/toast';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Dialog, Toast],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly title = signal('certame');
}
