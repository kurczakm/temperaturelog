import { Component, OnInit, signal, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SeriesService } from '../../services/series.service';
import { SeriesResponse } from '../../models/series.model';
import { Auth } from '../../auth/auth';

@Component({
  selector: 'app-series-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './series-list.component.html',
  styleUrl: './series-list.component.scss'
})
export class SeriesListComponent implements OnInit {
  series = signal<SeriesResponse[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  private destroyRef = inject(DestroyRef);

  constructor(
    private seriesService: SeriesService,
    public auth: Auth,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadSeries();
  }

  loadSeries(): void {
    this.loading.set(true);
    this.error.set(null);

    this.seriesService.getAllSeries()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.series.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to load series');
          this.loading.set(false);
        }
      });
  }

  deleteSeries(id: number, name: string): void {
    if (!confirm(`Are you sure you want to delete series "${name}"? This will also delete all associated measurements.`)) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.seriesService.deleteSeries(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.loadSeries();
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to delete series');
          this.loading.set(false);
        }
      });
  }

  viewMeasurements(seriesId: number): void {
    this.router.navigate(['/measurements'], { queryParams: { seriesId } });
  }

  isAdmin(): boolean {
    return this.auth.currentUser()?.role === 'ADMIN';
  }
}
