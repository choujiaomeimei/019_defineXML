#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Extract Variables/VLM codelists from cached XPT data and write atomically."""

from __future__ import annotations

import json
import hashlib
import math
import os
import re
import sys
import traceback
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

import pandas as pd
import pymysql
import pyreadstat


SOURCE_PREFIX = "codelist_extractor"
VAR_SOURCE = "extract_var_codelist"
VLM_SOURCE = "extract_vlm_codelist"
LEGACY_VAR_SOURCES: Tuple[str, ...] = (SOURCE_PREFIX + ":variables",)
LEGACY_VLM_SOURCES: Tuple[str, ...] = (SOURCE_PREFIX + ":vlm", "vlm_extractor")
VALID_SCOPES = {"VARIABLES", "VLM", "ALL"}
SYSTEM_REFERENCE_SOURCES = {
    VAR_SOURCE, VLM_SOURCE, SOURCE_PREFIX,
    SOURCE_PREFIX + ":variables", SOURCE_PREFIX + ":vlm", "vlm_extractor",
}
ENCODINGS: Tuple[Optional[str], ...] = (None, "latin1", "cp1252", "gb18030")
_PUNCT_TRANSLATION = str.maketrans({
    "，": ",", "。": ".", "；": ";", "：": ":", "（": "(", "）": ")",
    "【": "[", "】": "]", "！": "!", "？": "?", "“": '"', "”": '"',
    "‘": "'", "’": "'", "、": ",",
})


def _get_db_config() -> Dict[str, str]:
    return {
        "host": os.environ.get("DB_HOST", "localhost"),
        "port": int(os.environ.get("DB_PORT", "3306")),
        "user": os.environ.get("DB_USER", "root"),
        "password": os.environ.get("DB_PASSWORD", "123123"),
        "database": os.environ.get("DB_NAME", "define_db"),
    }


def normalize_text(value) -> str:
    """Return a stable display/dedup value for SAS/EDC scalar data."""
    if value is None:
        return ""
    try:
        if pd.isna(value):
            return ""
    except (TypeError, ValueError):
        pass
    if isinstance(value, bytes):
        for encoding in ("utf-8", "gb18030", "cp1252", "latin1"):
            try:
                value = value.decode(encoding)
                break
            except UnicodeDecodeError:
                continue
    if isinstance(value, float) and math.isfinite(value) and value.is_integer():
        return str(int(value))
    text = str(value)
    if text.lower() == "nan":
        return ""
    if text.startswith("b'") and text.endswith("'"):
        text = text[2:-1]
    text = unicodedata.normalize("NFKC", text).translate(_PUNCT_TRANSLATION)
    text = re.sub(r"\s+", " ", text).strip()
    if re.fullmatch(r"[+-]?\d+\.0+", text):
        return text.split(".", 1)[0]
    return text


def dedupe_values(values: Iterable) -> List[str]:
    """Normalize and deduplicate while retaining the first display value."""
    result: List[str] = []
    seen = set()
    for value in values:
        text = normalize_text(value)
        if not text:
            continue
        key = text.casefold()
        if key not in seen:
            seen.add(key)
            result.append(text)
    return result


def normalize_frame(df: pd.DataFrame) -> pd.DataFrame:
    result = df.copy()
    result.columns = [normalize_text(c).upper() for c in result.columns]
    for col in result.columns:
        if result[col].dtype == object:
            result[col] = result[col].map(normalize_text)
    return result


def read_xpt(xpt_path: Path) -> Tuple[pd.DataFrame, str]:
    """Read an XPORT file with deterministic legacy-encoding fallbacks."""
    errors = []
    for encoding in ENCODINGS:
        try:
            kwargs = {"encoding": encoding} if encoding else {}
            df, _ = pyreadstat.read_xport(str(xpt_path), **kwargs)
            return normalize_frame(df), encoding or "auto"
        except Exception as exc:  # pyreadstat uses several exception types by version
            errors.append(f"{encoding or 'auto'}: {exc}")
    raise RuntimeError("all XPORT encodings failed: " + " | ".join(errors))


def find_xpt_file(data_path: Path, dataset: str) -> Optional[Path]:
    target = normalize_text(dataset).lower()
    for path in data_path.glob("*.xpt"):
        stem = path.stem.lower()
        if stem == target or stem.endswith("_" + target):
            return path
    return None


def parse_where_clause(where_clause: str) -> Optional[Tuple[str, str]]:
    match = re.fullmatch(
        r"\s*([A-Za-z_]\w*)\s+(?:EQ|=)\s*(?:\"([^\"]*)\"|'([^']*)'|([^\s]+))\s*",
        where_clause or "",
        flags=re.IGNORECASE,
    )
    if not match:
        return None
    return match.group(1).upper(), normalize_text(next(v for v in match.groups()[1:] if v is not None))


def _is_numeric(value: str) -> bool:
    try:
        float(value)
        return True
    except (TypeError, ValueError):
        return False


def infer_data_type(values: Sequence[str]) -> str:
    nonempty = [normalize_text(v) for v in values if normalize_text(v)]
    if not nonempty or not all(_is_numeric(v) for v in nonempty):
        return "Char"
    return "Float" if any("." in value for value in nonempty) else "Integer"


