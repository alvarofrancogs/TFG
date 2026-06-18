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
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';

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
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DatePickerComponent),
      multi: true,
    },
  ],
})
export class DatePickerComponent implements OnChanges, ControlValueAccessor, Validator {
  @Input() set value(v: string) { this.innerValue.set(v ?? ''); }
  get value(): string { return this.innerValue(); }

  @Input() min = '';
  @Input() max = '';
  @Input() placeholder = 'Seleccionar fecha';
  @Input() disabled = false;
  /** Forwarded to the trigger button. Lets a `<label for="...">` reference this picker. */
  @Input() inputId: string | null = null;
 
  @Input() validateOnly = false;
  @Output() valueChange = new EventEmitter<string>();

  isOpen = signal(false);

  private innerValue = signal('');
  private viewMonth = signal<Date>(startOfDay(new Date()));

  private onChange: (v: string) => void = () => undefined;
  private onTouched: () => void = () => undefined;
  private validatorOnChange: () => void = () => undefined;

  weekdays = WEEKDAYS;

  private el = inject(ElementRef);

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
    const allowVisualDisable = !this.validateOnly;

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
        isDisabled: allowVisualDisable && (beforeMin || afterMax),
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
    if (changes['min'] || changes['max']) {
      this.validatorOnChange();
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
      this.viewMonth.set(this.computeOpenMonth());
    } else {
      this.onTouched();
    }
  }

  private computeOpenMonth(): Date {
    const parsed = fromIso(this.innerValue());
    if (parsed) return new Date(parsed.getFullYear(), parsed.getMonth(), 1);
    const maxDate = fromIso(this.max);
    if (maxDate) return new Date(maxDate.getFullYear(), maxDate.getMonth(), 1);
    const minDate = fromIso(this.min);
    if (minDate) return new Date(minDate.getFullYear(), minDate.getMonth(), 1);
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth(), 1);
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

  validate(control: AbstractControl): ValidationErrors | null {
    const raw = control.value;
    if (!raw) return null;
    const value = typeof raw === 'string' ? fromIso(raw) : null;
    if (!value) return null;

    const minDate = fromIso(this.min);
    if (minDate && value.getTime() < minDate.getTime()) {
      return { min: { min: this.min, actual: raw } };
    }
    const maxDate = fromIso(this.max);
    if (maxDate && value.getTime() > maxDate.getTime()) {
      return { max: { max: this.max, actual: raw } };
    }
    return null;
  }

  registerOnValidatorChange(fn: () => void): void {
    this.validatorOnChange = fn;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (!this.el.nativeElement.contains(event.target)) {
      if (this.isOpen()) this.onTouched();
      this.isOpen.set(false);
    }
  }
}
