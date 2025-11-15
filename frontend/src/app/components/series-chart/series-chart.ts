import { Component, DestroyRef, inject, OnInit, OnDestroy, signal, computed, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BaseChartDirective } from 'ng2-charts';
import { Chart, ChartConfiguration, ChartOptions, ChartType, registerables } from 'chart.js';
import 'chartjs-adapter-date-fns';
import { forkJoin, of } from 'rxjs';
import { switchMap, map } from 'rxjs/operators';

import { SeriesService } from '../../services/series.service';
import { MeasurementService } from '../../services/measurement.service';
import { SeriesResponse } from '../../models/series.model';
import { MeasurementResponse } from '../../models/measurement.model';
import { Auth } from '../../auth/auth';

// Register Chart.js components
Chart.register(...registerables);

interface SeriesSelection {
  series: SeriesResponse;
  selected: boolean;
  measurements: MeasurementResponse[];
}

interface TableMeasurement extends MeasurementResponse {
  seriesName: string;
  seriesIcon: string;
  seriesColor: string;
}

type TimePeriod = '24h' | '7d' | '30d' | 'all' | 'custom';

const HIGHLIGHT_STYLES = {
  pointRadius: 10,
  normalPointRadius: 4,
  pointBackgroundColor: '#FFD700', // Gold
  pointBorderColor: '#FF6B00', // Orange
  pointBorderWidth: 3,
  normalBorderWidth: 1,
  backgroundTransparency: '33', // 20% opacity
} as const;