def _term_weight(term: str) -> int:
    text = normalize_text(term)
    upper = text.upper()
    if text in ("未知", "其他") or upper.startswith(("UNKNOWN", "OTHER")):
        return 90
    if text in ("未查", "未做") or upper.startswith(("NOT DONE", "NOT EXAMINED")):
        return 80
    if text == "正常" or upper in ("NORMAL", "WNL", "WITHIN NORMAL LIMITS"):
        return 10
    if text == "阴性" or upper in ("NEGATIVE", "NEG"):
        return 11
    if text == "阳性" or upper in ("POSITIVE", "POS"):
        return 12
    if "无临床意义" in text or "NCS" in upper or (
        "NOT CLINICALLY" in upper and "SIGNIFICANT" in upper
    ):
        return 20
    if text == "异常" or upper == "ABNORMAL":
        return 25
    if "有临床意义" in text or (
        "CLINICALLY SIGNIFICANT" in upper and "NOT" not in upper
    ):
        return 30
    return 50


def sort_terms(terms: List[dict]) -> List[dict]:
    return sorted(
        terms,
        key=lambda term: (_term_weight(term.get("code", "")), normalize_text(term.get("code")).casefold()),
    )


def extract_unique_terms(frame: pd.DataFrame, column: str) -> List[dict]:
    if column.upper() not in frame.columns:
        return []
    values = dedupe_values(frame[column.upper()].tolist())
    return [{"code": value, "codeDes": ""} for value in sorted(values, key=str.casefold)]


def extract_paired_terms(frame: pd.DataFrame, code_column: str, text_column: str) -> Tuple[List[dict], List[dict]]:
    code_column, text_column = code_column.upper(), text_column.upper()
    if code_column not in frame.columns:
        return [], []
    code_terms: List[dict] = []
    text_terms: List[dict] = []
    seen_codes, seen_texts = set(), set()
    has_text = text_column in frame.columns
    columns = [code_column, text_column] if has_text else [code_column]
    for values in frame[columns].itertuples(index=False, name=None):
        code = normalize_text(values[0])
        description = normalize_text(values[1]) if has_text else ""
        code_key = code.casefold()
        if code and code_key not in seen_codes:
            seen_codes.add(code_key)
            code_terms.append({"code": code, "codeDes": description})
        text_key = description.casefold()
        if description and text_key not in seen_texts:
            seen_texts.add(text_key)
            text_terms.append({"code": description, "codeDes": ""})
    return sort_terms(code_terms), sort_terms(text_terms)


class EDCStore:
    """Load one optional EDC workbook/CSV and expose domain-specific frames."""

    def __init__(self, path: str):
        self.path = Path(path) if path else None
        self.frames: Dict[str, pd.DataFrame] = {}
        self.error = ""
        if self.path:
            self._load()

    def _load(self) -> None:
        if not self.path or not self.path.exists():
            self.error = f"EDC file not found: {self.path}"
            return
        try:
            if self.path.suffix.lower() in (".xlsx", ".xls", ".xlsm"):
                raw = pd.read_excel(self.path, sheet_name=None)
                self.frames = {normalize_text(name).upper(): normalize_frame(df) for name, df in raw.items()}
            elif self.path.suffix.lower() in (".csv", ".txt"):
                self.frames = {"__DEFAULT__": normalize_frame(pd.read_csv(self.path))}
            else:
                self.error = f"unsupported EDC file type: {self.path.suffix}"
        except Exception as exc:
            self.error = f"failed to read EDC file: {exc}"

    def get(self, dataset: str) -> Optional[pd.DataFrame]:
        dataset = dataset.upper()
        if dataset in self.frames:
            return self.frames[dataset]
        for frame in self.frames.values():
            for domain_col in ("DOMAIN", "DATASET"):
                if domain_col in frame.columns:
                    mask = frame[domain_col].map(normalize_text).str.upper() == dataset
                    if mask.any():
                        return frame.loc[mask].reset_index(drop=True)
        if len(self.frames) == 1:
            return next(iter(self.frames.values()))
        return None


class DatasetCache:
    """Resolve each dataset once from XPT, with EDC used for missing columns."""

    def __init__(self, data_path: Path, edc_path: str = ""):
        self.data_path = data_path
        self.edc = EDCStore(edc_path)
        self.cache: Dict[str, Optional[pd.DataFrame]] = {}
        self.encodings: Dict[str, str] = {}
        self.errors: List[str] = []
        self.fallbacks: List[dict] = []

    def _xpt(self, dataset: str) -> Optional[pd.DataFrame]:
        dataset = dataset.upper()
        if dataset in self.cache:
            return self.cache[dataset]
        path = find_xpt_file(self.data_path, dataset)
        if not path:
            self.cache[dataset] = None
            return None
        try:
            frame, encoding = read_xpt(path)
            self.cache[dataset] = frame
            self.encodings[dataset] = encoding
        except Exception as exc:
            self.cache[dataset] = None
            self.errors.append(f"{dataset}: {exc}")
        return self.cache[dataset]

    def get(self, dataset: str, required: Sequence[str] = ()) -> Optional[pd.DataFrame]:
        dataset = dataset.upper()
        required_set = {column.upper() for column in required if column}
        xpt = self._xpt(dataset)
        if xpt is not None and required_set.issubset(xpt.columns):
            return xpt
        edc = self.edc.get(dataset)
        if edc is not None and required_set.issubset(edc.columns):
            reason = "missing XPT" if xpt is None else "missing columns: " + ",".join(sorted(required_set - set(xpt.columns)))
            event = {"dataset": dataset, "reason": reason, "source": str(self.edc.path)}
            if event not in self.fallbacks:
                self.fallbacks.append(event)
            return edc
        if required_set and (xpt is None or not required_set.issubset(xpt.columns)):
            reason = (
                "missing XPT"
                if xpt is None
                else "missing columns: " + ",".join(sorted(required_set - set(xpt.columns)))
            )
            message = f"{dataset}: {reason}"
            if message not in self.errors:
                self.errors.append(message)
        return xpt


