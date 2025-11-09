import { Component, OnInit, signal, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MeasurementService } from '../../services/measurement.service';
import { SeriesService } from '../../services/series.service';
import { MeasurementRequest } from '../../models/measurement.model';
import { SeriesResponse } from '../../models/series.model';

@Component({
  selector: 'app-measurement-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './measurement-form.component.html',
  styleUrl: './measurement-form.component.scss'
})
export class MeasurementFormComponent implements OnInit {
  form!: FormGroup;
  loading = signal<boolean>(false);
  error = signal<string | null>(null);
  isEditMode = signal<boolean>(false);
  measurementId: number | null = null;
  allSeries = signal<SeriesResponse[]>([]);
  selectedSeries = signal<SeriesResponse | null>(null);

  private destroyRef = inject(DestroyRef);

  constructor(
    private fb: FormBuilder,
    private measurementService: MeasurementService,
    private seriesService: SeriesService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadAllSeries();

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.measurementId = parseInt(id, 10);
      this.isEditMode.set(true);
      this.loadMeasurement(this.measurementId);
    } else {
      const seriesId = this.route.snapshot.queryParamMap.get('seriesId');
      if (seriesId) {
        this.form.patchValue({ seriesId: parseInt(seriesId, 10) });
        this.onSeriesChange(parseInt(seriesId, 10));
      }
    }
  }

  initForm(): void {
    const now = new Date();
    const localDateTime = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16);

    this.form = this.fb.group({
      seriesId: ['', Validators.required],
      value: ['', [Validators.required]],
      timestamp: [localDateTime, Validators.required]
    });

    this.form.get('seriesId')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(seriesId => {
        if (seriesId) {
          this.onSeriesChange(parseInt(seriesId, 10));
        }
      });
  }

  loadAllSeries(): void {
    this.seriesService.getAllSeries()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.allSeries.set(data);
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to load series');
        }
      });
  }

  onSeriesChange(seriesId: number): void {
    const series = this.allSeries().find(s => s.id === seriesId);
    this.selectedSeries.set(series || null);
  }

  loadMeasurement(id: number): void {
    this.loading.set(true);
    this.measurementService.getMeasurementById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (measurement) => {
          const localDateTime = new Date(measurement.timestamp)
            .toISOString()
            .slice(0, 16);

          this.form.patchValue({
            seriesId: measurement.seriesId,
            value: measurement.value,
            timestamp: localDateTime
          });
          this.onSeriesChange(measurement.seriesId);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to load measurement');
          this.loading.set(false);
        }
      });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const formValue = this.form.value;
    const request: MeasurementRequest = {
      seriesId: parseInt(formValue.seriesId, 10),
      value: parseFloat(formValue.value),
      timestamp: new Date(formValue.timestamp).toISOString()
    };

    const operation = this.isEditMode()
      ? this.measurementService.updateMeasurement(this.measurementId!, request)
      : this.measurementService.createMeasurement(request);

    operation
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.router.navigate(['/measurements'], {
            queryParams: { seriesId: request.seriesId }
          });
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to save measurement');
          this.loading.set(false);
        }
      });
  }

  cancel(): void {
    const seriesId = this.form.get('seriesId')?.value;
    if (seriesId) {
      this.router.navigate(['/measurements'], { queryParams: { seriesId } });
    } else {
      this.router.navigate(['/measurements']);
    }
  }

  getFieldError(fieldName: string): string | null {
    const field = this.form.get(fieldName);
    if (!field || !field.touched || !field.errors) {
      return null;
    }

    if (field.errors['required']) {
      return 'This field is required';
    }
    return null;
  }

  isValueOutOfRange(): boolean {
    const value = this.form.get('value')?.value;
    const series = this.selectedSeries();

    if (!value || !series) {
      return false;
    }

    const numValue = parseFloat(value);
    return numValue < series.minValue || numValue > series.maxValue;
  }

  getValueStatus(): string | null {
    const value = this.form.get('value')?.value;
    const series = this.selectedSeries();

    if (!value || !series) {
      return null;
    }

    const numValue = parseFloat(value);
    if (numValue < series.minValue) {
      return `Below minimum (${series.minValue}°C)`;
    }
    if (numValue > series.maxValue) {
      return `Above maximum (${series.maxValue}°C)`;
    }
    return 'Within valid range';
  }
}
