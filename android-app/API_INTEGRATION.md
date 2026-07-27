# Integracja API — Legnicki Rynek Android

Aplikacja działa w trybie offline-first. Bez skonfigurowanego serwera wszystkie ogłoszenia pozostają w lokalnej bazie Room. Po ustawieniu adresu API aplikacja automatycznie synchronizuje dane podczas uruchamiania i ręcznego odświeżania.

## Konfiguracja

Adresu i tokenu nie należy zapisywać w kodzie ani commitować do repozytorium.

Obsługiwane wartości:

- zmienna środowiskowa lub właściwość Gradle `LISTINGS_API_BASE_URL`,
- opcjonalna zmienna środowiskowa lub właściwość Gradle `LISTINGS_API_TOKEN`.

Przykład lokalnego buildu:

```bash
gradle -p android-app assembleDebug \
  -PLISTINGS_API_BASE_URL=https://api.example.pl/v1 \
  -PLISTINGS_API_TOKEN=token
```

W GitHub Actions należy utworzyć sekrety repozytorium o tych samych nazwach. Pusty adres API oznacza poprawny tryb wyłącznie lokalny.

Poza emulatorem aplikacja akceptuje wyłącznie adres API wykorzystujący HTTPS. Dla testów emulatora dozwolone są `http://10.0.2.2`, `http://127.0.0.1` i `http://localhost`.

## Kontrakt ogłoszeń

### Pobranie ogłoszeń

`GET /listings`

Serwer może zwrócić bezpośrednią tablicę lub obiekt z polem `listings`:

```json
{
  "listings": [
    {
      "apiVersion": 1,
      "id": "listing-123",
      "title": "Rower miejski",
      "price": 900,
      "location": "Legnica, Tarninów",
      "categoryId": "sport",
      "description": "Sprawny rower w dobrym stanie.",
      "imageUrls": ["https://cdn.example.pl/rower.jpg"],
      "ownerId": "owner-id",
      "sellerName": "Jan",
      "createdAt": 1785160000000,
      "updatedAt": 1785160100000,
      "status": "ACTIVE",
      "latitude": 51.207,
      "longitude": 16.1619
    }
  ]
}
```

Dozwolone statusy: `ACTIVE`, `RESERVED`, `SOLD`, `EXPIRED`.

### Utworzenie lub aktualizacja

`PUT /listings/{id}`

Treść żądania ma strukturę pojedynczego ogłoszenia z przykładu powyżej. Serwer powinien zwrócić kod 2xx. Operacja musi być idempotentna dla tego samego identyfikatora.

### Usunięcie

`DELETE /listings/{id}`

Serwer powinien zwrócić kod 2xx również wtedy, gdy rekord został już wcześniej usunięty. Dzięki temu aplikacja może bezpiecznie ponawiać usunięcia zapisane lokalnie podczas braku internetu.

## Upload zdjęć

`POST /uploads`

Typ: `multipart/form-data`. Nazwa pola pliku: `file`.

Odpowiedź:

```json
{
  "url": "https://cdn.example.pl/listings/photo.jpg"
}
```

Wymagania:

- plik musi mieć MIME rozpoczynające się od `image/`,
- maksymalny rozmiar pojedynczego zdjęcia: 10 MB,
- maksymalnie 12 zdjęć w jednym ogłoszeniu,
- zwracany adres powinien być trwałym publicznym adresem HTTPS,
- lokalne URI `content://` nigdy nie są wysyłane jako adresy zdjęć w JSON.

## Synchronizacja i konflikty

- nowsza wartość `updatedAt` wygrywa konflikt,
- ulubione są stanem lokalnym użytkownika i nie są wysyłane do wspólnego API,
- lokalne URI zdjęć są zachowywane do czasu udanego uploadu,
- usunięcia wykonane offline trafiają do trwałej kolejki i są ponawiane,
- awaria serwera nie blokuje dodawania, edycji, usuwania ani przeglądania lokalnych danych.

## Autoryzacja

Gdy `LISTINGS_API_TOKEN` nie jest pusty, aplikacja wysyła nagłówek:

```text
Authorization: Bearer <token>
```

Token buildowy jest rozwiązaniem przejściowym dla integracji technicznej. Docelowy backend powinien wydawać token sesji konkretnego użytkownika, sprawdzać właściciela ogłoszenia po stronie serwera, ograniczać liczbę żądań i walidować wszystkie pola niezależnie od walidacji w aplikacji.