@dataclass
class Draft:
    codelists: Dict[Tuple[str, str], dict] = field(default_factory=dict)
    spec_references: Dict[int, Optional[str]] = field(default_factory=dict)
    vlm_references: Dict[int, Optional[str]] = field(default_factory=dict)
    dictionaries: set = field(default_factory=set)
    metadata_counts: Dict[str, int] = field(default_factory=lambda: {"VARIABLES": 0, "VLM": 0})

    def add(
        self,
        source: str,
        vcd: str,
        label: str,
        terms: List[dict],
        data_type: str = "Char",
        submission_value: str = "",
        ct_names: Optional[Dict[str, str]] = None,
        ct_headers: Optional[Dict[str, str]] = None,
        ct_terms: Optional[Dict[Tuple[str, str], str]] = None,
        terminology: str = "",
    ) -> None:
        vcd = normalize_text(vcd)
        if not vcd or not terms:
            return
        lookup_key = normalize_text(submission_value or vcd).upper()
        effective_label = (ct_names or {}).get(lookup_key, "") or normalize_text(label)
        nci_codelist_code = (ct_headers or {}).get(lookup_key, "")
        key = (source, vcd)
        entry = self.codelists.setdefault(key, {
            "source": source, "vcd": vcd, "vlabel": effective_label,
            "type": data_type, "terms": [], "_seen": set(),
            "nciCodelistCode": nci_codelist_code,
            "terminology": terminology if nci_codelist_code else "",
        })
        if effective_label and not entry["vlabel"]:
            entry["vlabel"] = effective_label
        for term in terms:
            code = normalize_text(term.get("code"))
            code_des = normalize_text(term.get("codeDes"))
            dedupe_key = code.casefold()
            if code and dedupe_key not in entry["_seen"]:
                entry["_seen"].add(dedupe_key)
                nci_term_code = ""
                if nci_codelist_code:
                    nci_term_code = (ct_terms or {}).get(
                        (nci_codelist_code.upper(), code.upper()), ""
                    )
                entry["terms"].append({
                    "code": code,
                    "codeDes": code_des,
                    "nciTermCode": nci_term_code,
                })


def _query_rows(conn, sql: str, params: Sequence) -> List[dict]:
    with conn.cursor(pymysql.cursors.DictCursor) as cursor:
        cursor.execute(sql, params)
        return list(cursor.fetchall())


def _load_ct_lookup(conn, project_id: str) -> Tuple[
    Dict[str, str], Dict[str, str], Dict[Tuple[str, str], str], Optional[int], str, str
]:
    rows = _query_rows(
        conn,
        "SELECT ct_version,standard_type FROM project_config WHERE project_id=%s LIMIT 1",
        (project_id,),
    )
    configured = normalize_text((rows[0] if rows else {}).get("ct_version"))
    standard_type = normalize_text((rows[0] if rows else {}).get("standard_type")) or "SDTM"
    standard_type = standard_type.split(",", 1)[0].upper()
    package_id = None
    release_date = ""
    if configured:
        match = re.search(r"\d{4}-\d{2}-\d{2}", configured)
        if not match:
            raise ValueError(f"configured CT version has no release date: {configured}")
        release_date = match.group(0)
        packages = _query_rows(
            conn,
            "SELECT id FROM ct_package WHERE release_date=%s "
            "AND UPPER(standard_type)=%s AND language_code='EN' LIMIT 1",
            (release_date, standard_type),
        )
        if not packages:
            raise ValueError(f"configured CT version not found: {configured}")
        package_id = packages[0]["id"]
    else:
        packages = _query_rows(
            conn,
            "SELECT id, release_date FROM ct_package WHERE UPPER(standard_type)=%s "
            "AND language_code='EN' ORDER BY release_date DESC LIMIT 1",
            (standard_type,),
        )
        if packages:
            package_id = packages[0]["id"]
            release_date = str(packages[0].get("release_date") or "")
    if not package_id:
        return {}, {}, {}, None, release_date, ""
    terms = _query_rows(
        conn,
        "SELECT codelist_code,codelist_name,term_code,cdisc_submission_value "
        "FROM ct_term WHERE package_id=%s",
        (package_id,),
    )
    names: Dict[str, str] = {}
    headers: Dict[str, str] = {}
    term_codes: Dict[Tuple[str, str], str] = {}
    for row in terms:
        submission = normalize_text(row.get("cdisc_submission_value")).upper()
        codelist_code = normalize_text(row.get("codelist_code")).upper()
        term_code = normalize_text(row.get("term_code"))
        if not codelist_code:
            if submission:
                names[submission] = normalize_text(row.get("codelist_name"))
                headers[submission] = term_code
        elif submission:
            term_codes[(codelist_code, submission)] = term_code
    terminology = f"{standard_type} Terminology {release_date}" if release_date else ""
    return names, headers, term_codes, package_id, release_date, terminology


