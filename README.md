# PartijaPro

Sistema drabužių perpardavėjams (Vinted ir pan.): nupirktos partijos kainą paskirsto kiekvienai prekei, pasiūlo pardavimo kainas ir pasako, kada prekę atpiginti.

PSP kursinis darbas.

- I etapas: [projektavimo dokumentas](docs/projektavimo-dokumentas.md)
- II etapas: veikiantis prototipas (šitas README)

## Ko reikia

- **JDK 21** (pvz. Eclipse Temurin 21). Patikrinti: `java -version`
- Maven atskirai diegti nereikia, yra Maven Wrapper (`mvnw`), jis pats parsisiunčia Maven 3.9.11
- Pirmą kartą reikia interneto (parsisiunčia dependencies)

DB yra H2 in-memory, nieko papildomai diegti nereikia. API raktų nėra.

## Paleidimas

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

Tada atidaryti http://localhost:8080

Config yra `src/main/resources/application.properties`:

- `PORT` env kintamasis, jei 8080 užimtas (pvz. `PORT=9090 ./mvnw spring-boot:run`)
- `partijapro.demo-data=true` - užkrauna demo duomenis paleidžiant
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:partijapro`, user `sa`, be slaptažodžio). Ten matosi, kad visose lentelėse yra `tenant_id`.

**Jei Windows vartotojo aplanke yra lietuviškų raidžių** (pvz. `C:\Users\Suško`), serveris gali nepasileisti su `Unable to establish loopback connection`. Tai JDK bug'as, ne projekto. Fix (PowerShell):

```bash
$env:JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=C:\Users\Public"; .\mvnw.cmd spring-boot:run
```

## Testai

```bash
./mvnw test
```

Turi būti `Tests run: 20, Failures: 0, Errors: 0` ir `BUILD SUCCESS`.

- `PricingEngineTest` - pagrindinis modulis be Spring ir DB. Visi 5 scenarijai iš I etapo dokumento (įprastas, apvalinimas, minimali kaina, rinkos kaina, per didelės rankinės savikainos) + keli papildomi
- `TenantIsolationTest` - vienas klientas negali perskaityti, matyti sąraše ar ištrinti kito kliento partijos (tikrinama ir per servisą, ir per API)
- `BatchApiTest` - visas scenarijus per API, klaida kai niekas neišsaugoma, nežinoma kategorija, "šiandien perkainoti" sąrašas

## Demo duomenys

Paleidus sukuriami 2 klientai (`DemoDataLoader`). Partijos sukuriamos per tą patį `BatchService`, tai kainos tikrai suskaičiuojamos, ne hardcoded.

| Klientas | Nustatymai | Kategorijos | Partijos |
|---|---|---|---|
| `lobis` (Vintažo lobis) | antkainis 100%, min 20%, mokestis 0% | striukė 3, džinsai 2, marškinėliai 1, suknelė 2 | Outlet dėžė #1, Urmo maišas (apvalinimas) |
| `spinta` (Antra spinta) | antkainis 80%, min 15%, mokestis 5% | striukė 4, džinsai 2, marškinėliai 1, batai 3 | Siunta iš Vokietijos |

Būklės koeficientai: nauja 1.2, labai gera 1.0, gera 0.8, patenkinama 0.6.

Outlet dėžė #1 = pavyzdys iš dokumento: 180 + 20 EUR, striukė ranka 50 EUR, 3 džinsai įkelti prieš 28 dienas, 9 marškinėliai (vieno rinkos kaina 9 EUR).

## Demo žingsniai

1. `lobis` -> Outlet dėžė #1. Savikainos 50 / 20 / 10, kainos 100 / 40 / 20, min 60 / 24 / 12, S = 200.
2. Paskutiniai marškinėliai: rinkos kaina 9, todėl kaina 12 (= min) ir žyma "nuostolinga rizika" (R5).
3. "Šiandien perkainoti": 3 džinsai, nauja kaina 32 (40 -> 36 -> 32). Pakeisti datą +42 dienas: džinsai 24 ir "reikia peržiūrėti" (36, 32, 28, 25, 24, ne 22) (R4).
4. Urmo maišas: 33.34 + 33.33 + 33.33 = 100.00 (R2).
5. "+ Nauja partija" -> "Pavyzdys" -> "Apskaičiuoti". Tas pats rezultatas kaip 1 punkte.
6. "+ Nauja partija" -> "Klaidos pavyzdys" -> "Apskaičiuoti". Klaida "Rankinės savikainos (210,00 €) viršija partijos savikainą (200,00 €)", sąraše nieko naujo neatsiranda.
7. Perjungti į `spinta`: matosi tik jos partija. "Atidaryti pagal ID" įvesti 1 -> "Partija 1 nerasta".
8. `./mvnw test`

Arba per curl:

```bash
curl -H "X-Tenant-Id: lobis" http://localhost:8080/api/batches/1
```

```bash
curl -H "X-Tenant-Id: spinta" http://localhost:8080/api/batches/1
```

Pirmas grąžina partiją, antras `404 {"error":"Partija 1 nerasta"}`.

## API

Visur, išskyrus `/api/tenants` ir `/api/conditions`, reikia header `X-Tenant-Id`.

| Endpoint | Ką daro |
|---|---|
| `GET /api/tenants` | klientų sąrašas (kol kas vietoj login) |
| `GET /api/categories` | kliento kategorijos |
| `GET /api/conditions` | būklės |
| `GET /api/batches` | kliento partijos |
| `POST /api/batches` | sukuria ir suskaičiuoja partiją (klaida -> 422, niekas neišsaugoma) |
| `GET /api/batches/{id}` | partija su savikainomis ir kainomis |
| `GET /api/batches/{id}/repricing?date=YYYY-MM-DD` | "šiandien perkainoti" (be datos = šiandien) |
| `DELETE /api/batches/{id}` | ištrina partiją |

POST pavyzdys:

```json
{
  "name": "Outlet dėžė", "purchasePrice": 180, "shippingCost": 20,
  "items": [
    {"category": "striukė", "condition": "LABAI_GERA", "manualCost": 50},
    {"category": "džinsai", "condition": "LABAI_GERA", "quantity": 3, "listedOn": "2026-10-01"},
    {"category": "marškinėliai", "condition": "LABAI_GERA", "quantity": 9}
  ]
}
```

## Struktūra

```
src/main/java/lt/partijapro/
  pricing/   pagrindinis modulis (R1-R5), be Spring ir be DB
    PricingEngine                - suskaičiuoja partiją ir kainos mažinimą
    CostAllocationStrategy       - interface (Strategy), kaip dalinti savikainą
    WeightedCostAllocation       - R1 + R2, pagal svorį + rankinės savikainos, centų dalinimas
    PriceCalculator              - R3 + R5, kaina su antkainiu/mokesčiu, rinkos kaina
    MarkdownPolicy               - interface (Strategy), kaip mažinti kainą
    FixedIntervalMarkdownPolicy  - R4, kas 14 d. -10%, ne žemiau min
  batch/     BatchService (use cases), BatchController (REST), JPA entities
  tenant/    klientai, jų nustatymai ir kategorijos
  web/       exceptions -> JSON klaidos
  config/    PricingConfig, čia parenkamos strategijos
  demo/      demo duomenys
