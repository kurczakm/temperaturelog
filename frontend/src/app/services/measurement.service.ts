import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MeasurementRequest, MeasurementResponse } from '../models/measurement.model';
import { Auth } from '../auth/auth';

@Injectable({
  providedIn: 'root',
})
export class MeasurementService {
  private readonly API_URL = 'http://localhost:8081/api/measurements';

  constructor(
    private http: HttpClient,
    private auth: Auth
  ) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.auth.getToken();
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  getAllMeasurements(): Observable<MeasurementResponse[]> {
    return this.http.get<MeasurementResponse[]>(this.API_URL, {
      headers: this.getAuthHeaders()
    });
  }

  getMeasurementById(id: number): Observable<MeasurementResponse> {
    return this.http.get<MeasurementResponse>(`${this.API_URL}/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  getMeasurementsBySeriesId(seriesId: number): Observable<MeasurementResponse[]> {
    return this.http.get<MeasurementResponse[]>(`${this.API_URL}/series/${seriesId}`, {
      headers: this.getAuthHeaders()
    });
  }

  createMeasurement(request: MeasurementRequest): Observable<MeasurementResponse> {
    return this.http.post<MeasurementResponse>(this.API_URL, request, {
      headers: this.getAuthHeaders()
    });
  }

  updateMeasurement(id: number, request: MeasurementRequest): Observable<MeasurementResponse> {
    return this.http.put<MeasurementResponse>(`${this.API_URL}/${id}`, request, {
      headers: this.getAuthHeaders()
    });
  }

  deleteMeasurement(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`, {
      headers: this.getAuthHeaders()
    });
  }
}
