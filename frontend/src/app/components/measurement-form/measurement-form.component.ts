import { Component, OnInit, signal, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, ValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
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

  // Validator methods defined as properties to be available in constructor
  private numberValidator = (): ValidatorFn => {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }
      const value = parseFloat(control.value);
      return isNaN(value) ? { invalidNumber: true } : null;
    };
  };

  private rangeValidator = (min: number, max: number): ValidatorFn => {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null; // Don't validate empty values (handled by required validator)
      }

      const value = parseFloat(control.value);
      if (isNaN(value)) {
        return null; // Don't validate non-numeric values
      }

      if (value < min) {
        return { belowMin: { min, actual: value } };
      }

      if (value > max) {
        return { aboveMax: { max, actual: value } };
      }

      return null;
    };
  };

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
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.measurementId = parseInt(id, 10);
      this.isEditMode.set(true);
      this.loadMeasurement(this.measurementId);
    } else {
      const seriesId = this.route.snapshot.queryParamMap.get('seriesId');
      if (seriesId) {
        this.form.patchValue({ seriesId: parseInt(seriesId, 10) });
      }
    }

    this.loadAllSeries();
  }

  initForm(): void {
    const now = new Date();
    const localDateTime = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16);

    this.form = this.fb.group({
      seriesId: ['', Validators.required],
      value: ['', [Validators.required, this.numberValidator()]],
      timestamp: [localDateTime, Validators.required]
    });

    this.form.get('seriesId')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(seriesId => {
        if (seriesId) {
          this.onSeriesChange(parseInt(seriesId, 10));
        }
      });

    // Clear error when form changes
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.error.set(null));
  }

  loadAllSeries(): void {
    this.seriesService.getAllSeries()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.allSeries.set(data);
          // Apply validators after series are loaded
          const currentSeriesId = this.form.get('seriesId')?.value;
          if (currentSeriesId) {
            this.onSeriesChange(parseInt(currentSeriesId, 10));
          }
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to load series');
        }
      });
  }

  onSeriesChange(seriesId: number): void {
    const series = this.allSeries().find(s => s.id === seriesId);
    this.selectedSeries.set(series || null);

    // Update value field validators when series changes
    if (series) {
      this.updateValueValidators(series.minValue, series.maxValue);
    }
  }

  private updateValueValidators(minValue: number, maxValue: number): void {
    const valueControl = this.form.get('value');
    if (valueControl) {
      valueControl.setValidators([
        Validators.required,
        this.numberValidator(),
        this.rangeValidator(minValue, maxValue)
      ]);
      valueControl.updateValueAndValidity();

      // In edit mode, mark as touched to show errors immediately
      if (this.isEditMode() && valueControl.value) {
        valueControl.markAsTouched();
      }
    }
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
          // onSeriesChange will be called automatically after series are loaded
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

    if (field.errors['invalidNumber']) {
      return 'Please enter a valid number';
    }

    if (field.errors['belowMin']) {
      const min = field.errors['belowMin'].min;
      return `Value must be at least ${min}°C`;
    }

    if (field.errors['aboveMax']) {
      const max = field.errors['aboveMax'].max;
      return `Value must not exceed ${max}°C`;
    }

    return null;
  }

  isValueOutOfRange(): boolean {
    const valueControl = this.form.get('value');
    return !!(valueControl?.errors?.['belowMin'] || valueControl?.errors?.['aboveMax']);
  }

  getValueStatus(): string | null {
    const valueControl = this.form.get('value');
    const series = this.selectedSeries();

    if (!valueControl?.value || !series) {
      return null;
    }

    // Leverage existing validation errors
    if (valueControl.errors?.['belowMin']) {
      const min = valueControl.errors['belowMin'].min;
      return `Below minimum (${min}°C)`;
    }

    if (valueControl.errors?.['aboveMax']) {
      const max = valueControl.errors['aboveMax'].max;
      return `Above maximum (${max}°C)`;
    }

    // Only show success status if value is valid and not empty
    const numValue = parseFloat(valueControl.value);
    if (!isNaN(numValue)) {
      return 'Within valid range';
    }

    return null;
  }
}
