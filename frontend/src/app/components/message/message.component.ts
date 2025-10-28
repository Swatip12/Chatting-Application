import { Component, Input } from '@angular/core';
import { Message, MessageType } from '../../models/message.model';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-message',
  templateUrl: './message.component.html',
  styleUrls: ['./message.component.css']
})
export class MessageComponent {
  @Input() message!: Message;
  @Input() currentUser!: User;
  @Input() isGroupChat: boolean = false;

  get isSentByCurrentUser(): boolean {
    return this.message.sender.id === this.currentUser.id;
  }

  get isSystemMessage(): boolean {
    return this.message.type === MessageType.JOIN || this.message.type === MessageType.LEAVE;
  }

  get messageTypeClass(): string {
    if (this.isSystemMessage) {
      return 'system-message';
    }
    return this.isSentByCurrentUser ? 'message-sent' : 'message-received';
  }

  get formattedTime(): string {
    const now = new Date();
    const messageTime = new Date(this.message.timestamp);
    const diffInHours = (now.getTime() - messageTime.getTime()) / (1000 * 60 * 60);

    if (diffInHours < 24) {
      // Show time only if message is from today
      return messageTime.toLocaleTimeString('en-US', { 
        hour: '2-digit', 
        minute: '2-digit',
        hour12: true 
      });
    } else {
      // Show date and time if message is older
      return messageTime.toLocaleString('en-US', { 
        month: 'short', 
        day: 'numeric',
        hour: '2-digit', 
        minute: '2-digit',
        hour12: true 
      });
    }
  }

  get systemMessageIcon(): string {
    switch (this.message.type) {
      case MessageType.JOIN:
        return 'fas fa-sign-in-alt';
      case MessageType.LEAVE:
        return 'fas fa-sign-out-alt';
      default:
        return 'fas fa-info-circle';
    }
  }
}