def _column(frame: Optional[pd.DataFrame], variable: str, submission_value: str) -> str:
    if frame is None:
        return ""
    for candidate in (variable, submission_value):
        candidate = normalize_text(candidate).upper()
        if candidate and candidate in frame.columns:
            return candidate
    return ""


def _pair_kind(submission_value: str) -> Optional[Tuple[str, str]]:
    upper = submission_value.upper()
    for suffix, role in (("TESTCD", "code"), ("TEST", "text"), ("PARMCD", "code"), ("PARM", "text")):
        if upper.endswith(suffix):
            family = suffix.replace("CD", "")
            return upper[:-len(suffix)] + family, role
    return None


def _build_variables(
    draft: Draft,
    rows: List[dict],
    cache: DatasetCache,
    ct_names: Dict[str, str],
    ct_headers: Dict[str, str],
    ct_terms: Dict[Tuple[str, str], str],
    terminology: str,
) -> None:
    draft.metadata_counts["VARIABLES"] = len(rows)
    pairs: Dict[Tuple[str, str], dict] = {}
    general = []
    domain_values: List[str] = []
    domain_label = ""
    for row in rows:
        row_id = int(row["id"])
        domain = normalize_text(row.get("domain")).upper()
        variable = normalize_text(row.get("variable")).upper()
        submission = normalize_text(row.get("cdisc_submission_value"))
        label = normalize_text(row.get("label"))
        if not submission or not domain or not variable:
            continue
        existing_reference = normalize_text(row.get("codelist"))
        reference_source = normalize_text(row.get("updated_by"))
        if not existing_reference or reference_source in SYSTEM_REFERENCE_SOURCES:
            draft.spec_references[row_id] = None
        if submission.upper() in ("MEDDRA", "WHODRUG"):
            draft.dictionaries.add(submission.upper())
            continue
        if submission.upper() == "DOMAIN":
            frame = cache.get(domain, (variable,))
            column = _column(frame, variable, submission)
            if not column:
                frame = cache.get(domain, (submission.upper(),))
                column = _column(frame, variable, submission)
            if column:
                domain_values.extend(frame[column].tolist())
                domain_label = domain_label or label
                if row_id in draft.spec_references:
                    draft.spec_references[row_id] = "DOMAIN"
            continue
        pair_kind = _pair_kind(submission)
        if pair_kind:
            family, role = pair_kind
            info = pairs.setdefault((domain, family), {})
            info[role] = {
                "id": row_id, "variable": variable, "submission": submission, "label": label
            }
            continue
        general.append(row)

    domain_terms = [{"code": value, "codeDes": ""} for value in sorted(dedupe_values(domain_values), key=str.casefold)]
    draft.add(
        VAR_SOURCE, "DOMAIN", domain_label, sort_terms(domain_terms),
        submission_value="DOMAIN", ct_names=ct_names, ct_headers=ct_headers,
        ct_terms=ct_terms, terminology=terminology,
    )

    for (domain, _family), info in pairs.items():
        code_info = info.get("code")
        text_info = info.get("text")
        if not code_info:
            if text_info:
                general.append(next(row for row in rows if int(row["id"]) == text_info["id"]))
            continue
        required = [code_info["variable"]]
        if text_info:
            required.append(text_info["variable"])
        frame = cache.get(domain, required)
        if frame is None or not all(
            _column(frame, item["variable"], item["submission"])
            for item in (code_info, text_info) if item
        ):
            submission_columns = [code_info["submission"]]
            if text_info:
                submission_columns.append(text_info["submission"])
            frame = cache.get(domain, submission_columns)
        code_col = _column(frame, code_info["variable"], code_info["submission"])
        text_col = _column(frame, text_info["variable"], text_info["submission"]) if text_info else ""
        if not code_col:
            continue
        code_terms, text_terms = extract_paired_terms(frame, code_col, text_col)
        code_vcd = f"{domain}.{code_info['variable']}"
        draft.add(
            VAR_SOURCE, code_vcd, code_info["label"], code_terms,
            submission_value=code_info["submission"], ct_names=ct_names,
            ct_headers=ct_headers, ct_terms=ct_terms, terminology=terminology,
        )
        if code_terms and code_info["id"] in draft.spec_references:
            draft.spec_references[code_info["id"]] = code_vcd
        if text_info and text_terms:
            text_vcd = f"{domain}.{text_info['variable']}"
            draft.add(
                VAR_SOURCE, text_vcd, text_info["label"], text_terms,
                submission_value=text_info["submission"], ct_names=ct_names,
                ct_headers=ct_headers, ct_terms=ct_terms, terminology=terminology,
            )
            if text_info["id"] in draft.spec_references:
                draft.spec_references[text_info["id"]] = text_vcd

    for row in general:
        row_id = int(row["id"])
        domain = normalize_text(row.get("domain")).upper()
        variable = normalize_text(row.get("variable")).upper()
        submission = normalize_text(row.get("cdisc_submission_value"))
        frame = cache.get(domain, (variable,))
        column = _column(frame, variable, submission)
        if not column and submission:
            frame = cache.get(domain, (submission.upper(),))
            column = _column(frame, variable, submission)
        if not column:
            continue
        paired = column[:-2] if column.endswith("CD") and column[:-2] in frame.columns else ""
        if paired:
            terms, _ = extract_paired_terms(frame, column, paired)
        else:
            terms = extract_unique_terms(frame, column)
        vcd = f"{domain}.{variable}"
        draft.add(
            VAR_SOURCE, vcd, row.get("label", ""), sort_terms(terms),
            infer_data_type([term["code"] for term in terms]), submission,
            ct_names, ct_headers, ct_terms, terminology,
        )
        if terms and row_id in draft.spec_references:
            draft.spec_references[row_id] = vcd


