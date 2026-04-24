# Display

## Opis
Część (part) montowana na transition plane, która wyświetla dynamiczne treści na ścianie. Może prezentować tekst, obrazy oraz dane z systemu AE2 poprzez system tokenów.

## Funkcjonalności

### Wyświetlanie tekstu
- Wprowadzanie dowolnego tekstu w GUI
- Obsługa formatowania kolorów (`&c`, `&b#RRGGBB`)
- Podświetlanie składni dla tokenów
- Opcje wyrównania (centered/left)
- Dodawanie marginesów

### Tokeny
System tokenów pozwala na dynamiczne wyświetlanie danych:
- `&d^` - tokeny debugowe
- `&s^` - tokeny statusu (storage, etc.)
- `&i^` - tokeny obrazków

### Obrazy
- Wgrywanie własnych obrazów przez GUI
- Podgląd obrazów w podmenu
- Możliwość ustawienia pozycji i rozmiaru (0-100%)
- Przechowywanie danych obrazów w NBT

### Merge Mode
- Połączenie wielu displayi w jedną dużą powierzchnię wyświetlania
- Automatyczne wykrywanie sąsiednich displayi w tej samej płaszczyźnie
- Tworzenie prostokątnych grup renderujących
- Obsługa obrotu (spin) dla ścian poziomych

## GUI
Główne okno Display zawiera:
- Pole tekstowe do wprowadzania treści
- Selektor koloru tła
- Selektor koloru tekstu
- Toggle dla Merge Mode
- Toggle dla Center Text
- Toggle dla Add Margin
- Przycisk do edycji obrazów
- Podgląd renderowania z tokenami

## Instalacja
- Montowana na transition plane (jak każdy AE2 part)
- Wymaga zasilania z sieci AE2
- Obrót ustawia się automatycznie na podstawie kierunku patrzenia gracza przy umieszczeniu (tylko dla ścian poziomych UP/DOWN)

## Interakcja
- Kliknięcie prawym przyciskiem - otwiera GUI grupy displayi (jeśli merge mode włączony)
- Shift + kliknięcie prawym przyciskiem - otwiera GUI pojedynczego displaya

---

# Emitter Terminal + Wireless

## Opis
Terminal do zarządzania wszystkimi ME Level Emitters w sieci AE2 z jednego miejsca. Dostępny jako part montowany na transition plane oraz jako przedmiot bezprzewodowy.

## Funkcjonalności

### Zarządzanie emiterami
- Wyświetlanie listy wszystkich aktywnych ME Level Emitters w sieci
- Podgląd ustawień każdego emitera (item, wartość threshold)
- Edycja wartości threshold dla każdego emitera z poziomu terminala
- Wyszukiwanie emiterów po nazwie lub itemie

### Interfejs
- 6 widocznych wierszy emiterów jednocześnie
- Paski przewijania dla dłuższych list
- Pole wyszukiwania z filtrowaniem
- Pola tekstowe do edycji wartości threshold
- Obsługa wyrażeń matematycznych w wartościach (np. `100/2`, `50+10`)

### Wireless Emitter Terminal
- Mobilna wersja emitter terminala jako przedmiot
- Użycie z dowolnego miejsca w zasięgu wireless terminala
- Obsługa upgrade slotów (przez AE2WTLib)
- Integracja z WUT (Wireless Universal Terminals)

## GUI
### Emitter Terminal (Part)
- Lista emiterów z ikonkami itemów
- Pola tekstowe do edycji threshold
- Pole wyszukiwania na górze
- Scrollbar po prawej stronie

### Wireless Emitter Terminal
- Ten sam interfejs co part wersja
- Dodatkowe sloty na upgrade po prawej
- Przycisk cyklowania terminali (jeśli WUT włączony)

---

- redstone terminal + wireless
# Wireless Notification Terminal

## Opis
Bezprzewodowy terminal który monitoruje poziomy itemów w ME Storage i wyświetla powiadomienia na HUD. Pozwala na ustawienie threshold dla 16 różnych itemów i otrzymywanie wizualnych alertów gdy poziom spadnie poniżej ustawionej wartości.

## Funkcjonalności

