import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatRequest {
  messages: ChatMessage[];
  session_id?: string;
}

export interface ChatResponse {
  response: string;
  agent_used: string;
  session_id: string;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private apiUrl = '/api/chat';

  chat(messages: ChatMessage[], sessionId?: string): Observable<ChatResponse> {
    const token = this.authService.token();
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    const request: ChatRequest = {
      messages,
      session_id: sessionId
    };

    return this.http.post<ChatResponse>(this.apiUrl, request, { headers });
  }
}
