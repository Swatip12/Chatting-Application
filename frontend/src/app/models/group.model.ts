export interface Group {
  id: string;
  name: string;
  description?: string;
  createdBy: string;
  members?: GroupMember[];
  memberCount: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface GroupMember {
  id: number;
  username: string;
  status: string;
  lastSeen?: Date;
}

export interface GroupCreateRequest {
  name: string;
  description?: string;
}

export interface GroupMemberRequest {
  username: string;
}