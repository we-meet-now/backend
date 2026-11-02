"""Read-only MCP server for the project's PostgreSQL development database.

Connection settings come from POSTGRES_* environment variables (preferred),
DATABASE_URL, or auth-service's local application-db.properties. Credentials
are never printed or returned by the MCP tools.
"""

from __future__ import annotations

import os
import re
from pathlib import Path
from typing import Any
from urllib.parse import parse_qs, unquote, urlparse

import psycopg
from mcp.server.fastmcp import FastMCP
from psycopg.conninfo import make_conninfo
from psycopg.rows import dict_row


PROJECT_ROOT = Path(__file__).resolve().parent
SPRING_DB_PROPERTIES = (
    PROJECT_ROOT
    / "auth-service"
    / "src"
    / "main"
    / "resources-env"
    / "local"
    / "application-db.properties"
)
MAX_ROWS = 500
MAX_RESULT_CHARS = 100_000

mcp = FastMCP("postgresql-dev")


def _read_properties(path: Path) -> dict[str, str]:
    properties: dict[str, str] = {}
    if not path.is_file():
        return properties

    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith(("#", "!")) or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties


def _spring_value(value: str, env_name: str) -> str:
    """Resolve Spring's ${ENV_NAME:default} syntax without exposing secrets."""
    match = re.fullmatch(r"\$\{([^}:]+)(?::(.*))?}", value)
    if not match:
        return value
    name, default = match.groups()
    return os.getenv(name, default or "")


def _database_settings() -> dict[str, str]:
    properties = _read_properties(SPRING_DB_PROPERTIES)
    jdbc_url = _spring_value(properties.get("spring.datasource.url", ""), "DB_URL")
    jdbc_user = _spring_value(properties.get("spring.datasource.username", ""), "DB_USER")
    jdbc_password = _spring_value(properties.get("spring.datasource.password", ""), "DB_PASSWORD")

    database_url = os.getenv("DATABASE_URL", "")
    if database_url:
        parsed = urlparse(database_url)
        if parsed.scheme not in {"postgres", "postgresql"}:
            raise ValueError("DATABASE_URL must use the postgres or postgresql scheme")
        query = parse_qs(parsed.query)
        return {
            "host": unquote(parsed.hostname or ""),
            "port": str(parsed.port or 5432),
            "dbname": unquote(parsed.path.lstrip("/")),
            "user": unquote(parsed.username or ""),
            "password": unquote(parsed.password or ""),
            "sslmode": os.getenv("POSTGRES_SSLMODE", query.get("sslmode", ["require"])[0]),
        }

    # Spring datasource URL format: jdbc:postgresql://host:port/database?...
    jdbc_url = os.getenv("DB_URL", jdbc_url)
    parsed = urlparse(jdbc_url.removeprefix("jdbc:"))
    settings = {
        "host": os.getenv("POSTGRES_HOST", parsed.hostname or ""),
        "port": os.getenv("POSTGRES_PORT", str(parsed.port or 5432)),
        "dbname": os.getenv("POSTGRES_DB", parsed.path.lstrip("/")),
        "user": os.getenv("POSTGRES_USER", jdbc_user),
        "password": os.getenv("POSTGRES_PASSWORD", jdbc_password),
        "sslmode": os.getenv("POSTGRES_SSLMODE", "require"),
    }
    missing = [key for key in ("host", "dbname", "user", "password") if not settings[key]]
    if missing:
        raise ValueError(
            "Database settings are incomplete. Set POSTGRES_HOST, POSTGRES_DB, "
            "POSTGRES_USER, and POSTGRES_PASSWORD, or configure auth-service's local DB properties."
        )
    return settings


def _connect() -> psycopg.Connection[Any]:
    settings = _database_settings()
    conninfo = make_conninfo(**settings, connect_timeout=10)
    # Enforce read-only mode on the PostgreSQL session itself, including CTEs.
    return psycopg.connect(
        conninfo,
        row_factory=dict_row,
        options="-c default_transaction_read_only=on",
    )


def _serialize_rows(rows: list[dict[str, Any]]) -> str:
    rendered = repr(rows)
    if len(rendered) > MAX_RESULT_CHARS:
        return rendered[:MAX_RESULT_CHARS] + "… [result truncated]"
    return rendered


@mcp.tool()
def list_schemas() -> str:
    """List non-system schemas in the connected PostgreSQL database."""
    with _connect() as conn:
        rows = conn.execute(
            """SELECT schema_name
               FROM information_schema.schemata
               WHERE schema_name NOT IN ('information_schema', 'pg_catalog')
                 AND schema_name NOT LIKE 'pg_toast%'
               ORDER BY schema_name"""
        ).fetchall()
    return _serialize_rows(rows)


@mcp.tool()
def list_tables(schema: str = "public") -> str:
    """List tables and views in a schema (defaults to public)."""
    with _connect() as conn:
        rows = conn.execute(
            """SELECT table_name, table_type
               FROM information_schema.tables
               WHERE table_schema = %s
               ORDER BY table_name""",
            (schema,),
        ).fetchall()
    return _serialize_rows(rows)


@mcp.tool()
def describe_table(table_name: str, schema: str = "public") -> str:
    """Return column names, PostgreSQL types, nullability, and defaults."""
    with _connect() as conn:
        rows = conn.execute(
            """SELECT column_name, data_type, character_maximum_length,
                      is_nullable, column_default
               FROM information_schema.columns
               WHERE table_schema = %s AND table_name = %s
               ORDER BY ordinal_position""",
            (schema, table_name),
        ).fetchall()
    if not rows:
        return f"No table or view named {schema}.{table_name} was found."
    return _serialize_rows(rows)


@mcp.tool()
def query(sql: str, limit: int = 100) -> str:
    """Run one read-only SQL query. Results are capped at 500 rows."""
    if not 1 <= limit <= MAX_ROWS:
        return f"limit must be between 1 and {MAX_ROWS}."
    statement = sql.strip()
    if not statement:
        return "SQL query is empty."
    if ";" in statement.rstrip(";"):
        return "Only one SQL statement is allowed."
    if not re.match(r"(?is)^(select|with|show|values|explain)\b", statement):
        return "Only SELECT, WITH, SHOW, VALUES, or EXPLAIN queries are allowed."

    try:
        with _connect() as conn:
            # The final LIMIT applies to row-returning statements. Read-only
            # transaction mode remains the authoritative write protection.
            cur = conn.execute(statement)
            if cur.description is None:
                return "Query completed without a result set."
            rows = cur.fetchmany(limit + 1)
        truncated = len(rows) > limit
        rows = rows[:limit]
        result = _serialize_rows(rows)
        if truncated:
            result += f"\n[showing first {limit} rows]"
        return result
    except Exception as exc:  # Keep the tool response concise; no DSN is included.
        return f"Query failed: {type(exc).__name__}: {exc}"


if __name__ == "__main__":
    mcp.run(transport="stdio")
