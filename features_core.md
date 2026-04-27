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
- Obsługa tagów itemów i fluidów (pełna ścieżka: `namespace:tag_path`)
- Logika booleanowska z operatorami:
  - `&` lub `AND` - koniunkcja (precedence: 2)
  - `|` lub `OR` - alternatywa (precedence: 0)
  - `^` lub `XOR` - alternatywa wykluczająca (precedence: 1)
  - `!` lub `NOT` - negacja (precedence: 3, right-associative)
- Nawiasy `( )` do grupowania wyrażeń
- Glob patterns z `*` jako wildcard (np. `minecraft:stone/*`)
- Przykłady:
  - `minecraft:planks` - wszystkie deski
  - `minecraft:planks & !minecraft:birch` - deski poza bruzowymi
  - `(minecraft:stone) | (minecraft:dirt)` - kamień lub ziemia
  - `c:logs` - wszystkie logs z tagu Create

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

# Redstone Terminal + Wireless

## Opis
Terminal do sterowania wszystkimi Redstone Emitters w sieci AE2. Pozwala na szybkie przełączanie stanów (on/off) wielu emiterów z jednego miejsca. Dostępny jako part oraz jako przedmiot bezprzewodowy.

## Funkcjonalności

### Zarządzanie Redstone Emitters
- Wyświetlanie listy wszystkich aktywnych Redstone Emitters w sieci
- Podgląd nazwy i aktualnego stanu każdego emitera (on/off)
- Przełączanie stanu emitera jednym kliknięciem przycisku
- Wyszukiwanie emiterów po nazwie

### Interfejs
- 7 widocznych wierszy emiterów jednocześnie
- Przyciski toggle z ikoną redstone (wysoki/niski sygnał)
- Paski przewijania dla dłuższych list
- Pole wyszukiwania z filtrowaniem
- Wizualna indykacja stanu (aktywny/nieaktywny)

### Wireless Redstone Terminal
- Mobilna wersja jako przedmiot
- Użycie z dowolnego miejsca w zasięgu wireless
- Obsługa upgrade slotów (przez AE2WTLib)
- Integracja z WUT (Wireless Universal Terminals)

## GUI
- Lista emiterów z nazwami
- Przyciski toggle (redstone high/low ikony)
- Pole wyszukiwania na górze
- Scrollbar po prawej stronie
- Upgrade sloty (tylko wireless wersja)

---

# Redstone Emitter

## Opis
Prosty part który emituje sygnał redstone i może być zdalnie sterowany przez Redstone Terminal. Każdy emitter ma unikalną nazwę (domyślnie losowy 4-znakowy hex ID) do identyfikacji.

## Funkcjonalności

### Sygnał redstone
- Emituje sygnał redstone w zależności od stanu (on/off)
- Domyślny stan: OFF
- Stan jest przechowywany w NBT

### Nazwa
- Każdy emitter ma unikalną nazwę do identyfikacji
- Domyślnie: losowy 4-znakowy hex ID (np. `A3F7`)
- Możliwość zmiany nazwy w GUI
- Nazwa wyświetlana w Redstone Terminal

### Sterowanie
- Stan zmieniany przez Redstone Terminal (toggle)
- Wszystkie emitters z tą samą nazwą są przełączane razem
- Przydatne do grupowania emiterów

## GUI
- Pole tekstowe na nazwę
- Przycisk Apply (Enter icon)
- Double-click w polu tekstowym czyści zawartość
- Enter zapisuje nazwę

---

# Wormhole

## Opis
Zaawansowany P2P Tunnel który działa jako **universal capability proxy** - przenosi wszystkie Forge capabilities przez tunel wraz z channel capacity. Pozwala na klikanie w bloki, otwieranie GUI, i nawet teleportację przez odległe odległości lub inne dimensje.

## Funkcjonalności

