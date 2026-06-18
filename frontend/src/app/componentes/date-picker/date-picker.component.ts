import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  computed,
  forwardRef,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

interface DayCell {
  date: Date;
  iso: string;
  day: number;
  inMonth: boolean;
  isToday: boolean;
  isSelected: boolean;
  isDisabled: boolean;
}

const WEEKDAYS = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];
const MONTHS = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];
const MONTHS_SHORT = [
  'ene', 'feb', 'mar', 'abr', 'may', 'jun',
  'jul', 'ago', 'sep', 'oct', 'nov', 'dic',
];

function toIso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function fromIso(iso: string): Date | null {
  if (!iso) return null;
  const [y, m, d] = iso.split('-').map(Number);
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d);
}

function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

@Component({
  selector: 'app-date-picker',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './date-picker.component.html',
  styleUrl: './date-picker.component.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatePickerComponent),
      multi: true,
    },
  ],
})
export class DatePickerComponent implements OnChanges, ControlValueAccessor {
  @Input() set value(v: string) { this.innerValue.set(v ?? ''); }
  get value(): string { return this.innerValue(); }

  @Input() min = '';
  @Input() max = '';
  @Input() placeholder = 'Seleccionar fecha';
  @Input() disabled = false;
  @Output() valueChange = new EventEmitter<string>();

  isOpen = signal(false);

  private innerValue = signal('');
  private viewMonth = signal<Date>(startOfDay(new Date()));

  private onChange: (v: string) => void = () => {};
  private onTouched: () => void = () => {};

  weekdays = WEEKDAYS;

  monthLabel = computed(() => {
    const d = this.viewMonth();
    return `${MONTHS[d.getMonth()]} ${d.getFullYear()}`;
  });

  days = computed<DayCell[]>(() => {
    const view = this.viewMonth();
    const year = view.getFullYear();
    const month = view.getMonth();
    const today = startOfDay(new Date());
    const selected = fromIso(this.innerValue());
    const minDate = fromIso(this.min);
    const maxDate = fromIso(this.max);

    const first = new Date(year, month, 1);
    const firstWeekday = (first.getDay() + 6) % 7; // monday-first
    const gridStart = new Date(year, month, 1 - firstWeekday);

    const cells: DayCell[] = [];
    for (let i = 0; i < 42; i++) {
      const d = new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + i);
      const t = d.getTime();
      const beforeMin = minDate !== null && t < minDate.getTime();
      const afterMax = maxDate !== null && t > maxDate.getTime();
      cells.push({
        date: d,
        iso: toIso(d),
        day: d.getDate(),
        inMonth: d.getMonth() === month,
        isToday: t === today.getTime(),
        isSelected: selected !== null && t === selected.getTime(),
        isDisabled: beforeMin || afterMax,
      });
    }
    return cells;
  });

  ngOnChanges(changes: SimpleChanges) {
    if (changes['value']) {
      const parsed = fromIso(this.innerValue());
      if (parsed) {
        this.viewMonth.set(new Date(parsed.getFullYear(), parsed.getMonth(), 1));
      }
    }
  }

  get triggerLabel(): string {
    const d = fromIso(this.innerValue());
    if (!d) return '';
    return `${String(d.getDate()).padStart(2, '0')} ${MONTHS_SHORT[d.getMonth()]} ${d.getFullYear()}`;
  }

  toggle() {
    if (this.disabled) return;
    this.isOpen.update(v => !v);
    if (this.isOpen()) {
      const parsed = fromIso(this.innerValue());
      this.viewMonth.set(parsed
        ? new Date(parsed.getFullYear(), parsed.getMonth(), 1)
        : new Date(new Date().getFullYear(), new Date().getMonth(), 1));
    } else {
      this.onTouched();
    }
  }

  prevMonth(event: MouseEvent) {
    event.stopPropagation();
    const v = this.viewMonth();
    this.viewMonth.set(new Date(v.getFullYear(), v.getMonth() - 1, 1));
  }

  nextMonth(event: MouseEvent) {
    event.stopPropagation();
    const v = this.viewMonth();
    this.viewMonth.set(new Date(v.getFullYear(), v.getMonth() + 1, 1));
  }

  prevYear(event: MouseEvent) {
    event.stopPropagation();
    const v = this.viewMonth();
    this.viewMonth.set(new Date(v.getFullYear() - 1, v.getMonth(), 1));
  }

  nextYear(event: MouseEvent) {
    event.stopPropagation();
    const v = this.viewMonth();
    this.viewMonth.set(new Date(v.getFullYear() + 1, v.getMonth(), 1));
  }

  selectDay(cell: DayCell) {
    if (cell.isDisabled) return;
    this.innerValue.set(cell.iso);
    this.onChange(cell.iso);
    this.valueChange.emit(cell.iso);
    this.isOpen.set(false);
    this.onTouched();
  }

  // ControlValueAccessor
  writeValue(v: string): void {
    this.innerValue.set(v ?? '');
  }
  registerOnChange(fn: (v: string) => void): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) this.isOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (!this.el.nativeElement.contains(event.target)) {
      if (this.isOpen()) this.onTouched();
      this.isOpen.set(false);
    }
  }

  constructor(private el: ElementRef) {}
}
