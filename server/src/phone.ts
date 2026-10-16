import { parsePhoneNumberFromString, AsYouType, getCountries, getCountryCallingCode } from 'libphonenumber-js';

/** Норматсияи рақам ба E.164. Ҳамаи кишварҳои ҷаҳон дастгирӣ мешаванд. */
export function toE164(input: string, defaultCountry?: string): string | null {
  const phone = input?.trim();
  if (!phone) return null;
  let parsed;
  if (phone.startsWith('+')) {
    parsed = parsePhoneNumberFromString(phone);
  } else if (defaultCountry) {
    parsed = parsePhoneNumberFromString(phone, defaultCountry as any);
  } else if (/^\d{7,15}$/.test(phone)) {
    parsed = parsePhoneNumberFromString('+' + phone);
  } else {
    return null;
  }
  if (!parsed) return null;
  if (!parsed.isValid()) return null;
  return parsed.number; // E.164
}

/** Метадатаи рақам: коди кишвар ва шакли зебо */
export function phoneMeta(e164: string): { country: string; callingCode: string; formatted: string } | null {
  const parsed = parsePhoneNumberFromString(e164);
  if (!parsed) return null;
  return {
    country: parsed.country ?? '',
    callingCode: '+' + parsed.countryCallingCode,
    formatted: parsed.formatInternational(),
  };
}

/** Рӯйхати кодҳои ҳамаи кишварҳо барои майдони интихоби код дар клиент */
export function allCountryCodes(): { country: string; code: string }[] {
  return getCountries()
    .map((c) => ({ country: c, code: '+' + getCountryCallingCode(c) }))
    .sort((a, b) => a.code.localeCompare(b.code) || a.country.localeCompare(b.country));
}

export function asYouType(country: string): (s: string) => string {
  const t = new AsYouType(country as any);
  return (s: string) => {
    t.reset();
    return t.input(s);
  };
}
