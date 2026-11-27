package fr.evacal.widget

/** Fictional sample payload; host and token are placeholders. */
object Sample {
    const val JSON = """{
  "generatedAt": "2026-11-27T12:00:00.000Z",
  "flag": null,
  "quota": { "peak": {"total": 6, "left": 4}, "offPeak": {"total": 2, "left": 0}, "daysLeft": 12, "endsAt": "2026-12-09" },
  "level": { "level": 37, "progressionPct": 62 },
  "bookings": [
    { "startLocal": "2026-12-01T19:20", "game": "After-H Battle Arena", "players": ["Roro", "Kaz"] },
    { "startLocal": "2026-12-03T20:40", "game": "Moon of the Dead", "players": [] },
    { "startLocal": "2026-12-05T14:00", "game": "Stellar Descent", "players": ["Roro"] },
    { "startLocal": "2026-12-08T18:10", "game": "After-H Battle Arena", "players": [] },
    { "startLocal": "2026-12-10T21:00", "game": "Moon of the Dead", "players": [] },
    { "startLocal": "2026-12-12T11:30", "game": "Stellar Descent", "players": [] },
    { "startLocal": "2026-12-15T17:00", "game": "After-H Battle Arena", "players": [] },
    { "startLocal": "2026-12-19T19:40", "game": "Moon of the Dead", "players": [] }
  ],
  "renewsAt": "2026-12-09"
}"""
    val data: WidgetData get() = Parser.parse(JSON)!!
}
