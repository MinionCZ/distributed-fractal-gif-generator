# DSVA - Semestrální práce

## Obecný popis

Práce se zabývá problémem ukončení distribuovaného výpočtu pomocí Dijkstra-Scholtenova algoritmu pro obecný orientovaný
graf.
Práce umožňuje vygenerovat gif fraktálu Mandelbrotovy množiny pomocí iterativního algoritmu a to až v rozlišení 4k.  
![image](https://gitlab.fel.cvut.cz/stengjir/dsva-semesteralwork/-/wikis/uploads/b46df7d93d347a9b40ddad5043df9259/output-git2.gif)

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

Parametry je možné přepsat pomocí vytvoření JSON souboru a vložení jeho jména/cesty jako argument při spouštění programu

```shell
java -jar dsva-semestral.jar ./example-properties.json
```

## Použití aplikace

Aplikace komunikuje s uživatele v interaktivním módu, kdy aplikace loguje jak do konzole, tak do souboru run.log.
Aplikace si od uživatele vyžadá cestu k JSON souboru obsahujicí parametry výpočtu. Po předání cesty k souboru uživatelem
aplikace spustí distribuovaný výpočet, pokud již neběží a o krocích informuje uživatele. Je dobré si povšimnout, že v
logu se objevují vektorové logické hodiny, které jsou seřazeny podle ip adres všech strojů, tedy na všech strojích mají
stejné pořadí, pokud by byly 2 ip adresy stejné, tak se řadí podle portů. Je dobré si povšimnout, že zelená hodnota
označuje hodnotu na aktuálním stroji.

Příklad vstupního JSON souboru:

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

### Struktura vstupního JSON souboru

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




