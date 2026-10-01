import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Auth } from '../../core/services/auth';
import { Navbar } from './navbar';

describe('Navbar', () => {
  let fixture: ComponentFixture<Navbar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Navbar],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    TestBed.inject(Auth).login({ username: 'ana', password: 'bpm' }).subscribe();
    TestBed.inject(HttpTestingController)
      .expectOne((r) => r.url.endsWith('/auth/login'))
      .flush({
        userId: '1',
        username: 'ana',
        firstName: 'Ana',
        lastName: 'G',
        role: 'COORDINADOR',
        group: 'Centro Regional Coordinador',
      });
    fixture = TestBed.createComponent(Navbar);
    await fixture.whenStable();
  });

  it('shows the user and role once the menu is open', async () => {
    const el = fixture.nativeElement as HTMLElement;
    el.querySelector<HTMLButtonElement>('.navbar-toggler')!.click();
    await fixture.whenStable();

    expect(el.textContent).toContain('ana · Centro Coordinador Regional');
  });
});
