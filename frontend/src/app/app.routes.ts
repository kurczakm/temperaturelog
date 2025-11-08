import { Routes } from '@angular/router';
import { Signin } from './auth/signin/signin';

export const routes: Routes = [
  { path: '', redirectTo: '/signin', pathMatch: 'full' },
  { path: 'signin', component: Signin }
];