src/main/resources/static/   UI (paprastas HTML + JS)
```

`BatchController -> BatchService -> PricingEngine`. `pricing` paketas nieko nežino apie Spring ar DB, todėl jį galima testuoti atskirai.

**Multitenancy:** visos kliento lentelės (`batches`, `items`, `categories`) turi `tenant_id`. Klientas ateina per `X-Tenant-Id`, `TenantService.require` patikrina, kad jis egzistuoja (kitaip 403). Partijos imamos tik per `findByIdAndTenantId` / `findAllByTenantIdOrderByIdDesc`, tai svetima partija tiesiog nerandama (404).

## Kas kitaip nei I etape

- H2 vietoj PostgreSQL, be Docker. Docker pagal užduotį III etape. Kadangi naudojamas JPA, pereiti į Postgres = pakeisti `application.properties` ir dependency.
- Paprastas HTML vietoj React (užduotyje parašyta, kad užtenka paprastos sąsajos).
- Nėra login ir rolių, klientas pasirenkamas sąraše. Tai III etapui.
- Pardavimai ir partijos pelnas dar nepadaryti.
- Būklės koeficientai (išskyrus labai gera = 1.0) sugalvoti pavyzdžiui.

## AI

Kodo ir testų juodraščiams naudotas Claude. Laukiamos testų reikšmės paimtos iš I etapo dokumento ir perskaičiuotos ranka.
