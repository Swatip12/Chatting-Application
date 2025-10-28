import { User } from './user.model';

export interface Message {
  id?: number | string;
  sender: User;
  receiver?: User;
  content: string;
  type: MessageType;
  timestamp: Date;
  groupId?: string;
}

export enum MessageType {
  CHAT = 'CHAT',
  JOIN = 'JOIN',
  LEAVE = 'LEAVE'
}

export interface ChatMessage {
  sender: string;
  receiver?: string;
  content: string;
  type: MessageType;
  groupId?: string;
}

export interface UserStatusMessage {
  username: string;
  status: string;
  type: MessageType;
}