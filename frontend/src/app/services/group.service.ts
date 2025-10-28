import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface GroupRequest {
  name: string;
  description?: string;
}

export interface GroupMemberRequest {
  username: string;
}

import { Group, GroupMember } from '../models/group.model';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class GroupService {
  private apiUrl = `${environment.apiUrl}/groups`;
  private userGroupsSubject = new BehaviorSubject<Group[]>([]);
  private allGroupsSubject = new BehaviorSubject<Group[]>([]);

  constructor(private http: HttpClient) {}

  /**
   * Create a new group
   */
  createGroup(groupRequest: GroupRequest): Observable<Group> {
    return this.http.post<ApiResponse<Group>>(`${this.apiUrl}`, groupRequest)
      .pipe(
        map(response => response.data),
        tap(() => this.refreshUserGroups())
      );
  }

  /**
   * Get group information by ID
   */
  getGroup(groupId: string, includeMembers: boolean = false): Observable<Group> {
    const params = new HttpParams().set('includeMembers', includeMembers.toString());
    return this.http.get<ApiResponse<Group>>(`${this.apiUrl}/${groupId}`, { params })
      .pipe(map(response => response.data));
  }

  /**
   * Get all groups where the current user is a member
   */
  getUserGroups(limit: number = 20): Observable<Group[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ApiResponse<Group[]>>(`${this.apiUrl}/my`, { params })
      .pipe(
        map(response => response.data),
        tap(groups => this.userGroupsSubject.next(groups))
      );
  }

  /**
   * Get all available groups (for discovery)
   */
  getAllGroups(limit: number = 50): Observable<Group[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ApiResponse<Group[]>>(`${this.apiUrl}`, { params })
      .pipe(
        map(response => response.data),
        tap(groups => this.allGroupsSubject.next(groups))
      );
  }

  /**
   * Search groups by name
   */
  searchGroups(searchTerm: string, limit: number = 20): Observable<Group[]> {
    const params = new HttpParams()
      .set('q', searchTerm)
      .set('limit', limit.toString());
    return this.http.get<ApiResponse<Group[]>>(`${this.apiUrl}/search`, { params })
      .pipe(map(response => response.data));
  }

  /**
   * Add a member to a group
   */
  addMember(groupId: string, memberRequest: GroupMemberRequest): Observable<Group> {
    return this.http.post<ApiResponse<Group>>(`${this.apiUrl}/${groupId}/members`, memberRequest)
      .pipe(
        map(response => response.data),
        tap(() => this.refreshUserGroups())
      );
  }

  /**
   * Remove a member from a group
   */
  removeMember(groupId: string, memberRequest: GroupMemberRequest): Observable<Group> {
    return this.http.delete<ApiResponse<Group>>(`${this.apiUrl}/${groupId}/members`, { body: memberRequest })
      .pipe(
        map(response => response.data),
        tap(() => this.refreshUserGroups())
      );
  }

  /**
   * Check if current user is a member of a group
   */
  checkMembership(groupId: string): Observable<boolean> {
    return this.http.get<ApiResponse<boolean>>(`${this.apiUrl}/${groupId}/membership`)
      .pipe(map(response => response.data));
  }

  /**
   * Get user groups as observable
   */
  getUserGroupsObservable(): Observable<Group[]> {
    return this.userGroupsSubject.asObservable();
  }

  /**
   * Get all groups as observable
   */
  getAllGroupsObservable(): Observable<Group[]> {
    return this.allGroupsSubject.asObservable();
  }

  /**
   * Refresh user groups
   */
  refreshUserGroups(): void {
    this.getUserGroups().subscribe();
  }

  /**
   * Refresh all groups
   */
  refreshAllGroups(): void {
    this.getAllGroups().subscribe();
  }
}