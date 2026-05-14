import { Component, INJECTOR, Input } from '@angular/core';

@Component({
  selector: 'app-user-input',
  imports: [],
  templateUrl: './user-input.html',
  styleUrl: './user-input.css',
})
export class UserInput {

  @Input() name = ''
  @Input() age = 0
  @Input() role = ''

}
