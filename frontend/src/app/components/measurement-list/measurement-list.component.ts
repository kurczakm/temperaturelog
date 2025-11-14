import { Component, OnInit, signal, DestroyRef, inject, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
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
  styleUrl: './measurement-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MeasurementListComponent implements OnInit {
  measurements = signal<MeasurementResponse[]>([]);
  series = signal<SeriesResponse | null>(null);
  allSeries = signal<SeriesResponse[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);
  seriesId: number | null = null;

  // Computed signal for series lookup
  seriesMap = computed(() => {
    const map = new Map<number, SeriesResponse>();
    this.allSeries().forEach(s => map.set(s.id, s));
    return map;
  });

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
          // Use string comparison for ISO 8601 timestamps (more efficient)
          this.measurements.set(data.sort((a, b) =>
            b.timestamp.localeCompare(a.timestamp)
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

    // Load both measurements and all series for ID resolution
    // Use catchError to handle partial failures gracefully
    forkJoin({
      measurements: this.measurementService.getAllMeasurements().pipe(
        catchError(err => {
          console.error('Failed to load measurements:', err);
          this.error.set('Failed to load measurements');
          return of([]);
        })
      ),
      series: this.seriesService.getAllSeries().pipe(
        catchError(err => {
          console.error('Failed to load series metadata:', err);
          return of([]); // Continue even if series fails
        })
      )
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ measurements, series }) => {
          // Set series FIRST to ensure seriesMap is ready before measurements
          this.allSeries.set(series);

          // Use string comparison for ISO 8601 timestamps (more efficient)
          this.measurements.set(measurements.sort((a, b) =>
            b.timestamp.localeCompare(a.timestamp)
          ));

          this.loading.set(false);
        },
        error: (err) => {
          // Fallback error handler (rarely called due to catchError)
          this.error.set('An unexpected error occurred');
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

  /**
   * Retrieves series information by ID from the cached series map.
   * Only available when viewing all measurements (not filtered by series).
   * @param seriesId The ID of the series to retrieve
   * @returns The series object if found, undefined otherwise
   */
  getSeriesById(seriesId: number): SeriesResponse | undefined {
    return this.seriesMap().get(seriesId);
  }
}
