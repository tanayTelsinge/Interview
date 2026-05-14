import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { User } from './components/user/user';
import { UserInput } from './components/user-input/user-input';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, User, UserInput],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('learn-angular');
}