def _build_vlm(
    draft: Draft,
    rows: List[dict],
    cache: DatasetCache,
    ct_names: Dict[str, str],
    ct_headers: Dict[str, str],
    ct_terms: Dict[Tuple[str, str], str],
    terminology: str,
) -> None:
    draft.metadata_counts["VLM"] = len(rows)
    for row in rows:
        row_id = int(row["id"])
        existing_reference = normalize_text(row.get("codelist"))
        reference_source = normalize_text(row.get("updated_by"))
        if not existing_reference or reference_source in SYSTEM_REFERENCE_SOURCES:
            draft.vlm_references[row_id] = None
        dataset = normalize_text(row.get("dataset")).upper()
        variable = normalize_text(row.get("variable")).upper()
        parsed = parse_where_clause(row.get("where_clause") or "")
        if not dataset or not variable or not parsed:
            continue
        filter_col, filter_value = parsed
        check_candidates = []
        if variable.endswith("ORRES"):
            check_candidates.append(variable[:-5] + "STRESC")
        check_candidates.append(variable)
        frame = None
        check_col = ""
        for candidate in check_candidates:
            candidate_frame = cache.get(dataset, (filter_col, candidate))
            if candidate_frame is not None and filter_col in candidate_frame.columns and candidate in candidate_frame.columns:
                frame, check_col = candidate_frame, candidate
                break
        if frame is None:
            continue
        mask = frame[filter_col].map(normalize_text).str.casefold() == filter_value.casefold()
        values = dedupe_values(frame.loc[mask, check_col].tolist())
        if not values or all(_is_numeric(value) for value in values):
            continue
        codelist_id = f"{dataset}.{variable}.{filter_value}"
        terms = sort_terms([{"code": value, "codeDes": ""} for value in values])
        draft.add(
            VLM_SOURCE, codelist_id, row.get("label", ""), terms,
            data_type=infer_data_type(values),
            submission_value=row.get("cdisc_submission_value") or variable,
            ct_names=ct_names, ct_headers=ct_headers, ct_terms=ct_terms,
            terminology=terminology,
        )
        if row_id in draft.vlm_references:
            draft.vlm_references[row_id] = codelist_id


def _validate_draft(draft: Draft, scope: str) -> None:
    requested = [VAR_SOURCE, VLM_SOURCE] if scope == "ALL" else [
        VAR_SOURCE if scope == "VARIABLES" else VLM_SOURCE
    ]
    if any(draft.metadata_counts["VARIABLES" if source == VAR_SOURCE else "VLM"] == 0 for source in requested):
        missing = ["VARIABLES" if source == VAR_SOURCE else "VLM" for source in requested
                   if draft.metadata_counts["VARIABLES" if source == VAR_SOURCE else "VLM"] == 0]
        raise ValueError("no source metadata rows for: " + ", ".join(missing))
    generated = [entry for (source, _), entry in draft.codelists.items() if source in requested and entry["terms"]]
    if not generated:
        raise ValueError("draft contains no codelists; existing database rows were preserved")
    for entry in generated:
        if not entry["vcd"] or any(not term["code"] for term in entry["terms"]):
            raise ValueError(f"invalid draft codelist: {entry['vcd']!r}")


def _scope_sources(scope: str) -> List[str]:
    if scope == "VARIABLES":
        return [VAR_SOURCE]
    if scope == "VLM":
        return [VLM_SOURCE]
    return [VAR_SOURCE, VLM_SOURCE]


def _deleted_vcds(conn, project_id: str, username: str) -> set:
    try:
        rows = _query_rows(
            conn,
            "SELECT vcd FROM sas_codelist_deleted WHERE project_id=%s AND username=%s",
            (project_id, username),
        )
        return {normalize_text(row.get("vcd")).casefold() for row in rows}
    except pymysql.MySQLError as exc:
        if getattr(exc, "args", [None])[0] == 1146:
            return set()
        raise


def _merge_rules(conn, project_id: str, username: str) -> Dict[str, str]:
    try:
        rows = _query_rows(
            conn,
            "SELECT original_vcd,merged_vcd FROM sas_codelist_merge_log "
            "WHERE project_id=%s AND username=%s AND original_vcd IS NOT NULL "
            "AND merged_vcd IS NOT NULL ORDER BY id",
            (project_id, username),
        )
        return {
            normalize_text(row.get("original_vcd")).casefold(): normalize_text(row.get("merged_vcd"))
            for row in rows
            if normalize_text(row.get("original_vcd")) and normalize_text(row.get("merged_vcd"))
        }
    except pymysql.MySQLError as exc:
        if getattr(exc, "args", [None])[0] == 1146:
            return {}
        raise


