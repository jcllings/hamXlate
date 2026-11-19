# hamXlate
A tool for translating Amateur Radio handset exports. For example, from a specific AnyTone model to a specific Baofeng model.

## Run

Requires Java 17 and Maven.

```sh
mvn spring-boot:run
```

List available format IDs with `GET http://localhost:8080/api/formats`. Upload a file
and download the converted CSV using multipart form data:

```sh
curl -f -o translated.csv -X POST http://localhost:8080/api/translations \
  -F 'file=@channels.csv' -F 'inputFormat=name-first' -F 'outputFormat=frequency-first'
```

The initial formats are **generic examples**, not vendor-specific radio formats:

| ID | CSV header |
| --- | --- |
| `name-first` | `Name,Frequency` |
| `frequency-first` | `Frequency,Name` |

Headers are required, case-sensitive, and must be in the specified order. Files
must be UTF-8 (an optional BOM is accepted), at most 10 MB, and use comma-separated
CSV with standard double-quote escaping. Header-only files are supported.
Values, including frequency text, are preserved; the application does not
validate radio-specific values. Invalid input or an unknown format returns HTTP
400. Treat downloaded CSV as untrusted when opening it in spreadsheet software:
cells beginning with formula characters may be interpreted as formulas.

To add a format, implement `CsvFormat` to read and write the shared `Channel`
representation and register it as a Spring bean with a unique ID. Extend the
shared representation as needed when implementing real device formats.

Run the tests with `mvn test`.
