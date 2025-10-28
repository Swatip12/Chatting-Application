import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { ChatRoomComponent } from './components/chat-room/chat-room.component';
import { MessageComponent } from './components/message/message.component';
import { UserListComponent } from './components/user-list/user-list.component';
import { GroupCreateComponent } from './components/group-create/group-create.component';
import { GroupListComponent } from './components/group-list/group-list.component';
import { GroupChatComponent } from './components/group-chat/group-chat.component';
import { NotificationComponent } from './components/notification/notification.component';
import { LoadingSpinnerComponent } from './components/loading-spinner/loading-spinner.component';
import { GlobalLoadingComponent } from './components/global-loading/global-loading.component';

import { ErrorInterceptor } from './interceptors/error.interceptor';
import { LoadingInterceptor } from './interceptors/loading.interceptor';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    ChatRoomComponent,
    MessageComponent,
    UserListComponent,
    GroupCreateComponent,
    GroupListComponent,
    GroupChatComponent,
    NotificationComponent,
    LoadingSpinnerComponent,
    GlobalLoadingComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: LoadingInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }