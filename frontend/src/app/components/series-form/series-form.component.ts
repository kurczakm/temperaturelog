import { Component, OnInit, signal, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SeriesService } from '../../services/series.service';
import { SeriesRequest } from '../../models/series.model';

@Component({
  selector: 'app-series-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './series-form.component.html',
  styleUrl: './series-form.component.scss'
})
export class SeriesFormComponent implements OnInit {
  form!: FormGroup;
  loading = signal<boolean>(false);
  error = signal<string | null>(null);
  isEditMode = signal<boolean>(false);
  seriesId: number | null = null;

  private destroyRef = inject(DestroyRef);

  availableColors = [
    { name: 'Blue', value: '#3B82F6' },
    { name: 'Red', value: '#EF4444' },
    { name: 'Green', value: '#10B981' },
    { name: 'Yellow', value: '#F59E0B' },
    { name: 'Purple', value: '#8B5CF6' },
    { name: 'Pink', value: '#EC4899' },
    { name: 'Teal', value: '#14B8A6' },
    { name: 'Orange', value: '#F97316' }
  ];

  availableIcons = ['🌡️', '📊', '📈', '🔥', '❄️', '☀️', '🌙', '⭐'];

  constructor(
    private fb: FormBuilder,
    private seriesService: SeriesService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.seriesId = parseInt(id, 10);
      this.isEditMode.set(true);
      this.loadSeries(this.seriesId);
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      description: ['', Validators.maxLength(500)],
      minValue: [0, [Validators.required]],
      maxValue: [100, [Validators.required]],
      color: ['#3B82F6', Validators.required],
      icon: ['🌡️', Validators.required]
    }, { validators: this.rangeValidator });
  }

  rangeValidator(group: FormGroup) {
    const min = group.get('minValue')?.value;
    const max = group.get('maxValue')?.value;
    return min < max ? null : { invalidRange: true };
  }

  loadSeries(id: number): void {
    this.loading.set(true);
    this.seriesService.getSeriesById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (series) => {
          this.form.patchValue({
            name: series.name,
            description: series.description || '',
            minValue: series.minValue,
            maxValue: series.maxValue,
            color: series.color,
            icon: series.icon
          });
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to load series');
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

    const request: SeriesRequest = this.form.value;

    const operation = this.isEditMode()
      ? this.seriesService.updateSeries(this.seriesId!, request)
      : this.seriesService.createSeries(request);

    operation
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.router.navigate(['/series']);
        },
        error: (err) => {
          this.error.set(err.error?.message || 'Failed to save series');
          this.loading.set(false);
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/series']);
  }

  getFieldError(fieldName: string): string | null {
    const field = this.form.get(fieldName);
    if (!field || !field.touched || !field.errors) {
      return null;
    }

    if (field.errors['required']) {
      return 'This field is required';
    }
    if (field.errors['minlength']) {
      return `Minimum length is ${field.errors['minlength'].requiredLength}`;
    }
    if (field.errors['maxlength']) {
      return `Maximum length is ${field.errors['maxlength'].requiredLength}`;
    }
    return null;
  }

  hasRangeError(): boolean {
    return this.form.errors?.['invalidRange'] &&
           (this.form.get('minValue')?.touched || this.form.get('maxValue')?.touched);
  }
}
