import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SeriesRequest, SeriesResponse } from '../models/series.model';
import { Auth } from '../auth/auth';

@Injectable({
  providedIn: 'root',
})
export class SeriesService {
  private readonly API_URL = 'http://localhost:8081/api/series';

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

  getAllSeries(): Observable<SeriesResponse[]> {
    return this.http.get<SeriesResponse[]>(this.API_URL, {
      headers: this.getAuthHeaders()
    });
  }

  getSeriesById(id: number): Observable<SeriesResponse> {
    return this.http.get<SeriesResponse>(`${this.API_URL}/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  createSeries(request: SeriesRequest): Observable<SeriesResponse> {
    return this.http.post<SeriesResponse>(this.API_URL, request, {
      headers: this.getAuthHeaders()
    });
  }

  updateSeries(id: number, request: SeriesRequest): Observable<SeriesResponse> {
    return this.http.put<SeriesResponse>(`${this.API_URL}/${id}`, request, {
      headers: this.getAuthHeaders()
    });
  }

  deleteSeries(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`, {
      headers: this.getAuthHeaders()
    });
  }
}
