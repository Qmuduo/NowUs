from pathlib import Path

MIGRATIONS = Path(__file__).resolve().parents[1] / "migrations"


def connect(database_url: str):
    import psycopg
    from psycopg.rows import dict_row

    return psycopg.connect(database_url, connect_timeout=5, row_factory=dict_row)


def apply_migrations(database_url: str, migrations_dir: Path = MIGRATIONS) -> None:
    with connect(database_url) as connection:
        connection.execute(
            "CREATE TABLE IF NOT EXISTS schema_migrations "
            "(version text PRIMARY KEY, applied_at timestamptz NOT NULL DEFAULT now())"
        )
        for migration in sorted(migrations_dir.glob("*.sql")):
            version = migration.name.split("_", 1)[0]
            applied = connection.execute(
                "SELECT 1 FROM schema_migrations WHERE version = %s", (version,)
            ).fetchone()
            if applied:
                continue
            connection.execute(migration.read_text(encoding="utf-8"), prepare=False)
            connection.execute("INSERT INTO schema_migrations (version) VALUES (%s)", (version,))
