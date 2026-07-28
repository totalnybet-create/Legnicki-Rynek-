# Legnicki Rynek Android — plan przebudowy

## Cel
Profesjonalna aplikacja Android zgodna z makietą, z backendem, kontami użytkowników, ogłoszeniami, wiadomościami oraz mobilnym panelem właściciela i moderatora.

## Kolejność
1. Stabilizacja Gradle, CI, testów i konfiguracji.
2. Architektura MVVM oraz warstwy data/domain/ui.
3. Backend, baza danych i autoryzacja ról.
4. Ogłoszenia: zdjęcia, szczegóły, edycja, statusy i moderacja.
5. Wyszukiwanie, filtry, ulubione i lokalizacja.
6. Wiadomości i powiadomienia.
7. Publiczny interfejs zgodny z makietą.
8. Panel właściciela i moderatora na telefonie.
9. Testy końcowe, APK debug/release i kopie zapasowe.

## Zasady
- Praca odbywa się na `android-final-legnicki-rynek`.
- `main` i wcześniejsze gałęzie pozostają nienaruszone.
- Każdy większy etap kończy się buildem, testami i kontrolą artefaktów.
- Żadne sekrety nie trafiają do repozytorium.
- APK będzie publikowane jako artefakt GitHub Actions i kopia na Google Drive.