### Monitorowanie poziomów
- 16 slotów do konfiguracji monitorowanych itemów
- Ustawianie indywidualnego threshold dla każdego itemu
- Automatyczne wykrywanie gdy poziom itemu spadnie poniżej threshold
- Wyświetlanie powiadomień na HUD w czasie rzeczywistym

### HUD Overlay
- Wyświetlanie ikon itemów z ich aktualnym poziomem
- Konfigurowalna pozycja HUD (osi X i Y w procentach)
- Opcje ukrywania powiadomień (gdy poziom powyżej/poniżej threshold)
- Automatyczne usuwanie powiadomień po 2.5 sekundzie
- Koloryzacja w zależności od poziomu (zielony/pomarańczowy/czerwony)

### Konfiguracja
- Pola tekstowe do wprowadzania threshold (obsługa wyrażeń matematycznych)
- Toggle dla Hide Above / Hide Below
- Slidery/pola do ustawienia pozycji HUD
- Upgrade sloty (przez AE2WTLib)
- Integracja z WUT

## GUI
- 16 slotów filtru na itemy (6 widocznych + scrollbar)
- Pola tekstowe do threshold obok każdego slotu
- Pola do konfiguracji pozycji HUD (X/Y)
- Checkboxy Hide Above / Hide Below
- Upgrade sloty po prawej stronie

---

# Multi Level Emitter

## Opis
Zaawansowany Storage Level Emitter z obsługą 16 itemów jednocześnie. Pozwala na tworzenie złożonych warunków wyjścia redstone poprzez logikę AND/OR oraz monitorowanie statusu craftingu dla każdego itemu osobno.

## Funkcjonalności

### Wiele filtrów
- 16 slotów do konfiguracji monitorowanych itemów
- Indywidualny threshold dla każdego itemu
- 6 widocznych slotów naraz z scrollbar

### Logika AND/OR
- Tryb OR (domyślny) - wyjście aktywne gdy choć jeden warunek jest spełniony
- Tryb AND - wyjście aktywne tylko gdy wszystkie warunki są spełnione
- Przełączanie przez przycisk w GUI

### Typy porównań
- Każdy slot może być ustawiony na `>=` (greater or equal) lub `<` (less than)
- Przełączanie typu porównania dla każdego slotu osobno przez przycisk

### Craft Monitoring
- Opcja emitowania sygnału gdy item jest w trakcie craftingu
- Konfigurowalne dla każdego slotu osobno
- Przydatne do wykrywania gdy coś jest craftowane a nie tylko gdy jest w storage

### Standardowe opcje
- Fuzzy Mode upgrade
- Redstone Mode (always on/off/inverted)
- Craft via Redstone

## GUI
- 16 slotów filtru (6 widocznych + scrollbar)
- Pola tekstowe do threshold obok każdego slotu
- Przyciski przełączające `>=` / `<` dla każdego slotu
- Przyciski do craft monitoring
- Przycisk AND/OR na górze
- Fuzzy Mode button
- Upgrade slot

---

# Tag Level Emitter

## Opis
Storage Level Emitter który zamiast konkretnych itemów używa wyrażeń z tagami. Pozwala na monitorowanie całkowitej ilości wszystkich itemów/fluidów pasujących do złożonego wyrażenia tagowego.

## Funkcjonalności

### Wyrażenia tagowe
- Obsługa tagów itemów i fluidów
- Logika booleanowska: `AND`, `OR`, `NOT`
- Nawiasy do grupowania wyrażeń
- Przykład: `(minecraft:planks) AND (NOT:birch)`

### Monitorowanie
- Sumowanie ilości wszystkich itemów pasujących do wyrażenia
- Threshold do ustawienia poziomu wyjścia redstone
- Standardowe opcje porównania (`>=` / `<`)

### Standardowe opcje
- Redstone Mode (high signal/low signal/inverted)
- Fuzzy Mode upgrade
- Craft via Redstone

## GUI
- Wieloliniowe pole tekstowe na wyrażenie (główny obszar)
- Pole tekstowe na threshold
- Przycisk Apply (Enter icon)
- Redstone Mode button
- Upgrade slot

---

- redstone emitter
- wormhole
- rr item/fluid p2p 
- portable spatial storage
- portable spatial cloner
- cpu priority tuner
- tag view cell
- pattern multiplier
- crazy upgrade / crazy pattern provider + part
- ejector
- recipe fabricator