## Spring Cron expression

A cron expression consists of six sequential fields:

```
second, minute, hour, day of month, month, day(s) of week
```

and is declared as follows

```
@Scheduled(cron = "* * * * * *")
```

We can also set the timezone as

```
@Scheduled(cron = "* * * * * *", zone = "Europe/Copenhagen)
```

#### Notes

| syntax  | means             | example          | explanation             |
| ------- | ----------------- | ---------------- | ----------------------- |
| *       | match any         | "* * * * * *"    | do always               |
| */x     | every x           | "*/5 * * * * *"  | do every five seconds   |
| ?       | no specification  | "0 0 0 25 12 ?"  | do every Christmas Day  |

#### Examples

|syntax                     |   means                                            |
| ------------------------- | -------------------------------------------------- |
| "0 0 * * * *"             |  the top of every hour of every day                |
| "*/10 * * * * *"          |  every ten seconds                                 |
| "0 0 8-10 * * *"          |  8, 9 and 10 o'clock of every day                  |
| "0 0/30 8-10 * * *"       |  8:00, 8:30, 9:00, 9:30 and 10 o'clock every day   |
| "0 0 9-17 * * MON-FRI"    |  on the hour nine-to-five weekdays                 |
| "0 0 0 25 12 ?"           |  every Christmas Day at midnight                   |