### Universal Capability Proxy
- Przenosi **wszystkie Forge capabilities** przez tunel
- Działa jako transparentny proxy dla dowolnej capability
- **Łączy handlery** item/fluid/energy itp. przez tunel
- Pozwala na podłączanie kabli, pipe'ów, conduitów przez tunel
- Obsługa energy, fluid, item, i custom capabilities z innych modów
- Mechanizm: `WormholeP2PCapabilityProxy` redirectuje wszystkie `getCapability` calls na blok na drugim końcu tunelu

### Przenoszenie interakcji
- Klikanie w bloki przez tunel aktywuje je zdalnie
- Otwieranie GUI bloków przez tunel
- Interakcje z machine'ami, doorami, buttonami itd.
- Obsługa wymiany itemów (np. crafting tables, furnaces)

### Teleportacja
- Użycie Ender Pearl przez tunel teleportuje gracza na drugi koniec
- Konfigurowalne w config (domyślnie włączone)
- Działa między dimensjami
- Konsumuje ender pearl (chyba że creative mode)

### Import/Export ustawień
- Obsługa Memory Card do kopiowania frequency
- Standardowe P2P funkcje (upgrades, frequency sync)

### Zaawansowane opcje
- P2P Wormhole Nesting (config) - pozwala na łączenie wielu wormhole'ów
- Działa z dowolnymi blokami i ich GUI
- Wormhole Anchor utrzymuje pozycję podczas otwartego GUI

## GUI
- Standardowe P2P Tunnel GUI
- Frequency selector
- Upgrade slots
- Direction indicators

---

# RR Item/Fluid P2P

## Opis
Round Robin P2P Tunnel który automatycznie rozdziela itemy/fluidy równomiernie między wszystkie połączone wyjścia w rotacyjnej kolejności. Działa jak "smart splitter" który balansuje load między wieloma machine'ami.

## Funkcjonalności

### Round Robin Distribution
- **RR Item P2P**: przyjmuje itemy na input i dzieli je równo na wszystkie outputs
- **RR Fluid P2P**: przyjmuje fluidy na input i dzieli je równo na wszystkie outputs
- Rotacyjna kolejność - każde wyjście otrzymuje swoją kolejność w cyklach
- Automatyczny balans load między wieloma machine'ami

### Mechanizm działania
- Input handler przyjmuje resources i dzieli je: `amount / outputCount` na każde wyjście
- Reszta (`amount % outputCount`) rozdzielana kolejno na pierwsze wyjścia
- `containerIndex` trackuje progresję między operacjami
- Output handler działa jak normalny P2P (możliwy extract/drain z inputu)

---

# CPU Priority Tuner

## Opis
Przedmiot który pozwala ustawiać priorytet dla Crafting CPU. CPU z wyższym priorytetem otrzymują crafting jobs w pierwszeństwie przed CPU z niższym priorytetem. Wymaga to szerokiej sieci mixinów aby zmodyfikować zachowanie CraftingService i GUI.

## Funkcjonalności

### Ustawianie priorytetu
- Kliknij prawym przyciskiem na Crafting CPU z CpuPrioTunerItem
- Otwiera GUI z polem tekstowym do wprowadzenia priorytetu (-1M do +1M)
- Priorytet jest zapisywany w NBT CPU (tag `CrazyPrio`)
- Domyślny priorytet: 0

### Mixinowe zmiany
Feature wymaga wielu mixinów aby zapewnić spójne zachowanie:

**CraftingService:**
- `insertIntoCpus` - wstawia jobs do CPU posortowanych po priorytecie (najwyższy pierwszy)
- `onServerEndTick` - iteruje po CPU w kolejności priorytetu
- `getCpus` - zwraca CPU posortowane po priorytecie

**CraftingCPUCluster:**
- Implementuje `ICpuPrio` interfejs
- Zapisuje/odczytuje priorytet z NBT
- Trackuje live owner name dla display

**CraftingCPUCycler:**
- Sortuje CPU records po priorytecie w GUI

**CraftingStatusMenu:**
- Przypisuje priorytety do CraftingCpuListEntry

