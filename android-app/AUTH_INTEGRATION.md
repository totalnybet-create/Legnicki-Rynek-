# Integracja kont użytkowników — Legnicki Rynek Android

Aplikacja obsługuje dwa tryby:

- konto serwerowe z rejestracją, logowaniem i szyfrowaną sesją,
- profil lokalny działający bez skonfigurowanego backendu.

Konto serwerowe korzysta z tego samego `LISTINGS_API_BASE_URL`, co synchronizacja ogłoszeń.

## Rejestracja

`POST /auth/register`

Żądanie:

```json
{
  "name": "Jan Kowalski",
  "email": "jan@example.pl",
  "password": "bezpieczne-haslo"
}
```

## Logowanie

`POST /auth/login`

Żądanie:

```json
{
  "email": "jan@example.pl",
  "password": "bezpieczne-haslo"
}
```

## Odpowiedź uwierzytelnienia

Preferowany format:

```json
{
  "user": {
    "id": "user-123",
    "name": "Jan Kowalski",
    "email": "jan@example.pl"
  },
  "session": {
    "accessToken": "token-dostepu",
    "refreshToken": "token-odswiezenia",
    "expiresAt": 1893456000000
  }
}
```

Kodek obsługuje również płaski format z polami `token`, `access_token`, `refresh_token`, `expiresIn` lub `expires_at`.

## Wylogowanie

`POST /auth/logout`

Nagłówek:

```text
Authorization: Bearer <accessToken>
```

Treść: pusty obiekt JSON `{}`. Niezależnie od odpowiedzi serwera aplikacja usuwa lokalną sesję.

## Bezpieczeństwo sesji

- token dostępu i token odświeżenia są szyfrowane AES-256-GCM,
- klucz jest generowany i przechowywany w Android Keystore,
- zaszyfrowane dane są przechowywane w Preferences DataStore,
- profil nie jest uznawany za zalogowany po wygaśnięciu lub uszkodzeniu sesji,
- poza lokalnym emulatorem backend musi używać HTTPS,
- hasło nie jest zapisywane w aplikacji.

## Walidacja po stronie serwera

Backend powinien niezależnie:

- normalizować i potwierdzać unikalność adresu e-mail,
- hashować hasła algorytmem przeznaczonym do haseł, np. Argon2id,
- wydawać krótko żyjące tokeny dostępu,
- rotować i unieważniać tokeny odświeżenia,
- egzekwować właściciela ogłoszenia na podstawie sesji, a nie pola `ownerId` przesłanego przez klienta,
- ograniczać próby logowania i rejestracji,
- nie ujawniać, czy konkretny adres e-mail istnieje, w komunikatach resetowania hasła.
