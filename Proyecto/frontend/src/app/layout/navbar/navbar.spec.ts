import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Auth } from '../../core/services/auth';
import { Navbar } from './navbar';

describe('Navbar', () => {
  let fixture: ComponentFixture<Navbar>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Navbar],
      providers: [provideRouter([])],
    }).compileComponents();

    TestBed.inject(Auth).login({ username: 'ana', rol: 'COORDINADOR' });
    fixture = TestBed.createComponent(Navbar);
    await fixture.whenStable();
  });

  it('shows the user and role', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'ana · Centro Coordinador Regional',
    );
  });
});
