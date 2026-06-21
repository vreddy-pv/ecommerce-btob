import { Routes } from '@angular/router';
import { OrdersComponent } from './orders.component';
import { OrderDetailComponent } from './order-detail.component';

export const ordersRoutes: Routes = [
  { path: '', component: OrdersComponent },
  { path: ':id', component: OrderDetailComponent },
];
