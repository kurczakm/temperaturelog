export interface MeasurementRequest {
  seriesId: number;
  value: number;
  timestamp: string; // ISO 8601 format
}

export interface MeasurementResponse {
  id: number;
  seriesId: number;
  value: number;
  timestamp: string;
  createdBy: string;
  createdAt: string;
  modifiedBy: string;
  modifiedAt: string;
}
