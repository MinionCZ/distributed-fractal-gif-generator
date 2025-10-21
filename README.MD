# DSVA - Semestrální práce

## Obecný popis

Práce se zabývá problémem ukončení distribuovaného výpočtu pomocí Dijkstra-Scholtenova algoritmu pro obecný orientovaný
graf.
Práce umožňuje vygenerovat gif fraktálu Mandelbrotovy množiny pomocí iterativního algoritmu a to až v rozlišení 4k.  
![output-git2](https://github.com/user-attachments/assets/7e21ca1a-670c-4083-a2b5-01527f5f4a18)

## Technologie

- Kotlin 2.0.20 - kompilovaný na Javu 21
- gRPC 3
- Gradle 8.8 - build aplikace

### Build aplikace

Aplikaci lze díky použitému gradle wrapperu stavět na všech počítačích s Javou 21 pomocí příkazu:

```shell
./gradlew clean build
```

Gradle wrapper automaticky stáhne správnou verzi gradlu na Váš počítač a projekt postaví. Postavený projekt lze také
stáhnout jako artefakt z pipeliny na fakultním gitlabu.

### Spuštění aplikace

Aplikaci lze spustit pomocí příkazu

```shell
java -jar dsva-semestral.jar 
```

Při puštění aplikace bez dalšího argumentu označujicí cestu k souboru obsahujicí nastavení aplikace, se použije toto
defaultni nastavení zabalené společně s aplikací v balíku jar.

```json
{
  "ip": "127.0.0.1",
  "port": 8990,
  "maxCalculationDuration": "PT300S",
  "batchSize": 50,
  "maxRequestRepeat": 5,
  "httpServerPort": 8080,
  "otherWorkstations": [
    {
      "ip": "127.0.0.1",
      "port": 14000
    }
  ]
}
```

V JSONu vidíme nastavené parametry pro konkrétní pracovní stanici:

- ip: IP adresa konkrétního uzlu
- port: Port konkrétního uzlu
- maxCalculationDuration: Maximální délka timeoutu po kterou stanice čeká na výsledek výpočtu delegovaného na další
  stanici
- batchSize: Udává velikost předaných dat dalším uzlům výpočtu ve snímcích
- maxRequestRepeat: Udává počet opakování volání rozhraní dalšího uzlu
- otherWorkstations: Seznam stanic účastnících se distribuovaného výpočtu
- httpServerPort - Port pro manažerský http server

Parametry je možné přepsat pomocí vytvoření JSON souboru a vložení jeho jména/cesty jako argument při spouštění programu

```shell
java -jar dsva-semestral.jar ./example-properties.json
```

## Použití aplikace

Aplikace komunikuje s uživatele v interaktivním módu, kdy aplikace loguje jak do konzole, tak do souboru run.log.
Distribuovaný výpočet lze spustit pomocí provolání RESTového endpointu /new-job, pokud již neběží a o krocích informuje
uživatele pomocí logů. Je dobré si povšimnout, že v logu se objevují vektorové logické hodiny, které jsou seřazeny podle
ip adres všech strojů, tedy na všech strojích mají
stejné pořadí, pokud by byly 2 ip adresy stejné, tak se řadí podle portů. Je dobré si povšimnout, že zelená hodnota
označuje hodnotu na aktuálním stroji.

### Endpointy

#### /new-job

POST endpoint sloužící pro spuštění distribuovaného výpočtu. Vrací 204, pokud vše proběhne v pořádku jinak vrátí 400
pokud je JSON/data nevalidní. Pokud již běží distribuovaný výpočet, tak vrátí 409.
Příklad vstupního JSON body pro POST /new-job request:

```json
{
  "imageProperties": {
    "width": 1920,
    "height": 1080
  },
  "gifProperties": {
    "numberOfFrames": 250,
    "duration": "PT10S",
    "filename": "/path-to-gif/output-git2.gif"
  },
  "juliaSetProperties": {
    "escapeRadius": 2.0,
    "startingOffset": {
      "real": -0.4,
      "imaginary": 0.6
    },
    "endingOffset": {
      "real": -0.4,
      "imaginary": 0.6
    },
    "bottomLeftCorner": {
      "real": -1.6,
      "imaginary": -1.1
    },
    "topRightCorner": {
      "real": 1.6,
      "imaginary": 1.1
    },
    "startingNumberOfIterations": 60,
    "endingNumberOfIterations": 500
  }
}
```

### Struktura vstupního JSONu

Vstupní JSON soubor je rozdělen do tří částí

#### ImageProperties

Atribut imageProperties udává rozlišení výsledného gifu v pixelech.

#### GifProperties

Atribut gitProperties udává parametry výsledného gifu, který vznikne výsledek výpočtu.

- NumberOfFrames - udává celkový počet snímků výsledného gifu
- Duration - udává celkové trvání gifu
- Filename - udává cestu k výslednému gifu

#### JuliaSetProperties

Atribut juliaSetProperties udává parametry Mandelbrotovy množiny pro výpočet.

- EscapeRadius - escape radius, který určuje, zdali bod patří či nikoliv do množiny
- StartingOffset - komplexní číslo určujicí startovní offset množiny
- EndingOffset - komplexní číslo určujicí konečný offset množiny
- BottomLeftCorner - komplexní číslo určujicí souřadnice spodního pravého rohu
- TopRightCorner - komplexní číslo určujicí souřadnice pravého horního rohu
- StartingNumberOfIterations - číslo určujicí počet iterací na začátku výpočtu
- EndingNumberOfIterations - číslo určujicí počet iterací na konci výpočtu

Parametry offset a numberOfIterations udávají hodnotu na začátku a na konci výpočtu, pokud se hodnoty liší tak aplikace
linearně podle počtu snímků použije hodnoty mezi startovní a koncovou hodnotou na výpočet jednotlivých snímků.

#### /join

POST endpoint pro připojení uzlu do topologie. Endpoint bere JSON body ve kterém je seznam uzlů, ke kterým se má
připojit. Vrací:

- 204 - uzel se úspěšně připojil
- 400 - JSON má nevalidní údaje nebo špatnou strukturu

Příklad struktury JSON body:

```json
[
  {
    "ip": "127.0.0.1",
    "port": 14000
  }
]
```

#### /kill

POST endpoint na zabití GRPC serveru a klienta na uzlu. Vrací:

- 204 - pokud byl uzel úspěšně zabit
- 409 - pokud již je uzel neaktivní

#### /revive

POST endpoint na oživení zabitého uzlu. Vrací:

- 204 - pokud byl uzel úspěšně vzkřísen
- 409 - pokud uzel již beží

#### /leave

POST endpoint, který přikáže uzlu se odhlásit z topologie a potom se vypnout.

- 204 - pokud se uzel úspěšně odhlásil
- 409 - pokud je již uzel odhlášený/zabitý

#### /set-delay

POST endpoint, který nastaví delay na síti pro uzel. Vrací:

- 204 - pokud uzel přijal nové nastavení v pořádku
- 400 - pokud je JSON ve špatném formátu, nebo je delay větší jak maximum (1 min)

Příklad JSON body:
```json
{
  "delay": "PT3S"
}
```

## Popis funkce uzlů

Každý uzel má od uživatele nastavené parametry a to včetně toho, kolik a kde se nachází další stanice. Dále má každý
uzel synchronizované uložiště parametrů distribuovaného výpočtu. Uzel si do uložiště job uloží buď při vytvoření tasku
uživatelem, nebo pokud dostanou přes gRPC pokyn k výpočtu. Pokud už provádí nějaký výpočet, tak odmítne participovat na
dalším výpočtu. Dále si každý uzel pamatuje komu a jaké výpočty přeposlal/zdali aktuálně někomu vypočítává nějaké
snímky.

### Průběh výpočtu

1. Uživatel zadá nový výpočet do nějakého uzlu
2. Uzel zkusí distribuovat práci ostatním uzlům, pokud se mu to podaří a na vedlejším vlákně spustí výpočet u sebe
3. Pokud kterýkoliv uzel dokončí práci, tak napočítané hodnoty odešle zpět uzlu, který mu práci přiřadil a zeptá se
   ostatních uzlů, zdali pro něj nemají práci
    1. Pokud mu nějaký uzel vrátí požadavek na práci, tak začne vypočet práce pro tento uzel
    2. Pokud mu žádný uzel nevrátí požadavek na práci, tak považuje výpočet za ukončený a odebere data z výpočtu ze
       synchronizovaného uložiště
4. Pokud kterýkoliv uzel, který se účastní výpočtu dostane požadavek na to, zdali nemá nějakou práci tak odpoví jedním
   ze 2 způsobů
    1. Pokud má dostatek snímků, které může předat dál, tak se podělí o svou práci a uloží si, že konkrétnímu uzlu
       poskytl zadání práce. Pak pokračuje ve svém výpočtu. Po dokončení svého výpočtu vyčká na timeout jobu a nebo na
       návrat hodnot, až poté odesílá výsledek původnímu uzlu
    2. Pokud uzel nemá dostatek snímků, tak vrátí prázdný list zadání značící nedostatek práce
5. Hlavní uzel po dokončení lokálního výpočtu čeká na distribuované joby, pokud joby vytimeoutují tak začne s výpočtem
   sám
6. Po získání všech vypočtených snímků, hlavní uzel složí a uloží výsledný gif



