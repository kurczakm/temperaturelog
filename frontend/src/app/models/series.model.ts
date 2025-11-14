export interface SeriesRequest {
  name: string;
  description?: string;
  minValue: number;
  maxValue: number;
  color: string;
  icon: string;
}

export interface SeriesResponse {
  id: number;
  name: string;
  description?: string;
  minValue: number;
  maxValue: number;
  color: string;
  icon: string;
  /**
   * User ID of the creator. Use createdByUsername for display purposes.
   * Can be null if user was deleted or series was created by system.
   */
  createdBy: number | null;
  /**
   * Username of the creator for display purposes.
   * Can be null if user information is unavailable.
   */
  createdByUsername: string | null;
  createdAt: string;
}
