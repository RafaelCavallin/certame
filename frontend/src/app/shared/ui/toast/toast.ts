import { Component, inject } from '@angular/core';
import { ToastStore } from './toast-store';

@Component({
  selector: 'app-toast',
  templateUrl: './toast.html',
  styleUrl: './toast.scss',
})
export class Toast {
  protected readonly store = inject(ToastStore);
}
