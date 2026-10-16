import { describe, it, expect } from 'vitest';
import { toE164, phoneMeta, allCountryCodes } from '../src/phone.js';

describe('E.164 — рақамҳои ҳамаи кишварҳо', () => {
  it('Тоҷикистон', () => {
    expect(toE164('+992 90 123 4567')).toBe('+992901234567');
    expect(toE164('992901234567')).toBe('+992901234567');
    expect(toE164('901234567', 'TJ')).toBe('+992901234567');
  });
  it('Ӯзбекистон', () => {
    expect(toE164('+998 90 123 45 67')).toBe('+998901234567');
  });
  it('Русия', () => {
    expect(toE164('+7 926 123 45 67')).toBe('+79261234567');
  });
  it('Эрон', () => {
    expect(toE164('+98 912 345 6789')).toBe('+989123456789');
  });
  it('Олмон ва ИМА', () => {
    expect(toE164('+49 151 23456789')).toBe('+4915123456789');
    expect(toE164('+1 415 555 2671')).toBe('+14155552671');
  });
  it('Рақами нодуруст рад мешавад', () => {
    expect(toE164('+99290123')).toBeNull();
    expect(toE164('abc')).toBeNull();
    expect(toE164('')).toBeNull();
  });
  it('метадатаи кишвар', () => {
    const m = phoneMeta('+992901234567');
    expect(m?.country).toBe('TJ');
    expect(m?.callingCode).toBe('+992');
  });
  it('кодҳои ҳамаи кишварҳо (>200)', () => {
    expect(allCountryCodes().length).toBeGreaterThan(200);
  });
});
