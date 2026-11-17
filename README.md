# DeltaCraft Shop + MultiMine — wersja 0.1.0 (pierwszy etap)

**Stan:** kod źródłowy do pierwszej próby kompilacji. NIE jest jeszcze przetestowany ani potwierdzony jako działający JAR na Minecraft 26.3.

## Co już jest
- `/bal` i `/money` — saldo gracza.
- `/mm` i `/multimine` — poziom, XP i mnożnik.
- Stone: bazowo $1 za zniszczenie w survivalu; nagroda rośnie ×1.10 na poziom.
- Zombie, skeleton, spider: $10; creeper: $15; inne moby klasy Monster: $30; moby klasy Animal: $20. Mnożnik MultiMine obowiązuje.
- MultiMine: +1 XP za dowolny zniszczony blok; za rudy +3 do +30 XP zgodnie z kodem.
- Pierwszy poziom wymaga 100 XP, następne progi ×1.15, zaokrąglane w górę.
- Saldo, poziom i XP zapisują się co 60 sekund i przy zatrzymaniu serwera w `world/deltacraft-shop/accounts/`.

## Co jeszcze NIE jest gotowe
- GUI sklepu i kategorie, cennik wszystkich bloków, kupno, sprzedaż, XP za sprzedaż.
- `/pay`, `/baltop`, komendy admina.
- Zabezpieczenia przed farmieniem MultiMine przez ponowne stawianie i niszczenie bloków.
- Dokładniejsza klasyfikacja wszystkich mobów, obsługa killów przez oswojone zwierzęta i wszystkie przypadki pośrednich obrażeń.
- Nie zweryfikowano zgodności nazw API / mapowań z Minecraft 26.3; GitHub Actions może zgłosić błędy do poprawy.

## Budowanie przez GitHub
1. Utwórz nowe repozytorium, np. `DeltaCraft-Shop` (nie nadpisuj StoneDrop).
2. Wgraj zawartość tego folderu do głównego katalogu repozytorium, razem z `.github/workflows/build.yml`.
3. Zakładka **Actions** -> **Build DeltaCraft Shop** -> **Run workflow**.
4. Jeżeli build przejdzie, pobierz artefakt `DeltaCraft-Shop-JAR`.
5. Jeśli wystąpi błąd, prześlij tekst pierwszego błędu `error:` z logu kompilacji. Poprawimy kod pod konkretne mapowania.

## Uzgodnione zasady przyszłego sklepu
- LPM na bloku w sklepie: osobne GUI zakupu z +1/+10/+64 i -1/-10/-64.
- PPM: sprzedaż jednej sztuki; Shift+PPM: sprzedaż wszystkich posiadanych sztuk tego bloku.
- Shift+LPM: brak akcji.
- Sprzedaż zawsze 80% ceny sklepowej, bez mnożnika MultiMine; XP za sprzedaż zależne od rodzaju bloku.
- Wszystkie bloki możliwe do zdobycia w Survivalu; ceny w konfiguracji.
