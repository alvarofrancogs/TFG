import {
  Component,
  ElementRef,
  HostListener,
  Input,
  Output,
  EventEmitter,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';

export interface DropdownOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-dropdown',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dropdown.component.html',
  styleUrl: './dropdown.component.css',
})
export class DropdownComponent {
  @Input() options: DropdownOption[] = [];
  @Input() placeholder = 'Selecciona una opción';
  @Input() value = '';
  @Output() valueChange = new EventEmitter<string>();

  isOpen = signal(false);

  get selectedLabel(): string {
    return this.options.find(o => o.value === this.value)?.label ?? '';
  }

  toggle() {
    this.isOpen.update(v => !v);
  }

  select(option: DropdownOption) {
    this.valueChange.emit(option.value);
    this.isOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (!this.el.nativeElement.contains(event.target)) {
      this.isOpen.set(false);
    }
  }

  constructor(private el: ElementRef) {}
}
