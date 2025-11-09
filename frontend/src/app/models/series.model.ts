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
  createdBy: string;
  createdAt: string;
  modifiedBy: string;
  modifiedAt: string;
}
