# Legnicki Rynek Android — audyt i plan realizacji

Data audytu: 2026-07-27
Gałąź robocza: `android-complete-v3`
Bazowy commit: `2baa468884e83bd372aede18a3fcdf54bca95a5e`

## 1. Ocena ogólna

Obecny projekt jest wczesnym prototypem aplikacji Jetpack Compose. Zawiera podstawową nawigację, pięć ekranów, statyczne dane demonstracyjne oraz prosty lokalny zapis ogłoszeń, ulubionych i profilu. Nie jest jeszcze kompletną aplikacją użytkową ani wersją gotową do publikacji.

Szacowany stan:

- prototyp interfejsu: około 35%
- funkcjonalny MVP: około 15%
- wersja produkcyjna: poniżej 10%

## 2. Elementy obecnie wykonane

- projekt Android w Kotlinie i Jetpack Compose,
- Material 3 jako podstawowa biblioteka UI,
- pięć głównych sekcji dolnej nawigacji,
- ekran główny z prostym wyszukiwaniem,
- lista kategorii i lokalne filtrowanie po kategorii,
- formularz tworzenia prostego ogłoszenia,
- lokalne oznaczanie ulubionych,
- demonstracyjny ekran wiadomości,
- demonstracyjny profil użytkownika,
- lokalny zapis przez SharedPreferences i ręcznie tworzony JSON,
- podstawowy workflow budowania debug APK.

## 3. Krytyczne problemy P0

### 3.1. Prawdopodobny błąd kompilacji

`HomeScreen.kt` i `MessagesScreen.kt` używają `Modifier.weight(...)`, ale nie importują `androidx.compose.foundation.layout.weight`.

### 3.2. Brak powtarzalnego środowiska budowania

- brak Gradle Wrappera,
- workflow używa globalnego Gradle 8.9,
- brak jawnego uruchamiania testów i lint,
- brak potwierdzonego statusu ostatniego buildu.

### 3.3. Brak architektury aplikacji

- stan aplikacji jest przechowywany bezpośrednio w głównym composable,
- brak ViewModeli dla ekranów,
- brak warstwy repository,
- brak use case'ów,
- brak dependency injection,
- brak rozdzielenia modeli domenowych, bazodanowych i sieciowych.

### 3.4. Dane nie są gotowe do użycia produkcyjnego

Aktualny model ogłoszenia nie zawiera m.in.:

- autora,
- zdjęć,
- daty utworzenia i modyfikacji,
- statusu publikacji,
- stanu przedmiotu,
- danych kontaktowych,
- współrzędnych,
- informacji o dostawie,
- liczby wyświetleń,
- identyfikatora zdalnego,
- informacji moderacyjnych.

### 3.5. Logowanie jest atrapą

Profil zapisuje wyłącznie imię, e-mail i wartość `loggedIn` w SharedPreferences. Nie ma hasła, tokenu, sesji, rejestracji, resetu hasła ani backendu.

### 3.6. Wiadomości są statyczne

Nie można otworzyć rozmowy, wysłać wiadomości ani przechowywać historii. Wszystkie rozmowy pochodzą z `SampleData`.

## 4. Braki funkcjonalne

### Ogłoszenia

- brak ekranu szczegółów,
- brak edycji,
- brak usuwania,
- brak galerii zdjęć,
- brak aparatu i wyboru zdjęć,
- brak podglądu przed publikacją,
- brak wersji roboczych,
- brak statusów aktywne/wygasłe/sprzedane,
- brak zgłaszania i moderacji,
- brak udostępniania ogłoszenia.

### Wyszukiwanie i filtry

- wyszukiwanie obejmuje tylko tytuł i lokalizację,
- brak ceny minimalnej i maksymalnej,
- brak sortowania,
- brak promienia od lokalizacji,
- brak historii wyszukiwania,
- brak zapisanych wyszukiwań,
- brak filtrów zależnych od kategorii.

### Profil

- brak rejestracji i prawdziwego logowania,
- brak zdjęcia profilowego,
- brak edycji danych,
- brak listy własnych ogłoszeń,
- brak ustawień,
- brak ustawień prywatności i powiadomień,
- brak blokowania użytkowników.

### Wiadomości

- brak ekranu rozmowy,
- brak wysyłania tekstu i zdjęć,
- brak statusów dostarczenia i odczytu,
- brak powiązania rozmowy z ogłoszeniem,
- brak powiadomień push.

### Funkcje lokalne

- brak mapy,
- brak geolokalizacji,
- brak pogody,
- brak aktualnych wydarzeń,
- brak aktualności,
- brak integracji z rzeczywistymi źródłami danych.

### Pozostałe

- brak reklam,
- brak panelu moderacji,
- brak regulaminu i polityki prywatności,
- brak analityki i raportowania błędów,
- brak mechanizmu aktualizacji danych,
- brak powiadomień.

## 5. Problemy UI i UX

- brak dark mode,
- brak edge-to-edge,
- brak responsywności dla tabletów i dużych ekranów,
- brak obsługi Window Size Classes,
- ikony są zastąpione znakami Unicode i emoji,
- brak opisów dostępności,
- brak pełnej obsługi czytników ekranu,
- wiele kolorów jest wpisanych na stałe,
- brak kompletnej typografii Material 3,
- brak wspólnego systemu odstępów, kształtów i komponentów,
- brak stanów ładowania, pustych danych i błędów sieciowych,
- brak animacji przejść i mikrointerakcji,
- karta ogłoszenia nie jest otwieralna,
- brak zdjęć w kartach,
- ekran startowy nie ma panoramy Legnicy,
- wydarzenia i aktualności są statyczne i bez zdjęć.

