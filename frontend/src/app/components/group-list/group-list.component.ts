import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { GroupService } from '../../services/group.service';
import { Group } from '../../models/group.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-group-list',
  templateUrl: './group-list.component.html',
  styleUrls: ['./group-list.component.css']
})
export class GroupListComponent implements OnInit {
  @Input() showMyGroups = true;
  @Input() showAllGroups = false;
  @Output() groupSelected = new EventEmitter<Group>();
  @Output() createGroupClicked = new EventEmitter<void>();

  myGroups: Group[] = [];
  allGroups: Group[] = [];
  searchResults: Group[] = [];
  isLoading = false;
  searchTerm = '';
  currentUser: any;
  showSearch = false;

  constructor(
    private groupService: GroupService,
    private authService: AuthService
  ) {
    this.currentUser = this.authService.getCurrentUser();
  }

  ngOnInit(): void {
    this.loadGroups();
  }

  loadGroups(): void {
    this.isLoading = true;

    if (this.showMyGroups) {
      this.groupService.getUserGroups().subscribe({
        next: (groups) => {
          this.myGroups = groups;
        },
        error: (error) => {
          console.error('Error loading user groups:', error);
        }
      });
    }

    if (this.showAllGroups) {
      this.groupService.getAllGroups().subscribe({
        next: (groups) => {
          this.allGroups = groups;
        },
        error: (error) => {
          console.error('Error loading all groups:', error);
        },
        complete: () => {
          this.isLoading = false;
        }
      });
    } else {
      this.isLoading = false;
    }
  }

  onGroupClick(group: Group): void {
    this.groupSelected.emit(group);
  }

  onCreateGroup(): void {
    this.createGroupClicked.emit();
  }

  onSearch(): void {
    if (this.searchTerm.trim()) {
      this.isLoading = true;
      this.groupService.searchGroups(this.searchTerm.trim()).subscribe({
        next: (groups) => {
          this.searchResults = groups;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error searching groups:', error);
          this.isLoading = false;
        }
      });
    } else {
      this.searchResults = [];
    }
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.searchResults = [];
    this.showSearch = false;
  }

  toggleSearch(): void {
    this.showSearch = !this.showSearch;
    if (!this.showSearch) {
      this.clearSearch();
    }
  }

  isGroupCreator(group: Group): boolean {
    return group.createdBy === this.currentUser?.username;
  }

  getDisplayGroups(): Group[] {
    if (this.searchTerm && this.searchResults.length > 0) {
      return this.searchResults;
    }
    
    if (this.showMyGroups && this.showAllGroups) {
      // Combine and deduplicate
      const combined = [...this.myGroups];
      this.allGroups.forEach(group => {
        if (!combined.find(g => g.id === group.id)) {
          combined.push(group);
        }
      });
      return combined;
    }
    
    return this.showMyGroups ? this.myGroups : this.allGroups;
  }
}