def _write_draft(conn, draft: Draft, project_id: str, username: str, scope: str) -> dict:
    sources = _scope_sources(scope)
    deleted_vcds = _deleted_vcds(conn, project_id, username)
    merge_rules = _merge_rules(conn, project_id, username)
    entries = [
        entry for (source, _), entry in draft.codelists.items()
        if source in sources and entry["vcd"].casefold() not in deleted_vcds
    ]
    inserted = 0
    updated = 0
    deleted = 0
    spec_updated = 0
    vlm_updated = 0
    reapplied_merges = 0
    with conn.cursor() as cursor:
        replacement_sources = list(sources)
        if VAR_SOURCE in sources:
            replacement_sources.extend(LEGACY_VAR_SOURCES)
        if VLM_SOURCE in sources:
            replacement_sources.extend(LEGACY_VLM_SOURCES)
        if scope == "ALL":
            replacement_sources.append(SOURCE_PREFIX)
        placeholders = ",".join(["%s"] * len(replacement_sources))
        cursor.execute(
            f"SELECT id,vcd,code FROM sas_codelist_data "
            f"WHERE project_id=%s AND username=%s AND created_by IN ({placeholders}) "
            "ORDER BY id",
            (project_id, username, *replacement_sources),
        )
        existing_rows = list(cursor.fetchall())
        if scope != "ALL":
            dot_comparison = "<= 1" if scope == "VARIABLES" else ">= 2"
            cursor.execute(
                "SELECT id,vcd,code FROM sas_codelist_data "
                "WHERE project_id=%s AND username=%s AND created_by=%s "
                f"AND (LENGTH(vcd)-LENGTH(REPLACE(vcd,'.',''))) {dot_comparison} ORDER BY id",
                (project_id, username, SOURCE_PREFIX),
            )
            existing_rows.extend(cursor.fetchall())
        cursor.execute(
            f"SELECT vcd,code FROM sas_codelist_data "
            f"WHERE project_id=%s AND username=%s "
            f"AND (created_by IS NULL OR created_by NOT IN ({placeholders}))",
            (project_id, username, *replacement_sources),
        )
        manual_keys = {
            (normalize_text(row[0]).casefold(), normalize_text(row[1]).casefold())
            for row in cursor.fetchall()
        }
        existing_by_key: Dict[Tuple[str, str], List[dict]] = {}
        for row in existing_rows:
            key = (normalize_text(row[1]).casefold(), normalize_text(row[2]).casefold())
            existing_by_key.setdefault(key, []).append({"id": row[0]})

        insert_sql = (
            "INSERT INTO sas_codelist_data "
            "(project_id,username,vcd,vlabel,nci_codelist_code,type,terminology,cdnum,"
            "code,nci_term_code,code_des,origin,sort_order,created_by,updated_by) "
            "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)"
        )
        update_sql = (
            "UPDATE sas_codelist_data SET vlabel=%s,nci_codelist_code=%s,type=%s,"
            "terminology=%s,cdnum=%s,code=%s,nci_term_code=%s,code_des=%s,"
            "origin=%s,sort_order=%s,created_by=%s,updated_by=%s,updated_time=NOW() WHERE id=%s"
        )
        global_order = 0
        retained_ids = set()
        for entry in sorted(entries, key=lambda item: (item["source"], item["vcd"].casefold())):
            origin = (
                ".".join(entry["vcd"].split(".")[:2]) + ".VLM"
                if entry["source"] == VLM_SOURCE else entry["vcd"]
            )
            for order, term in enumerate(sort_terms(entry["terms"]), 1):
                global_order += 1
                key = (entry["vcd"].casefold(), term["code"].casefold())
                if key in manual_keys:
                    continue
                candidates = existing_by_key.get(key, [])
                if candidates:
                    row_id = candidates.pop(0)["id"]
                    retained_ids.add(row_id)
                    cursor.execute(update_sql, (
                        entry["vlabel"], entry["nciCodelistCode"], entry["type"],
                        entry["terminology"], order, term["code"], term["nciTermCode"],
                        term["codeDes"], origin, global_order, entry["source"],
                        entry["source"], row_id,
                    ))
                    updated += 1
                else:
                    cursor.execute(insert_sql, (
                        project_id, username, entry["vcd"], entry["vlabel"],
                        entry["nciCodelistCode"], entry["type"], entry["terminology"],
                        order, term["code"], term["nciTermCode"], term["codeDes"],
                        origin, global_order, entry["source"], entry["source"],
                    ))
                    inserted += 1

        stale_ids = [row["id"] for rows in existing_by_key.values() for row in rows
                     if row["id"] not in retained_ids]
        if stale_ids:
            for start in range(0, len(stale_ids), 500):
                batch = stale_ids[start:start + 500]
                cursor.execute(
                    "DELETE FROM sas_codelist_data WHERE id IN (" + ",".join(["%s"] * len(batch)) + ")",
                    batch,
                )
                deleted += cursor.rowcount

        if VAR_SOURCE in sources:
            spec_updates = []
            for row_id, codelist in draft.spec_references.items():
                if codelist and codelist.casefold() in deleted_vcds:
                    codelist = None
                elif codelist and codelist.casefold() in merge_rules:
                    merged = merge_rules[codelist.casefold()]
                    codelist = None if merged.casefold() in deleted_vcds else merged
                    reapplied_merges += 1
                spec_updates.append((codelist, VAR_SOURCE, row_id))
            if spec_updates:
                cursor.executemany(
                    "UPDATE sas_project_spec SET codelist=%s,updated_by=%s,updated_time=NOW() WHERE id=%s",
                    spec_updates,
                )
                spec_updated = cursor.rowcount
            dictionary_sources = (VAR_SOURCE, *LEGACY_VAR_SOURCES, SOURCE_PREFIX)
            cursor.execute(
                "SELECT id,dictionary_id,name,dictionary,version,created_by "
                "FROM sas_dictionaries_data WHERE project_id=%s AND username=%s ORDER BY id",
                (project_id, username),
            )
            dictionary_rows = list(cursor.fetchall())
            metadata = {
                "MEDDRA": ("Adverse Event Dictionary", "MEDDRA"),
                "WHODRUG": ("Drug Dictionary", "WHODRUG"),
            }
            retained_dictionary_ids = set()
            for order, dictionary_id in enumerate(sorted(draft.dictionaries), 1):
                matches = [
                    row for row in dictionary_rows
                    if normalize_text(row[1]).upper() == dictionary_id
                ]
                manual = next(
                    (row for row in matches if normalize_text(row[5]) not in dictionary_sources),
                    None,
                )
                if manual:
                    retained_dictionary_ids.add(manual[0])
                    continue
                generated = matches[0] if matches else None
                name, dictionary = metadata[dictionary_id]
                if generated:
                    retained_dictionary_ids.add(generated[0])
                    cursor.execute(
                        "UPDATE sas_dictionaries_data SET name=%s,data_type='text',dictionary=%s,"
                        "sort_order=%s,created_by=%s,updated_time=NOW() WHERE id=%s",
                        (
                            normalize_text(generated[2]) or name,
                            normalize_text(generated[3]) or dictionary,
                            order, VAR_SOURCE, generated[0],
                        ),
                    )
                else:
                    cursor.execute(
                        "INSERT INTO sas_dictionaries_data "
                        "(project_id,username,dictionary_id,name,data_type,dictionary,version,sort_order,created_by) "
                        "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)",
                        (
                            project_id, username, dictionary_id, name, "text",
                            dictionary, "", order, VAR_SOURCE,
                        ),
                    )
            stale_dictionary_ids = [
                row[0] for row in dictionary_rows
                if normalize_text(row[5]) in dictionary_sources
                and row[0] not in retained_dictionary_ids
            ]
            if stale_dictionary_ids:
                cursor.execute(
                    "DELETE FROM sas_dictionaries_data WHERE id IN ("
                    + ",".join(["%s"] * len(stale_dictionary_ids)) + ")",
                    stale_dictionary_ids,
                )

        if VLM_SOURCE in sources:
            vlm_updates = []
            for row_id, codelist in draft.vlm_references.items():
                if codelist and codelist.casefold() in deleted_vcds:
                    codelist = None
                elif codelist and codelist.casefold() in merge_rules:
                    merged = merge_rules[codelist.casefold()]
                    codelist = None if merged.casefold() in deleted_vcds else merged
                    reapplied_merges += 1
                vlm_updates.append((codelist, VLM_SOURCE, row_id))
            if vlm_updates:
                cursor.executemany(
                    "UPDATE sas_vlm_data SET codelist=%s,updated_by=%s,updated_time=NOW() WHERE id=%s",
                    vlm_updates,
                )
                vlm_updated = cursor.rowcount
    written_terms = [
        term for entry in entries for term in entry["terms"]
    ]
    return {
        "inserted": inserted,
        "updated": updated,
        "deletedSystemRows": deleted,
        "specReferencesUpdated": spec_updated,
        "vlmReferencesUpdated": vlm_updated,
        "suppressedDeletedVcds": len([
            entry for entry in draft.codelists.values()
            if entry["source"] in sources and entry["vcd"].casefold() in deleted_vcds
        ]),
        "nciMatched": sum(1 for term in written_terms if term.get("nciTermCode")),
        "nciUnmatched": sum(
            1 for entry in entries if entry.get("nciCodelistCode")
            for term in entry["terms"] if not term.get("nciTermCode")
        ),
        "reappliedMerges": reapplied_merges,
        "preservedManual": len(manual_keys),
    }


