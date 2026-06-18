import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  signal,
} from '@angular/core';

export type FlashMessageType = 'error' | 'success' | 'warning' | 'info';

@Component({
  selector: 'app-flash-message',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './flash-message.component.html',
  styleUrl: './flash-message.component.css',
})
export class FlashMessageComponent implements OnChanges, OnDestroy {
  @Input() text: string | null | undefined = '';
  @Input() type: FlashMessageType = 'error';
  /** Custom margin-top utility, e.g. "mt-2" or empty string. */
  @Input() spacing = 'mt-2';
  /** Milliseconds before the message auto-dismisses. 0 disables auto-dismiss. */
  @Input() autoDismissMs = 0;
  /** Emitted when the message disappears (either by external clear or auto-dismiss). */
  @Output() dismissed = new EventEmitter<void>();

  display = signal('');
  leaving = signal(false);

  private leaveTimer: ReturnType<typeof setTimeout> | null = null;
  private autoDismissTimer: ReturnType<typeof setTimeout> | null = null;
  private static readonly LEAVE_DURATION_MS = 250;

  ngOnChanges(changes: SimpleChanges) {
    if (!changes['text']) return;

    const next = (this.text ?? '').toString();

    if (next) {
      this.cancelLeaveTimer();
      this.cancelAutoDismissTimer();
      this.leaving.set(false);
      this.display.set(next);
      this.scheduleAutoDismiss();
      return;
    }

    if (this.display()) {
      this.startLeaveAnimation();
    }
  }

  ngOnDestroy() {
    this.cancelLeaveTimer();
    this.cancelAutoDismissTimer();
  }

  private scheduleAutoDismiss() {
    if (this.autoDismissMs <= 0) return;
    this.autoDismissTimer = setTimeout(() => {
      this.startLeaveAnimation();
      this.dismissed.emit();
      this.autoDismissTimer = null;
    }, this.autoDismissMs);
  }

  private startLeaveAnimation() {
    this.cancelAutoDismissTimer();
    this.leaving.set(true);
    this.cancelLeaveTimer();
    this.leaveTimer = setTimeout(() => {
      this.display.set('');
      this.leaving.set(false);
      this.leaveTimer = null;
    }, FlashMessageComponent.LEAVE_DURATION_MS);
  }

  private cancelLeaveTimer() {
    if (this.leaveTimer !== null) {
      clearTimeout(this.leaveTimer);
      this.leaveTimer = null;
    }
  }

  private cancelAutoDismissTimer() {
    if (this.autoDismissTimer !== null) {
      clearTimeout(this.autoDismissTimer);
      this.autoDismissTimer = null;
    }
  }
}
