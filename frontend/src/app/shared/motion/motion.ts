import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

if (typeof matchMedia === 'function') {
  gsap.registerPlugin(ScrollTrigger);
}

export function prefersReducedMotion(): boolean {
  return typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
}

export function animateAuthSplit(root: HTMLElement): gsap.Context {
  return gsap.context(() => {
    if (prefersReducedMotion()) {
      return;
    }
    gsap.from('.auth-form-inner > *', {
      y: 22,
      autoAlpha: 0,
      duration: 0.65,
      stagger: 0.07,
      ease: 'power3.out',
      immediateRender: false,
    });
    gsap.from('.auth-visual', {
      xPercent: 8,
      autoAlpha: 0,
      duration: 0.95,
      ease: 'power3.out',
      immediateRender: false,
    });
    gsap.from('.auth-visual-copy > *', {
      y: 20,
      autoAlpha: 0,
      duration: 0.7,
      stagger: 0.1,
      delay: 0.28,
      ease: 'power3.out',
      immediateRender: false,
    });
  }, root);
}

export function animateLanding(root: HTMLElement): gsap.Context {
  return gsap.context(() => {
    if (prefersReducedMotion()) {
      return;
    }

    const hero = gsap.timeline({ defaults: { ease: 'power3.out' } });
    hero
      .from('.hero-copy > *', { y: 36, opacity: 0, duration: 0.8, stagger: 0.1 })
      .from('.hero-media', { scale: 1.08, opacity: 0, duration: 1.05, ease: 'power2.out' }, 0)
      .from('.hero-chip', { y: 16, opacity: 0, duration: 0.55, stagger: 0.08 }, '-=0.45');

    gsap.to('.hero-media img', {
      yPercent: 14,
      ease: 'none',
      scrollTrigger: {
        trigger: '.hero',
        start: 'top top',
        end: 'bottom top',
        scrub: true,
      },
    });

    gsap.from('.stat-card', {
      y: 32,
      opacity: 0,
      duration: 0.65,
      stagger: 0.1,
      ease: 'power3.out',
      scrollTrigger: { trigger: '.stats', start: 'top 82%' },
    });

    gsap.from('.section-head', {
      y: 24,
      opacity: 0,
      duration: 0.6,
      scrollTrigger: { trigger: '#modulos', start: 'top 80%' },
    });

    gsap.utils.toArray<HTMLElement>('.module-card').forEach((card, index) => {
      gsap.from(card, {
        y: 48,
        opacity: 0,
        duration: 0.65,
        delay: (index % 3) * 0.08,
        ease: 'power3.out',
        scrollTrigger: { trigger: card, start: 'top 88%' },
      });
    });

    gsap.utils.toArray<HTMLElement>('.step-card').forEach((step, index) => {
      gsap.from(step, {
        x: index % 2 === 0 ? -36 : 36,
        opacity: 0,
        duration: 0.7,
        ease: 'power3.out',
        scrollTrigger: { trigger: step, start: 'top 86%' },
      });
    });

    gsap.from('.cta-band', {
      y: 40,
      opacity: 0,
      duration: 0.8,
      ease: 'power3.out',
      scrollTrigger: { trigger: '.cta-band', start: 'top 88%' },
    });
  }, root);
}
