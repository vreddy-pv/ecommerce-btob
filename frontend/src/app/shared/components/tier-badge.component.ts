import { Component, Input, computed } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { AccountTier } from '../../core/models/api.models';

@Component({
  selector: 'app-tier-badge',
  standalone: true,
  imports: [MatChipsModule],
  template: `
    <span class="tier-badge" [class]="tierClass()">
      {{ tierLabel() }}
    </span>
  `,
  styles: [`
    .tier-badge {
      display: inline-flex;
      align-items: center;
      padding: 4px 12px;
      border-radius: 16px;
      font-size: 12px;
      font-weight: 500;
      letter-spacing: 0.5px;
      text-transform: uppercase;
    }
    .tier-standard {
      background-color: #E0E0E0;
      color: #616161;
    }
    .tier-silver {
      background-color: #E0E0E0;
      color: #424242;
    }
    .tier-gold {
      background-color: #FFF8E1;
      color: #F57F17;
      border: 1px solid #FFD700;
    }
    .tier-platinum {
      background-color: #F3E5F5;
      color: #7B1FA2;
      border: 1px solid #CE93D8;
    }
  `],
})
export class TierBadgeComponent {
  @Input({ required: true }) tier: AccountTier = 'STANDARD';

  tierLabel = computed(() => this.tier);

  tierClass = computed(() => {
    switch (this.tier) {
      case 'STANDARD': return 'tier-standard';
      case 'SILVER': return 'tier-silver';
      case 'GOLD': return 'tier-gold';
      case 'PLATINUM': return 'tier-platinum';
      default: return 'tier-standard';
    }
  });
}