**CraftingCpuListEntry:**
- Implementuje `ICpuPrio` dla entries w GUI
- Synchronizuje priorytet przez network packets

**CPUSelectionList:**
- Sortuje entries w GUI po priorytecie (najwyższy na górze)

**CraftConfirmMenu:**
- Auto-select wybiera CPU z najwyższym priorytetem

### CpuPriorityHelper
Utility class który dostarcza comparatory:
- `clusterComparator()` - najwyższy priorytet pierwszy
- `clusterComparatorAscending()` - najniższy priorytet pierwszy
- `cpuComparator()` - dla ICraftingCPU
- `entryComparator()` - dla CraftingCpuListEntry
- `extendFastFirstComparator()` / `extendFastLastComparator()` - wrap existing comparators

## GUI
- Ikona target CPU
- Number entry widget dla priorytetu
- Pole tekstowe do wprowadzania wartości
- Przycisk Save

---

# Tag View Cell

## Opis
View Cell który zamiast konkretnych itemów używa wyrażeń tagowych. Pozwala na filtrowanie itemów w ME Terminal na podstawie tagów z obsługą logiki booleanowskiej, podobnie jak Tag Level Emitter.

## Funkcjonalności

### Wyrażenia tagowe
- Obsługa tagów itemów (pełna ścieżka: `namespace:tag_path`)
- Logika booleanowska z operatorami:
  - `&` lub `AND` - koniunkcja
  - `|` lub `OR` - alternatywa
  - `^` lub `XOR` - alternatywa wykluczająca
  - `!` lub `NOT` - negacja
- Nawiasy `( )` do grupowania wyrażeń
- Glob patterns z `*` jako wildcard
- Przykłady:
  - `minecraft:planks` - wszystkie deski
  - `minecraft:stone/*` - wszystkie varianty kamienia
  - `c:logs` - wszystkie logs z tagu Create

### Filtrowanie w ME Terminal
- Tag View Cell działa jak normalny view cell w ME Terminal
- Wyświetla tylko itemy pasujące do wyrażenia tagowego
- Używa `TagPriorityList` jako `IPartitionList`
- `TagMatcher.doesItemMatch()` sprawdza czy item pasuje do kryteriów

### Mixinowa integracja
- `MixinViewCellItem.createFilter` wykrywa TagViewCellItem w liście
- Jeśli znaleziony, tworzy `TagPriorityList` zamiast standardowego filtra
- Obsługa wielu Tag View Cell jednocześnie (łączenie filtrów)

## GUI
- Wieloliniowe pole tekstowe na wyrażenie tagowe
- Auto-save przy każdej zmianie
- Placeholder z instrukcją

---

# Pattern Multiplier

## Opis
Przedmiot który pozwala na mnożenie ilości inputów i outputów w patternach craftingowych. Działa jak "bulk converter" dla patternów - zamiast craftować 1 item, można craftować 64 items naraz z odpowiednim mnożeniem inputów.

## Funkcjonalności

### Mnożenie patternów
- Wprowadź mnożnik (np. 64, 16, 0.5)
- Wszystkie itemy w patternach w GUI są mnożone przez ten współczynnik
- Inputy i outputy są mnożone równomiernie
- Minimalna ilość: 1 (nie można mieć 0 items)

### Limit outputów
- Opcjonalny limit maksymalnej ilości outputów
- Przydatne do zapobiegania przepełnieniu stacków
- Jeśli limit ustawiony, sprawdza czy wszystkie ilości są podzielne przez divisor

### Aplikacja do różnych hostów
- **Interface**: aplikacja do patternów w Interface
- **Pattern Provider**: aplikacja do patternów w terminalu Pattern Provider
- **Container**: aplikacja do dowolnego containera z patternami
- **AppEngInternalInventory**: aplikacja do wewnętrznych inventory AE

### GUI
- 36 slotów na encoded patterns
- Pole tekstowe na mnożnik (obsługa wyrażeń matematycznych)
- Pole tekstowe na limit outputów
- Przycisk Apply Multiplier (Enter icon)
- Przycisk Clear All Patterns (Clear icon) - zamienia wszystkie patterns na blank patterns

