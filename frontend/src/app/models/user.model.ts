export interface User {
  id: number;
  username: string;
  status: UserStatus;
  createdAt?: Date;
  lastSeen?: Date;
}

export enum UserStatus {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE'
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  user: User;
  message: string;
  success: boolean;
}