## 6. Problemy bezpieczeństwa i prywatności

- `allowBackup=true` przy przechowywaniu profilu w SharedPreferences,
- brak szyfrowania danych lokalnych,
- brak bezpiecznego przechowywania tokenów,
- brak walidacji danych po stronie serwera,
- brak autoryzacji operacji,
- brak ochrony przed nadużyciami i spamem,
- brak polityki usuwania konta i danych,
- brak konfiguracji bezpieczeństwa sieci.

## 7. Testy i jakość

Brakuje:

- testów jednostkowych,
- testów ViewModeli,
- testów repository,
- testów bazy danych,
- testów UI Compose,
- testów nawigacji,
- testów dostępności,
- screenshot tests,
- testów wydajności,
- lint jako obowiązkowego kroku CI,
- statycznej analizy kodu,
- testów release build.

## 8. Kolejność realizacji

### ETAP 1 — stabilizacja projektu i CI

1. Naprawić błędy kompilacji.
2. Dodać Gradle Wrapper.
3. Dodać `strings.xml`, ikony i brakujące zasoby.
4. Skonfigurować debug/release, R8 i ProGuard.
5. Rozszerzyć CI o `test`, `lint` i `assembleDebug`.
6. Dodać podstawowe testy uruchamiane w CI.

### ETAP 2 — architektura i warstwa danych

1. Wprowadzić MVVM i jednokierunkowy przepływ stanu.
2. Rozdzielić warstwy data/domain/ui.
3. Dodać ViewModele i SavedStateHandle.
4. Dodać Room dla danych lokalnych.
5. Dodać DataStore dla preferencji.
6. Dodać repository i interfejsy źródeł danych.
7. Dodać dependency injection.

### ETAP 3 — system projektowy i nawigacja

1. Pełny Material 3.
2. Jasny i ciemny motyw.
3. Edge-to-edge i poprawne insets.
4. Adaptacyjny układ telefon/tablet.
5. Prawidłowe ikony Material.
6. Wspólne komponenty, typografia, kształty i odstępy.
7. Typowana nawigacja i obsługa deep linków.

### ETAP 4 — kompletne ogłoszenia

1. Nowy model ogłoszenia.
2. Lista, szczegóły i pełna karta.
3. Dodawanie, edycja i usuwanie.
4. Galerie zdjęć i wybór zdjęć.
5. Walidacja formularzy.
6. Statusy ogłoszeń.
7. Moje ogłoszenia.
8. Udostępnianie i zgłaszanie.

### ETAP 5 — wyszukiwanie, filtry i ulubione

1. Pełnotekstowe wyszukiwanie.
2. Filtry ceny, kategorii i lokalizacji.
3. Sortowanie.
4. Historia i zapisane wyszukiwania.
5. Trwałe ulubione powiązane z kontem.

### ETAP 6 — konto i profil

1. Rejestracja.
2. Logowanie i wylogowanie.
3. Reset hasła.
4. Sesja użytkownika.
5. Edycja profilu i avatar.
6. Ustawienia prywatności i powiadomień.
7. Usunięcie konta i eksport danych.

### ETAP 7 — wiadomości

1. Lista rozmów.
2. Ekran rozmowy.
3. Wysyłanie wiadomości.
4. Powiązanie z ogłoszeniem.
5. Statusy odczytu.
6. Blokowanie i zgłaszanie użytkowników.
7. Powiadomienia push.

### ETAP 8 — funkcje lokalne

1. Mapa i geolokalizacja.
2. Odległość od ogłoszenia.
3. Pogoda dla Legnicy.
4. Aktualne wydarzenia.
5. Aktualności lokalne.
6. Mechanizm aktualizacji i cache.

### ETAP 9 — ekran startowy i dopracowanie UX

1. Panorama Legnicy.
2. Nowy HomeScreen.
3. Sekcje aktualności, wydarzeń i promowanych ogłoszeń.
4. Animacje i mikrointerakcje.
5. Dostępność.
6. Obsługa pustych stanów i błędów.
7. Optymalizacja obrazów i list.

### ETAP 10 — monetyzacja i bezpieczeństwo

1. Miejsca reklamowe bez pogarszania UX.
2. Oznaczanie ofert promowanych.
3. Moderacja treści.
4. Zgłoszenia i blokady.
5. Regulamin i polityka prywatności.
6. Analityka i raportowanie awarii.

### ETAP 11 — testy końcowe i publikacja

1. Testy jednostkowe, integracyjne i UI.
2. Testy różnych rozmiarów ekranów.
3. Testy dark mode i dostępności.
4. Profilowanie wydajności.
5. Release signing.
6. App Bundle.
7. Dokumentacja publikacji w Google Play.
8. Kontrola bezpieczeństwa i prywatności.

## 9. Zasady dalszej pracy

- wszystkie zmiany powstają na `android-complete-v3`,
- brak pracy bezpośrednio na `main`,
- brak Pull Requestów,
- brak scalania gałęzi,
- commit po każdym większym, działającym etapie,
- każdy etap kończy się kontrolą kompilacji, testów i krótkim raportem.