### Formatowanie
- Decimal format: `#.######`
- Obsługa ułamków (np. 0.5 dla połowy ilości)
- Obsługa wyrażeń matematycznych (np. `64/2`, `16*4`)

## Użycie
1. Otwórz Pattern Multiplier (right-click)
2. Włóż encoded patterns do slotów
3. Wprowadź mnożnik (np. 64)
4. Opcjonalnie ustaw limit outputów
5. Kliknij Apply Multiplier
6. Wyciągnij zmodyfikowane patterns

### Alternatywne użycie
- Shift + right-click na Interface/Pattern Provider z Pattern Multiplier
- Aplikuje zapisany mnożnik i limit do patternów w host

---

# Crazy Pattern Provider Block/Part + Upgrade

## Opis
Rozszerzalny Pattern Provider dostępny jako block lub part, który obsługuje do 72 patternów (8*9) zamiast standardowych 36. Posiada upgrade sloty i dynamicznie rozszerzalną liczbę slotów przez mechanizm `added`.

## Funkcjonalności

### Dostępne wersje
- **Crazy Pattern Provider Block**: Block entity z upgrade slotami
- **Crazy Pattern Provider Part**: Part montowany na transition plane z upgrade slotami

### Rozszerzalna liczba slotów
- Domyślna liczba slotów: 72 (8*9)
- Mechanizm `added` pozwala na dynamiczne rozszerzanie
- Mixin `MixinPatternProviderLogic.crazyAE2Addons$setSize` zmienia rozmiar inventory
- Obsługa przez interfejs `IProviderLogicResizable`

### Upgrade sloty
- **1 slot** (domyślnie)
- **2 sloty** (jeśli AppFlux załadowany - Induction Card)
- Obsługa standardowych AE2 upgrade'ów
- AppFlux Induction Card (jeśli dostępny)

### GUI
- 9 kolumn x 4 widoczne wiersze (36 slotów widocznych naraz)
- Scrollbar dla dodatkowych slotów
- Upgrade panel (jeśli AppFlux nie załadowany)
- Tooltip z liczbą slotów: "X patterns"

### Sync i persistence
- LowDragLib sync dla `added` i `upgrades`
- `@DescSynced` dla sync na klient
- `@Persisted` dla zapisu w NBT
- `@UpdateListener` dla obsługu zmian

### Import/Export
- Memory Card support
- Dismantle item zachowuje patterns i state
- Wrench zachowuje patterns

---

# Ejector

## Opis
Block który craftuje skonfigurowane itemy i wyrzuca je na sąsiedni block (interface, pattern provider, inventory). Działa jak "automated crafter + ejector" w jednym bloku.

## Funkcjonalności

### Konfiguracja
- 36 slotów konfiguracyjnych na itemy do ejectowania
- 1 slot na pattern (do szybkiego ładowania configu z patterna)
- Każdy slot config może mieć ustawioną ilość itemów
- Right-click na slot config otwiera GUI do zmiany ilości

### Craftowanie i buforowanie
- `ManagedBuffer` zarządza craftowaniem i buforowaniem itemów
- Craftuje brakujące itemy przez `ICraftingRequester`
- Buforuje crafted itemy przed ejectowaniem
- Sprawdza czy wszystkie wymagane itemy są dostępne przed eject

### Ejectowanie
- Wyrzuca itemy w kierunku wskazanym przez block (FACING)
- Celuje w `PatternProviderTarget` (interface, pattern provider, inventory)
- Jeśli cel niedostępny, wraca itemy do ME Storage
- Wymaga kanału grid do działania

### Pattern loading
- Przycisk "Load Pattern" w GUI
- Wczytuje inputy z patterna do slotów config
- Automatycznie ustawia ilości z patterna
- Przydatne do szybkiej konfiguracji

