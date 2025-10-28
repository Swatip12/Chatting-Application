import { Component, Input, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { Subscription } from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { GroupService } from '../../services/group.service';
import { AuthService } from '../../services/auth.service';
import { Group } from '../../models/group.model';
import { Message, MessageType } from '../../models/message.model';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-group-chat',
  templateUrl: './group-chat.component.html',
  styleUrls: ['./group-chat.component.css']
})
export class GroupChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @Input() group!: Group;
  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  messages: Message[] = [];
  newMessage = '';
  currentUser: User | null = null;
  isLoading = false;
  showMembers = false;
  
  private messageSubscription?: Subscription;
  private shouldScrollToBottom = false;

  constructor(
    private chatService: ChatService,
    private groupService: GroupService,
    private authService: AuthService
  ) {
    this.currentUser = this.authService.getCurrentUser();
  }

  ngOnInit(): void {
    this.loadGroupMessages();
    this.subscribeToMessages();
    this.subscribeToGroupUpdates();
  }

  ngOnDestroy(): void {
    if (this.messageSubscription) {
      this.messageSubscription.unsubscribe();
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  private loadGroupMessages(): void {
    this.isLoading = true;
    // Load message history from backend
    this.authService.getGroupMessageHistory(this.group.id).subscribe({
      next: (messages) => {
        this.messages = messages;
        this.shouldScrollToBottom = true;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading group messages:', error);
        this.isLoading = false;
      }
    });
  }

  private subscribeToMessages(): void {
    this.messageSubscription = this.chatService.subscribeToMessages().subscribe({
      next: (message) => {
        // Only add messages for this group
        if (message.groupId === this.group.id) {
          this.messages.push(message);
          this.shouldScrollToBottom = true;
        }
      },
      error: (error) => {
        console.error('Error receiving messages:', error);
      }
    });
  }

  private subscribeToGroupUpdates(): void {
    // Subscribe to group-specific topic for member join/leave notifications
    if (this.chatService.isConnected()) {
      // This would be handled by the ChatService's group subscription
    }
  }

  sendMessage(): void {
    if (this.newMessage.trim() && this.currentUser) {
      const message: Message = {
        sender: this.currentUser,
        content: this.newMessage.trim(),
        type: MessageType.CHAT,
        timestamp: new Date(),
        groupId: this.group.id
      };

      this.chatService.sendGroupMessage(message, this.group.id);
      this.newMessage = '';
    }
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  toggleMembers(): void {
    this.showMembers = !this.showMembers;
    if (this.showMembers && this.group.members) {
      // Refresh group details to get latest member list
      this.groupService.getGroup(this.group.id, true).subscribe({
        next: (updatedGroup) => {
          this.group = updatedGroup;
        },
        error: (error) => {
          console.error('Error refreshing group details:', error);
        }
      });
    }
  }

  private scrollToBottom(): void {
    try {
      if (this.messagesContainer) {
        const element = this.messagesContainer.nativeElement;
        element.scrollTop = element.scrollHeight;
      }
    } catch (err) {
      console.error('Error scrolling to bottom:', err);
    }
  }

  isOwnMessage(message: Message): boolean {
    return message.sender.username === this.currentUser?.username;
  }

  isSystemMessage(message: Message): boolean {
    return message.type === MessageType.JOIN || message.type === MessageType.LEAVE;
  }

  getMessageTime(message: Message): string {
    return new Date(message.timestamp).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  }

  getMessageDate(message: Message): string {
    return new Date(message.timestamp).toLocaleDateString();
  }

  shouldShowDateSeparator(index: number): boolean {
    if (index === 0) return true;
    
    const currentDate = new Date(this.messages[index].timestamp).toDateString();
    const previousDate = new Date(this.messages[index - 1].timestamp).toDateString();
    
    return currentDate !== previousDate;
  }
}