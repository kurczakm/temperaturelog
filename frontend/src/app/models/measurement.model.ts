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
  /**
   * User ID of the creator. Use createdByUsername for display purposes.
   * Can be null if user was deleted or measurement was created by system.
   */
  createdBy: number | null;
  /**
   * Username of the creator for display purposes.
   * Can be null if user information is unavailable.
   */
  createdByUsername: string | null;
  createdAt: string;
}