def run(project_id: str, data_path: str, username: str = "", scope: str = "") -> dict:
    scope = normalize_text(scope or os.environ.get("EXTRACT_SCOPE", "ALL")).upper()
    result = {
        "success": False,
        "projectId": project_id,
        "scope": scope,
        "draftValidated": False,
        "datasets": [],
        "fallbacks": [],
        "counts": {},
        "errors": [],
        "warnings": [],
    }
    conn = None
    lock_name = ""
    lock_acquired = False
    try:
        if scope not in VALID_SCOPES:
            raise ValueError(f"EXTRACT_SCOPE must be one of {sorted(VALID_SCOPES)}, got {scope!r}")
        data_dir = Path(data_path)
        edc_path = os.environ.get("EDC_CODELIST_PATH", "")
        if not data_dir.exists() and not edc_path:
            raise FileNotFoundError(f"XPT path not found and no EDC fallback configured: {data_dir}")
        conn = pymysql.connect(
            **_get_db_config(), charset="utf8mb4", autocommit=False,
        )
        lock_digest = hashlib.sha256(f"{project_id}\0{username}".encode("utf-8")).hexdigest()[:40]
        lock_name = "codelist:" + lock_digest
        with conn.cursor() as cursor:
            cursor.execute("SELECT GET_LOCK(%s,0)", (lock_name,))
            lock_acquired = cursor.fetchone()[0] == 1
        if not lock_acquired:
            raise RuntimeError("该项目正在执行 Codelist 提取，请稍后重试")
        (
            ct_names, ct_headers, ct_terms, package_id, ct_release, terminology
        ) = _load_ct_lookup(conn, project_id)
        cache = DatasetCache(data_dir, edc_path)
        draft = Draft()

        if scope in ("VARIABLES", "ALL"):
            sql = (
                "SELECT id,domain,variable,cdisc_submission_value,codelist,label,updated_by "
                "FROM sas_project_spec WHERE project_id=%s"
            )
            params: List = [project_id]
            if username:
                sql += " AND username=%s"
                params.append(username)
            sql += " ORDER BY domain,variable"
            _build_variables(
                draft, _query_rows(conn, sql, params), cache,
                ct_names, ct_headers, ct_terms, terminology,
            )

        if scope in ("VLM", "ALL"):
            sql = (
                "SELECT v.id,v.dataset,v.variable,v.where_clause,v.label,v.codelist,v.updated_by,"
                "(SELECT p.cdisc_submission_value FROM sas_project_spec p "
                " WHERE p.project_id=v.project_id AND p.username=v.username "
                " AND UPPER(p.domain)=UPPER(v.dataset) AND UPPER(p.variable)=UPPER(v.variable) "
                " LIMIT 1) AS cdisc_submission_value "
                "FROM sas_vlm_data v WHERE v.project_id=%s"
            )
            params = [project_id]
            if username:
                sql += " AND v.username=%s"
                params.append(username)
            sql += " ORDER BY v.dataset,v.sort_order"
            _build_vlm(
                draft, _query_rows(conn, sql, params), cache,
                ct_names, ct_headers, ct_terms, terminology,
            )

        _validate_draft(draft, scope)
        result["draftValidated"] = True
        result["datasets"] = [
            {"dataset": dataset, "source": "XPT", "encoding": cache.encodings.get(dataset, ""),
             "available": frame is not None, "rows": len(frame) if frame is not None else 0}
            for dataset, frame in sorted(cache.cache.items())
        ]
        result["fallbacks"] = cache.fallbacks
        if cache.edc.error:
            result["warnings"].append(cache.edc.error)
        result["warnings"].extend(cache.errors)
        result["counts"] = _write_draft(conn, draft, project_id, username, scope)
        result["counts"]["draftCodelists"] = len([
            entry for entry in draft.codelists.values() if entry["source"] in _scope_sources(scope)
        ])
        result["counts"]["draftTerms"] = sum(
            len(entry["terms"]) for entry in draft.codelists.values()
            if entry["source"] in _scope_sources(scope)
            and entry["vcd"].casefold() not in _deleted_vcds(conn, project_id, username)
        )
        result["ctPackageId"] = package_id
        result["ctRelease"] = ct_release
        conn.commit()
        result["success"] = True
    except Exception as exc:
        if conn:
            conn.rollback()
        result["errors"].append(str(exc))
        traceback.print_exc()
    finally:
        if conn:
            if lock_acquired:
                try:
                    with conn.cursor() as cursor:
                        cursor.execute("SELECT RELEASE_LOCK(%s)", (lock_name,))
                except Exception:
                    pass
            conn.close()
    print("[RESULT] " + json.dumps(result, ensure_ascii=False, default=str))
    counts = result.get("counts", {})
    summary = {
        "codelists": counts.get("draftCodelists", 0),
        "terms": counts.get("draftTerms", 0),
        "inserted": counts.get("inserted", 0),
        "updated": counts.get("updated", 0),
        "deleted": counts.get("deletedSystemRows", 0),
        "specReferences": counts.get("specReferencesUpdated", 0),
        "vlmReferences": counts.get("vlmReferencesUpdated", 0),
        "skippedDeleted": counts.get("suppressedDeletedVcds", 0),
        "nciMatched": counts.get("nciMatched", 0),
        "nciUnmatched": counts.get("nciUnmatched", 0),
        "reappliedMerges": counts.get("reappliedMerges", 0),
        "preservedManual": counts.get("preservedManual", 0),
        "failedDatasets": "|".join(sorted({
            normalize_text(error).split(":", 1)[0] for error in result.get("warnings", [])
            if ":" in normalize_text(error)
        })),
        "warningCount": len(result.get("warnings", [])),
        "fallbackCount": len(result.get("fallbacks", [])),
    }
    print("__CODELIST_RESULT__=" + json.dumps(summary, ensure_ascii=False))
    return result


def main() -> int:
    project_id = os.environ.get("PROJECT_ID", "P001")
    upload_base = os.environ.get("UPLOAD_BASE_PATH", "C:/Project_Web/019_defineXML/uploads")
    data_path = os.environ.get("DATA_PATH", os.path.join(upload_base, project_id, "xpt"))
    username = os.environ.get("USERNAME_CONTEXT", "")
    scope = os.environ.get("EXTRACT_SCOPE", os.environ.get("EXTRACTION_SCOPE", "ALL"))
    result = run(project_id, data_path, username, scope)
    return 0 if result.get("success") else 1


if __name__ == "__main__":
    sys.exit(main())
