import { Component, EventEmitter, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { GroupService } from '../../services/group.service';
import { Group, GroupCreateRequest } from '../../models/group.model';

@Component({
  selector: 'app-group-create',
  templateUrl: './group-create.component.html',
  styleUrls: ['./group-create.component.css']
})
export class GroupCreateComponent {
  @Output() groupCreated = new EventEmitter<Group>();
  @Output() cancelled = new EventEmitter<void>();

  createGroupForm: FormGroup;
  isLoading = false;
  errorMessage = '';

  constructor(
    private formBuilder: FormBuilder,
    private groupService: GroupService
  ) {
    this.createGroupForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]]
    });
  }

  onSubmit(): void {
    if (this.createGroupForm.valid && !this.isLoading) {
      this.isLoading = true;
      this.errorMessage = '';

      const groupRequest: GroupCreateRequest = {
        name: this.createGroupForm.value.name.trim(),
        description: this.createGroupForm.value.description?.trim() || undefined
      };

      this.groupService.createGroup(groupRequest).subscribe({
        next: (group) => {
          console.log('Group created successfully:', group);
          this.groupCreated.emit(group);
          this.resetForm();
        },
        error: (error) => {
          console.error('Error creating group:', error);
          this.errorMessage = error.error?.message || 'Failed to create group. Please try again.';
          this.isLoading = false;
        },
        complete: () => {
          this.isLoading = false;
        }
      });
    }
  }

  onCancel(): void {
    this.cancelled.emit();
    this.resetForm();
  }

  private resetForm(): void {
    this.createGroupForm.reset();
    this.errorMessage = '';
    this.isLoading = false;
  }

  get name() {
    return this.createGroupForm.get('name');
  }

  get description() {
    return this.createGroupForm.get('description');
  }
}