import { Component } from '@angular/core';

@Component({
  selector: 'app-user',
  imports: [],
  templateUrl: './user.html',
  styleUrl: './user.css',
})
export class User {
  name = 'John Doe';
  age = 28;
  role = 'Backend Developer';
  address = { city: 'Mumbai', country: 'India' };
}
