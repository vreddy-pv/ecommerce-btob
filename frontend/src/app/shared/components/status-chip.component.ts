import { Component, input, computed } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { OrderStatus } from '../../core/models/api.models';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  imports: [MatChipsModule],
  template: `
    <span class="status-chip" [class]="chipClass()" [attr.aria-label]="'Order status: ' + status()">
      {{ status() }}
    </span>
  `,
  styles: [`
    .status-chip {
      display: inline-flex;
      align-items: center;
      padding: 4px 12px;
      border-radius: 16px;
      font-size: 12px;
      font-weight: 500;
      line-height: 1.4;
      color: #FFFFFF;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .status-pending {
      background-color: #F9A825;
    }
    .status-confirmed {
      background-color: #1976D2;
    }
    .status-shipped {
      background-color: #1976D2;
    }
    .status-delivered {
      background-color: #388E3C;
    }
  `],
})
export class StatusChipComponent {
  status = input.required<OrderStatus>();

  chipClass = computed(() => {
    switch (this.status()) {
      case 'PENDING':   return 'status-pending';
      case 'CONFIRMED': return 'status-confirmed';
      case 'SHIPPED':   return 'status-shipped';
      case 'DELIVERED': return 'status-delivered';
      default:          return 'status-pending';
    }
  });
}