### Status i feedback
- `isCrafting` flaga pokazuje czy trwa craftowanie
- `cantCraftStack` i `cantCraftCountText` pokazują brakujące itemy
- Ikona brakującego itemu wyświetlana w GUI
- Tooltip z informacją o brakujących itemach

### Tickowanie
- `IGridTickable` z min tick 5, max tick 5
- `tick()` w `ManagedBuffer` obsługuje craftowanie i eject
- `URGENT` tick modulation podczas craftowania lub flush
- `IDLE` gdy nic nie dzieje się

### Import/Export
- Memory Card support dla configu
- Dismantle item zachowuje config, pattern i buffer
- Buffer items są dropowane przy destrukcji

## GUI
- 36 slotów config (itemy do ejectowania)
- 1 slot na pattern
- Przycisk "Load Pattern" (Enter icon)
- Ikona brakującego itemu z licznikiem
- Text "Nothing" lub lista brakujących itemów
- Right-click na slot config otwiera GUI do zmiany ilości

---

# Recipe Fabricator

## Opis
Block podłączony do sieci AE2, który przetwarza surowce według niestandardowych przepisów (`fabrication` recipe type). Przyjmuje do 6 stacków itemów i opcjonalny fluid na wejście, produkuje item i/lub fluid na wyjście. Automatycznie wyrzuca gotowy produkt do sąsiedniego bloku.

## Funkcjonalności

### Przetwarzanie przepisów
- Własny typ receptury `fabrication` definiowany w JSON
- Do 6 niezależnych slotów wejściowych (każdy może mieć dowolną ilość)
- Opcjonalny fluid input (zbiornik 8000 mB)
- Jeden slot na output item + opcjonalny fluid output (zbiornik 8000 mB)
- Czas craftu: 10 ticków, pobór mocy: 2 AE/t (idle) / 16 AE/t (aktywny)

### Auto-eject
- Po zakończeniu receptury produkt jest automatycznie wyrzucany do sąsiedniego bloku
- Priorytet: strona z której ostatnio wpłynął surowiec
- Fallback: wszystkie pozostałe kierunki
- Jeśli żaden sąsiedni blok nie przyjmuje, output zostaje w slocie

### Capabilities
- Przyjmuje itemy przez `IItemHandler` z dowolnej strony (trafiają do slotów wejściowych)
- Przyjmuje i oddaje fluidy przez `IFluidHandler` (tank 0 = input, tank 1 = output)

### JEI/EMI
- Własna kategoria receptur z podglądem wejść i wyjść (items + fluidy)

## GUI
- 6 slotów item input
- 1 slot item output
- Dwa wirtualne sloty fluidów (input i output) — klikalne, tooltipem pokazują fluid i ilość w mB
- Pasek postępu z tooltipem pokazującym % ukończenia bieżącej receptury

---

# Portable Spatial Storage

## Opis
Przedmiot który wycina prostokątny obszar bloków ze świata (cut), zapisuje go w pamięci serwera i pozwala wkleić go z powrotem w innym miejscu (paste). Bloki są usuwane z oryginalnej lokalizacji. Wymaga AE2 ME storage do ładowania energii.

## Funkcjonalności

### Zaznaczanie i wycinanie
- Zaznaczenie dwóch narożników obszaru przez kliknięcie prawym przyciskiem
- Po zaznaczeniu obu narożników: wycina strukturę, usuwa bloki ze świata, zapisuje jako `StructureTemplate`
- Koszt: konfigurowalny AE per blok, mnożony przez odległość od punktu centralnego
- Obsługa konfigurowalnego limitu rozmiaru struktury

### Wklejanie
- Prawy przycisk w powietrzu: paste na blok wskazany raycastem (zasięg 50 bloków)
- Prawy przycisk na bloku: paste na kliknięty bok
- Sprawdza kolizje przed wklejeniem — odmawia jeśli jakiś blok koliduje z istniejącym
- Po wklejeniu usuwa zapisaną strukturę z pamięci serwera

