import { Component, inject, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ChatService, ChatMessage } from '../../core/services/chat.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="chat-container">
      <div class="chat-header">
        <mat-icon>smart_toy</mat-icon>
        <h2>B2B Auto Parts Assistant</h2>
      </div>

      <div class="chat-messages" #messagesContainer>
        @if (messages().length === 0) {
          <div class="welcome-message">
            <mat-icon class="welcome-icon">smart_toy</mat-icon>
            <h3>Welcome! How can I help you today?</h3>
            <p>I can help you with:</p>
            <ul>
              <li>Checking order status</li>
              <li>Creating new orders</li>
              <li>Finding products</li>
              <li>Product details and pricing</li>
            </ul>
          </div>
        }

        @for (msg of messages(); track $index) {
          <div class="message" [class.user]="msg.role === 'user'" [class.assistant]="msg.role === 'assistant'">
            <div class="message-content">
              {{ msg.content }}
            </div>
            <div class="message-time">{{ msg.timestamp | date:'shortTime' }}</div>
          </div>
        }

        @if (loading()) {
          <div class="message assistant">
            <div class="message-content">
              <mat-spinner diameter="20"></mat-spinner>
            </div>
          </div>
        }
      </div>

      <div class="chat-input">
        <mat-form-field appearance="outline" class="input-field">
          <mat-label>Type your message...</mat-label>
          <input
            matInput
            [(ngModel)]="inputMessage"
            (keyup.enter)="sendMessage()"
            [disabled]="loading()"
            placeholder="Ask about orders, products, or create an order..."
          >
        </mat-form-field>
        <button
          mat-fab
          color="primary"
          (click)="sendMessage()"
          [disabled]="!inputMessage().trim() || loading()"
          class="send-button"
        >
          <mat-icon>send</mat-icon>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .chat-container {
      display: flex;
      flex-direction: column;
      height: calc(100vh - 64px);
      max-width: 800px;
      margin: 0 auto;
      background: white;
    }

    .chat-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 24px;
      background: #1976D2;
      color: white;

      mat-icon {
        font-size: 28px;
        width: 28px;
        height: 28px;
      }

      h2 {
        margin: 0;
        font-size: 20px;
        font-weight: 500;
      }
    }

    .chat-messages {
      flex: 1;
      overflow-y: auto;
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .welcome-message {
      text-align: center;
      padding: 48px 24px;
      color: rgba(0, 0, 0, 0.6);

      .welcome-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: #1976D2;
        margin-bottom: 16px;
      }

      h3 {
        margin: 0 0 16px 0;
        font-size: 20px;
        font-weight: 500;
      }

      p {
        margin: 0 0 8px 0;
        font-size: 14px;
      }

      ul {
        text-align: left;
        display: inline-block;
        margin: 0;
        padding-left: 24px;
        font-size: 14px;

        li {
          margin: 4px 0;
        }
      }
    }

    .message {
      display: flex;
      flex-direction: column;
      max-width: 70%;

      &.user {
        align-self: flex-end;

        .message-content {
          background: #1976D2;
          color: white;
          border-bottom-right-radius: 4px;
        }
      }

      &.assistant {
        align-self: flex-start;

        .message-content {
          background: #f5f5f5;
          color: rgba(0, 0, 0, 0.87);
          border-bottom-left-radius: 4px;
        }
      }

      .message-content {
        padding: 12px 16px;
        border-radius: 16px;
        font-size: 14px;
        line-height: 1.5;
        white-space: pre-wrap;
        word-wrap: break-word;

        mat-spinner {
          display: inline-block;
        }
      }

      .message-time {
        font-size: 11px;
        color: rgba(0, 0, 0, 0.4);
        margin-top: 4px;
        padding: 0 8px;
      }
    }

    .chat-input {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 24px;
      border-top: 1px solid rgba(0, 0, 0, 0.12);
      background: white;

      .input-field {
        flex: 1;

        ::ng-deep .mat-mdc-form-field-subscript-wrapper {
          display: none;
        }
      }

      .send-button {
        flex-shrink: 0;
      }
    }
  `]
})
export class ChatComponent implements AfterViewChecked {
  private chatService = inject(ChatService);
  private authService = inject(AuthService);

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  messages = signal<Array<ChatMessage & { timestamp: Date }>>([]);
  inputMessage = signal('');
  loading = signal(false);

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  sendMessage() {
    const message = this.inputMessage().trim();
    if (!message || this.loading()) return;

    // Add user message
    const userMsg: ChatMessage & { timestamp: Date } = {
      role: 'user',
      content: message,
      timestamp: new Date()
    };
    this.messages.update(msgs => [...msgs, userMsg]);
    this.inputMessage.set('');
    this.loading.set(true);

    // Send to chatbot
    const allMessages = this.messages().map(m => ({ role: m.role, content: m.content }));

    this.chatService.chat(allMessages).subscribe({
      next: (response) => {
        const assistantMsg: ChatMessage & { timestamp: Date } = {
          role: 'assistant',
          content: response.response,
          timestamp: new Date()
        };
        this.messages.update(msgs => [...msgs, assistantMsg]);
        this.loading.set(false);
      },
      error: (err) => {
        const errorMsg: ChatMessage & { timestamp: Date } = {
          role: 'assistant',
          content: 'Sorry, I encountered an error. Please try again.',
          timestamp: new Date()
        };
        this.messages.update(msgs => [...msgs, errorMsg]);
        this.loading.set(false);
        console.error('Chat error:', err);
      }
    });
  }

  private scrollToBottom() {
    if (this.messagesContainer) {
      const container = this.messagesContainer.nativeElement;
      container.scrollTop = container.scrollHeight;
    }
  }
}
