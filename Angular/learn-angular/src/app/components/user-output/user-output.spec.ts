import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserOutput } from './user-output';

describe('UserOutput', () => {
  let component: UserOutput;
  let fixture: ComponentFixture<UserOutput>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserOutput],
    }).compileComponents();

    fixture = TestBed.createComponent(UserOutput);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
