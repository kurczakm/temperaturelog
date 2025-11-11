import { Component, OnInit, signal, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MeasurementService } from '../../services/measurement.service';
import { SeriesService } from '../../services/series.service';
import { MeasurementResponse } from '../../models/measurement.model';
import { SeriesResponse } from '../../models/series.model';
import { Auth } from '../../auth/auth';

@Component({
  selector: 'app-measurement-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './measurement-list.component.html',
  styleUrl: './measurement-list.component.scss'
})
export class MeasurementListComponent implements OnInit {
  measurements = signal<MeasurementResponse[]>([]);
  series = signal<SeriesResponse | null>(null);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);
  seriesId: number | null = null;

  private destroyRef = inject(DestroyRef);

  constructor(
    private measurementService: MeasurementService,
    private seriesService: SeriesService,
    public auth: Auth,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        const id = params['seriesId'];
        if (id) {
          this.seriesId = parseInt(id, 10);
          this.loadSeriesInfo(this.seriesId);
          this.loadMeasurements(this.seriesId);
        } else {
          this.loadAllMeasurements();
        }
      });
  }

  loadSeriesInfo(seriesId: number): void {
    this.seriesService.getSeriesById(seriesId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.series.set(data);
        },
        error: (err) => {
          console.error('Failed to load series info:', err);
        }
      });
  }

  loadMeasurements(seriesId: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.measurementService.getMeasurementsBySeriesId(seriesId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.measurements.set(data.sort((a, b) =>
            new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
          ));
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to load measurements');
          this.loading.set(false);
        }
      });
  }

  loadAllMeasurements(): void {
    this.loading.set(true);
    this.error.set(null);

    this.measurementService.getAllMeasurements()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.measurements.set(data.sort((a, b) =>
            new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
          ));
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to load measurements');
          this.loading.set(false);
        }
      });
  }

  deleteMeasurement(id: number): void {
    if (!confirm('Are you sure you want to delete this measurement?')) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.measurementService.deleteMeasurement(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          if (this.seriesId) {
            this.loadMeasurements(this.seriesId);
          } else {
            this.loadAllMeasurements();
          }
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to delete measurement');
          this.loading.set(false);
        }
      });
  }

  isAdmin(): boolean {
    return this.auth.currentUser()?.role === 'ADMIN';
  }
}