@Component({
  selector: 'app-series-chart',
  imports: [CommonModule, FormsModule, BaseChartDirective],
  templateUrl: './series-chart.html',
  styleUrl: './series-chart.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeriesChart implements OnInit, OnDestroy {
  private seriesService = inject(SeriesService);
  private measurementService = inject(MeasurementService);
  private destroyRef = inject(DestroyRef);
  private router = inject(Router);

  auth = inject(Auth);

  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  seriesSelections = signal<SeriesSelection[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  highlightedMeasurement = signal<TableMeasurement | null>(null);

  selectedPeriod = signal<TimePeriod>('7d');
  customStartDate = signal<string>('');
  customEndDate = signal<string>('');
  currentDate = signal(new Date());

  // Computed signals for reactive state
  hasSelectedSeries = computed(() =>
    this.seriesSelections().some((s) => s.selected)
  );

  selectedCount = computed(() =>
    this.seriesSelections().filter((s) => s.selected).length
  );

  tableMeasurements = computed(() => {
    const selections = this.seriesSelections();
    const selectedSelections = selections.filter(s => s.selected);

    const measurements: TableMeasurement[] = [];
    selectedSelections.forEach(selection => {
      const filtered = this.getFilteredMeasurements(selection.measurements);
      filtered.forEach(m => {
        measurements.push({
          ...m,
          seriesName: selection.series.name,
          seriesIcon: selection.series.icon,
          seriesColor: selection.series.color,
        });
      });
    });

    // Sort by timestamp descending (newest first)
    return measurements.sort((a, b) =>
      new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    );
  });

  // Chart configuration with improved typing
  public readonly lineChartType = 'line' as const;
  public lineChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    layout: {
      padding: {
        top: 20,
        bottom: 10,
        left: 10,
        right: 10,
      },
    },
    scales: {
      x: {
        type: 'time',
        time: {
          unit: 'hour',
          displayFormats: {
            hour: 'MMM d, HH:mm',
            day: 'MMM d',
          },
        },
        title: {
          display: true,
          text: 'Time',
        },
      },
      y: {
        title: {
          display: true,
          text: 'Temperature Value',
        },
        grace: '5%',
      },
    },
    plugins: {
      legend: {
        display: true,
        position: 'top',
      },
      tooltip: {
        mode: 'index',
        intersect: false,
      },
    },
    interaction: {
      mode: 'nearest',
      axis: 'x',
      intersect: false,
    },
  };

  public lineChartData: ChartConfiguration<'line'>['data'] = {
    datasets: [],
  };

  ngOnInit(): void {
    this.loadData();
  }

  ngOnDestroy(): void {
    // Explicitly destroy chart instance to prevent memory leaks
    if (this.chart?.chart) {
      this.chart.chart.destroy();
    }
  }

  private loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    this.seriesService
      .getAllSeries()
      .pipe(
        switchMap((series) => {
          if (series.length === 0) {
            return of([]);
          }

          const measurementRequests = series.map((s) =>
            this.measurementService.getMeasurementsBySeriesId(s.id).pipe(
              map((measurements) => ({
                series: s,
                selected: false,
                measurements: this.sortMeasurements(measurements),
              }))
            )
          );

          return forkJoin(measurementRequests);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (selections) => {
          this.seriesSelections.set(selections);
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Error loading data:', err);
          this.error.set('Failed to load series and measurements');
          this.loading.set(false);
        },
      });
  }

  private sortMeasurements(measurements: MeasurementResponse[]): MeasurementResponse[] {
    return [...measurements].sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );
  }

  private getFilteredMeasurements(measurements: MeasurementResponse[]): MeasurementResponse[] {
    const now = new Date();
    let startDate: Date;

    switch (this.selectedPeriod()) {
      case '24h':
        startDate = new Date(now.getTime() - 24 * 60 * 60 * 1000);
        break;
      case '7d':
        startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
        break;
      case '30d':
        startDate = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
        break;
      case 'custom':
        if (this.customStartDate() && this.customEndDate()) {
          startDate = new Date(this.customStartDate());
          const endDate = new Date(this.customEndDate());
          return measurements.filter((m) => {
            const timestamp = new Date(m.timestamp);
            return timestamp >= startDate && timestamp <= endDate;
          });
        }
        startDate = new Date(0);
        break;
      case 'all':
      default:
        startDate = new Date(0);
        break;
    }

    return measurements.filter((m) => {
      const timestamp = new Date(m.timestamp);
      return timestamp >= startDate;
    });
  }

  onSeriesSelectionChange(): void {
    // Force signal update to trigger computed signals like tableMeasurements
    this.seriesSelections.set([...this.seriesSelections()]);
    this.highlightedMeasurement.set(null);
    this.updateChart();
  }

  onPeriodChange(): void {
    this.highlightedMeasurement.set(null);
    this.updateChart();
  }

  onCustomDateChange(): void {
    if (this.selectedPeriod() !== 'custom') {
      this.error.set(null);
      return;
    }

    const start = this.customStartDate();
    const end = this.customEndDate();

    if (!start || !end) {
      this.error.set(null);
      return;
    }

    const startDate = new Date(start);
    const endDate = new Date(end);

    if (startDate >= endDate) {
      this.error.set('End date must be after start date');
      return;
    }

    if (endDate > new Date()) {
      this.error.set('End date cannot be in the future');
      return;
    }

    this.error.set(null);
    this.highlightedMeasurement.set(null);
    this.updateChart();
  }

  selectAllSeries(): void {
    this.seriesSelections.update(selections =>
      selections.map(s => ({ ...s, selected: true }))
    );
    this.highlightedMeasurement.set(null);
    this.updateChart();
  }

  deselectAllSeries(): void {
    this.seriesSelections.update(selections =>
      selections.map(s => ({ ...s, selected: false }))
    );
    this.highlightedMeasurement.set(null);
    this.updateChart();
  }

  highlightMeasurement(measurement: TableMeasurement): void {
    this.highlightedMeasurement.set(measurement);
    this.updateChart();
  }

  isHighlighted(measurement: TableMeasurement): boolean {
    const highlighted = this.highlightedMeasurement();
    return highlighted !== null &&
           highlighted.id === measurement.id &&
           highlighted.seriesId === measurement.seriesId;
  }

  clearHighlight(): void {
    this.highlightedMeasurement.set(null);
    this.updateChart();
  }

  private updateChart(): void {
    try {
      const selections = this.seriesSelections();
      const selectedSelections = selections.filter((s) => s.selected);

      if (selectedSelections.length === 0) {
        this.lineChartData = {
          datasets: [],
        };
        this.chart?.update();
        return;
      }

      this.lineChartData = {
        datasets: selectedSelections.map((selection) => {
          const filteredMeasurements = this.getFilteredMeasurements(selection.measurements);
          return this.createDataset(selection, filteredMeasurements);
        }),
      };

      this.chart?.update();
    } catch (err) {
      console.error('Error updating chart:', err);
      this.error.set('Failed to render chart. Please try again.');
    }
  }

  private createDataset(
    selection: SeriesSelection,
    measurements: MeasurementResponse[]
  ) {
    const highlighted = this.highlightedMeasurement();

    // Single pass through measurements for optimal performance - O(n) instead of O(4n)
    const data: Array<{x: number; y: number}> = [];
    const pointRadius: number[] = [];
    const pointBackgroundColor: string[] = [];
    const pointBorderColor: string[] = [];
    const pointBorderWidth: number[] = [];

    for (const m of measurements) {
      const isHighlighted = this.isHighlightedMeasurement(m, highlighted);

      data.push({
        x: new Date(m.timestamp).getTime(),
        y: m.value,
      });

      pointRadius.push(isHighlighted ? HIGHLIGHT_STYLES.pointRadius : HIGHLIGHT_STYLES.normalPointRadius);
      pointBackgroundColor.push(isHighlighted ? HIGHLIGHT_STYLES.pointBackgroundColor : selection.series.color);
      pointBorderColor.push(isHighlighted ? HIGHLIGHT_STYLES.pointBorderColor : selection.series.color);
      pointBorderWidth.push(isHighlighted ? HIGHLIGHT_STYLES.pointBorderWidth : HIGHLIGHT_STYLES.normalBorderWidth);
    }

    return {
      label: `${selection.series.icon} ${selection.series.name}`,
      data,
      borderColor: selection.series.color,
      backgroundColor: `${selection.series.color}${HIGHLIGHT_STYLES.backgroundTransparency}`,
      tension: 0.1,
      pointRadius,
      pointHoverRadius: 6,
      pointBackgroundColor,
      pointBorderColor,
      pointBorderWidth,
    };
  }

  private isHighlightedMeasurement(
    measurement: MeasurementResponse,
    highlighted: TableMeasurement | null
  ): boolean {
    return highlighted !== null &&
           measurement.id === highlighted.id &&
           measurement.seriesId === highlighted.seriesId;
  }

  printChart(): void {
    // Update current date before printing
    this.currentDate.set(new Date());

    // Clear any highlights for cleaner print output
    const currentHighlight = this.highlightedMeasurement();
    this.highlightedMeasurement.set(null);

    // First update to clear highlights
    this.updateChart();

    // Wait for the chart to fully update with cleared highlights
    setTimeout(() => {
      if (this.chart?.chart) {
        // Destroy and recreate the chart data to ensure fresh rendering
        this.chart.chart.data = this.lineChartData;

        // Force chart to resize for print dimensions
        this.chart.chart.resize();

        // Force a complete update with animation disabled
        this.chart.chart.update('none');
      }

      // Longer delay to ensure chart is fully rendered with correct values
      setTimeout(() => {
        // One final update right before printing to ensure correct data
        if (this.chart?.chart) {
          this.chart.chart.update('none');
        }

        // Small delay before opening print dialog
        setTimeout(() => {
          window.print();

          // Restore highlight and chart after printing
          setTimeout(() => {
            if (currentHighlight) {
              this.highlightedMeasurement.set(currentHighlight);
            }

            // Resize and update chart back to normal view
            if (this.chart?.chart) {
              this.chart.chart.resize();
              this.chart.chart.update('none');
            }
          }, 150);
        }, 100);
      }, 400);
    }, 400);
  }

  getPeriodLabel(): string {
    switch (this.selectedPeriod()) {
      case '24h':
        return 'Last 24 Hours';
      case '7d':
        return 'Last 7 Days';
      case '30d':
        return 'Last 30 Days';
      case 'all':
        return 'All Time';
      case 'custom':
        if (this.customStartDate() && this.customEndDate()) {
          const start = new Date(this.customStartDate()).toLocaleString();
          const end = new Date(this.customEndDate()).toLocaleString();
          return `Custom Range: ${start} to ${end}`;
        }
        return 'Custom Range';
      default:
        return 'Unknown';
    }
  }

  getSelectedSeriesNames(): string {
    const selectedSeries = this.seriesSelections()
      .filter(s => s.selected)
      .map(s => `${s.series.icon} ${s.series.name}`);

    if (selectedSeries.length === 0) {
      return 'None';
    }

    return selectedSeries.join(', ');
  }

  navigateToSignIn(): void {
    this.router.navigate(['/signin']);
  }
}