### Transformacje w przestrzeni
- Flip poziomy, flip pionowy, obrót o 90° (1–3× naraz) — operacje wykonywane server-side na surowym NBT StructureTemplate
- Transformacje stosują Minecraft'owy system rotacji i mirroru BlockState, więc bloki kierunkowe (facing, connection, powered) obracają się poprawnie — piece, skrzynie, maszyny GregTech, kable AE2, covery rur itp.
- Dwa tryby: normalny (przesuwa strukturę) i "around origin" (Shift — rotuje zawartość w miejscu, bez przesuwania punktu centralnego)
- Stan transformacji śledzony jako side-map — kumuluje kolejne transformacje do poprawnego wyświetlania preview

### Preview
- Podczas trzymania przedmiotu w ręce: ghost render struktury na docelowej pozycji (raycast 50 bloków)
- Renderer obsługuje wszystkie typy przejść: solid, cutout, cutout_mipped, translucent — bloki wyglądają identycznie jak w świecie
- Block entity renderery też są uruchamiane (animacje, dynamiczne modele) przez `BlockEntityRenderer`
- Czerwone obramowania na blokach kolidujących z istniejącymi
- Niebieski, podświetlony box zaznaczenia podczas wybierania narożników
- VertexBuffery cachowane per (struktura × stan transformacji) — brak kosztów przeliczania przy każdej klatce
- GTCEu: osobny renderer (`PortableSpatialStoragePreviewRendererGTCEu`) dla poprawnych modeli maszyn GTCEu

### GUI
- Podgląd 3D zapisanej struktury (obracanie myszą, zoom scrollem)
- Przyciski transformacji: flip poziomy, flip pionowy, obrót zgodnie ze wskazówkami zegara
- Shift + transformacja: tryb "wokół punktu centralnego"
- Przyciski przesunięcia offsetu (lewo/prawo/góra/dół/przód/tył) z podglądem X/Y/Z
- Sloty na upgrade'y (energy upgrade zwiększa pojemność wewnętrzną)

---

# Portable Spatial Cloner

## Opis
Wariant Portable Spatial Storage który **kopiuje** strukturę zamiast wycinać — oryginalne bloki pozostają na miejscu. Paste w trybie survival wymaga posiadania odpowiednich bloków w ekwipunku gracza lub ME Storage. Obsługuje AE2 kable i party z zachowaniem ustawień i upgrade'ów.

## Funkcjonalności

### Kopiowanie i wklejanie
- Identyczne zaznaczanie jak PSS, ale bloki **nie są usuwane**
- Paste "best effort": wkleja wszystko co możliwe, pomija kolizje i brakujące zasoby
- Wynik paste: HUD pokazuje "Placed: X, Skipped: Y"

### Transformacje w przestrzeni
- Identyczny zestaw transformacji co PSS (flip, obrót, tryb around origin)
- Te same gwarancje poprawności BlockState — bloki kierunkowe obracają się razem z ich modelem
- AE2 cable bus: po wklejeniu przywraca ustawienia partów przez `importSettings(SettingsFrom.MEMORY_CARD)` i upgrade cardy — konfiguracja terminali, emiterów, level emitterów itp. jest zachowywana
- GTCEu: `schedulePostPlacementInit` uruchamia inicjalizację maszyn po wklejeniu (wieloblokowe struktury składają się poprawnie)

### Preview
- Identyczny ghost renderer jak PSS (wszystkie typy przejść, BE renderery, cache VertexBufferów)
- Podczas wybierania narożników: niebieski box zaznaczenia ze świecącymi narożnikami
- Kiedy struktura wklejona i zamknięte GUI: ghost nadal widoczny w świecie przy trzymaniu narzędzia

### Zarządzanie materiałami (tryb survival)
- Wymagane bloki pobierane z ekwipunku gracza, a następnie z ME Storage (przez AE2 grid link)
- Symulacja przed wykonaniem: sprawdza dostępność każdego bloku przed podjęciem próby
- Lista materiałów w GUI z informacją ile jest dostępne, a ile brakuje
- Przycisk craftowania brakujących itemów bezpośrednio z GUI (inicjuje crafting job w ME)

