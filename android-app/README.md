# Legnicki Rynek — Android

Natywna aplikacja Android zbudowana w Kotlinie i Jetpack Compose.

## Obecne ekrany

- Główna: wyszukiwarka, kategorie, wydarzenia i ogłoszenia
- Kategorie: filtrowanie ofert
- Dodaj: formularz publikowania ogłoszenia
- Wiadomości: lista rozmów
- Profil: demonstracyjne logowanie

## Budowanie

Projekt wymaga Java 17, Android SDK 35 i Gradle 8.9.

```bash
gradle -p android-app assembleDebug
```

Plik APK powstaje w `android-app/app/build/outputs/apk/debug/app-debug.apk`.
