import { User } from './user.model';

export interface Group {
  id: string;
  name: string;
  description?: string;
  createdBy: string;
  members?: User[];
  memberCount: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface GroupCreateRequest {
  name: string;
  description?: string;
}

export interface GroupMemberRequest {
  username: string;
}