### AE2 Cable Bus
- Rozpoznaje bloki AE2 i odtwarza układ kabli i partów
- Przywraca ustawienia partów przez `importSettings(SettingsFrom.MEMORY_CARD, ...)`
- Przywraca upgrade cardy w partsach i blockentity'ach

### GUI
- Podgląd 3D, transformacje i offset — identyczne jak PSS
- Lista materiałów po lewej: item, dostępna ilość, brakująca ilość
- Przyciski "Craft" przy brakujących itemach
- Przycisk "Forget" — czyści zapisaną strukturę bez wklejania
- Dodatkowy slot upgrade'u względem PSS

# Analog card

daje 2 nowe tryby do me level emittera i do tag level emittera, outputuje analogowy sygnal na podstawie tego ile jest itemow w me
albo logarytmicznie albo liniowo na podstawie ustawionego progu, jak jest limit 64 a masz 32, liniowo da signal str 7/15 logarytmicznie 14/15


1. Display — Part montowany na ścianie, wyświetlający tekst, obrazy i dane AE2 z obsługą tokenów, kolorowania i merge mode do tworzenia dużych ekranów.
2. Emitter Terminal + Wireless — Terminal zarządzający wszystkimi ME Level Emitters w sieci z jednego miejsca, z edycją threshold i wersją bezprzewodową.
3. Wireless Notification Terminal — Bezprzewodowy terminal monitorujący 16 itemów i wyświetlający kolorowe alerty na HUD gdy poziom spadnie poniżej threshold.
4. Multi Level Emitter — Storage Level Emitter obsługujący 16 itemów jednocześnie z logiką AND/OR i monitorowaniem statusu craftingu.
5. Tag Level Emitter — Storage Level Emitter używający wyrażeń tagowych z logiką booleanowską zamiast konkretnych itemów.
6. Redstone Terminal + Wireless — Terminal do przełączania stanów wszystkich Redstone Emitters w sieci jednym kliknięciem, z wersją bezprzewodową.
7. Redstone Emitter — Prosty part emitujący sygnał redstone, zdalnie sterowany przez Redstone Terminal z unikalną nazwą identyfikacyjną.
8. Wormhole — Universalny P2P Tunnel przekazujący wszystkie capabilities, interakcje z blokami i teleportację (nawet między wymiarami) przez tunel.
9. RR Item/Fluid P2P — Round Robin P2P Tunnel równomiernie rozdzielający itemy lub fluidy między wszystkie wyjścia.
10. CPU Priority Tuner — Przedmiot ustawiający priorytet Crafting CPU, determinujący kolejność przydzielania zadań craftingu.
11. Tag View Cell — View Cell filtrujący ME Terminal według wyrażeń tagowych z logiką booleanowską.
12. Pattern Multiplier — Przedmiot mnożący ilości inputów i outputów w encoded patternach przez zadany współczynnik.
13. Crazy Pattern Provider Block/Part — Rozszerzalny Pattern Provider z 72 slotami (do 8×9) i dynamicznym zwiększaniem przez upgrade.
14. Ejector — Block automatycznie craftujący skonfigurowane itemy i wyrzucający je do sąsiedniego inventory lub pattern providera.
15. Recipe Fabricator — Block przetwarzający surowce według własnego typu receptury fabrication z opcjonalnym fluid I/O i auto-ejectem produktu.
16. Portable Spatial Storage — Przedmiot wycinający prostokątny obszar bloków ze świata, zapisujący go i wklejający w innym miejscu z obsługą transformacji i ghost preview.
17. Portable Spatial Cloner — Wariant PSS kopiujący strukturę (bez usuwania oryginału) z listą wymaganych materiałów i możliwością craftowania brakujących z GUI.
18. Analog Card — Upgrade do ME Level Emitter i Tag Level Emitter, outputujący analogowy sygnał redstone proporcjonalny do ilości itemów (liniowo lub logarytmicznie).    