import { Routes } from '@angular/router';
import { Signin } from './auth/signin/signin';
import { SeriesListComponent } from './components/series-list/series-list.component';
import { SeriesFormComponent } from './components/series-form/series-form.component';
import { SeriesChart } from './components/series-chart/series-chart';
import { MeasurementListComponent } from './components/measurement-list/measurement-list.component';
import { MeasurementFormComponent } from './components/measurement-form/measurement-form.component';
import { authGuard, adminGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/series', pathMatch: 'full' },
  { path: 'signin', component: Signin },
  { path: 'series', component: SeriesListComponent, canActivate: [authGuard] },
  { path: 'series/new', component: SeriesFormComponent, canActivate: [adminGuard] },
  { path: 'series/edit/:id', component: SeriesFormComponent, canActivate: [adminGuard] },
  { path: 'chart', component: SeriesChart, canActivate: [authGuard] },
  { path: 'measurements', component: MeasurementListComponent, canActivate: [authGuard] },
  { path: 'measurements/new', component: MeasurementFormComponent, canActivate: [adminGuard] },
  { path: 'measurements/edit/:id', component: MeasurementFormComponent, canActivate: [adminGuard] }
];
