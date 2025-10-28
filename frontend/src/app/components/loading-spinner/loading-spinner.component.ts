import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  templateUrl: './loading-spinner.component.html',
  styleUrls: ['./loading-spinner.component.css']
})
export class LoadingSpinnerComponent {
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() color: 'primary' | 'secondary' | 'light' | 'dark' = 'primary';
  @Input() text: string = 'Loading...';
  @Input() inline: boolean = false;

  constructor() {}

  getSpinnerClass(): string {
    let classes = 'spinner-border';
    
    if (this.size === 'sm') {
      classes += ' spinner-border-sm';
    }
    
    classes += ` text-${this.color}`;
    
    return classes;
  }
}