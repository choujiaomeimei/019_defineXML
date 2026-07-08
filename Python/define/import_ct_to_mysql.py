"""
将 define/CT 目录下的 CDISC 受控术语 Excel 导入 MySQL 库 cdisc_ct。
分类维度：standard_type（ADaM / SDTM / SEND）、release_date、language_code（EN / ZH）。

建表可通过 MCP user-mysql 执行；本脚本负责读取 .xls 并批量写入。
环境变量（可选）：MYSQL_HOST, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DATABASE（默认 cdisc_ct）
"""
from __future__ import annotations

import os
import re
from datetime import date
from pathlib import Path

import pandas as pd
import pymysql

# 与后端 application-dev.yml 一致的本地默认值（可被环境变量覆盖）
DEFAULT_HOST = os.environ.get("MYSQL_HOST", "127.0.0.1")
DEFAULT_USER = os.environ.get("MYSQL_USER", "root")
DEFAULT_PASSWORD = os.environ.get("MYSQL_PASSWORD", "123123")
DEFAULT_DB = os.environ.get("MYSQL_DATABASE", "define_db")

CT_DIR = Path(__file__).resolve().parent / "CT"

DATE_IN_NAME = re.compile(r"(\d{4})-(\d{2})-(\d{2})")


def _norm(v):
    if v is None or (isinstance(v, float) and pd.isna(v)):
        return None
    if pd.isna(v):
        return None
    s = str(v).strip()
    return s if s else None


def classify_file(path: Path) -> tuple[str, str, date | None]:
    """返回 (standard_type, language_code, release_date)。"""
    name = path.name.upper()
    if "SEND" in name:
        std = "SEND"
    elif "ADAM" in name:
        std = "ADaM"
    elif "SDTM" in name:
        std = "SDTM"
    elif "CHINESE" in name or "CONTROLLED_TERMINOLOGY_CHINESE" in name:
        std = "SDTM"
    else:
        std = "SDTM"

    lang = "ZH" if ("CHINESE" in name or "CONTROLLED_TERMINOLOGY_CHINESE" in name) else "EN"

    rel: date | None = None
    m = DATE_IN_NAME.search(path.name)
    if m:
        rel = date(int(m.group(1)), int(m.group(2)), int(m.group(3)))
    return std, lang, rel


def pick_sheet(xl: pd.ExcelFile, path: Path, std: str) -> str:
    names = xl.sheet_names
    if std == "ADaM":
        for s in names:
            if s.startswith("ADaM Terminology"):
                return s
    if std == "SDTM" and "CHINESE" not in path.name.upper():
        for s in names:
            if s.startswith("SDTM Terminology"):
                return s
    if "Most Commonly Used" in " ".join(names):
        for s in names:
            if "Most Commonly Used" in s:
                return s
    return names[-1]


def load_frame(path: Path) -> tuple[pd.DataFrame, str]:
    xl = pd.ExcelFile(path)
    std, lang, _ = classify_file(path)
    sheet = pick_sheet(xl, path, std)
    df = pd.read_excel(path, sheet_name=sheet, header=0)
    return df, sheet


def _df_col(df: pd.DataFrame, *names: str):
    cmap = {str(c).strip(): c for c in df.columns}
    for n in names:
        if n in cmap:
            return cmap[n]
    lower = {str(c).strip().lower(): c for c in df.columns}
    for n in names:
        k = n.lower()
        if k in lower:
            return lower[k]
    return None


def build_term_rows(df: pd.DataFrame, lang: str) -> list[tuple]:
    """不含 package_id，末尾为 sort_order。"""
    c_code = _df_col(df, "Code")
    if c_code is None:
        raise ValueError(f"缺少 Code 列，当前列: {list(df.columns)}")

    c_clist = _df_col(df, "Codelist Code")
    c_ext = _df_col(df, "Codelist Extensible (Yes/No)")
    c_name = _df_col(df, "Codelist Name", "Codelist name")
    c_sub = _df_col(df, "CDISC Submission Value")
    c_syn = _df_col(df, "CDISC Synonym(s)")
    c_def = _df_col(df, "CDISC Definition")
    c_nci = _df_col(df, "NCI Preferred Term")
    c_name_zh = _df_col(df, "代码表名称")
    c_sub_zh = _df_col(df, "CDISC提交值")
    c_syn_zh = _df_col(df, "CDISC 同义词")

    rows = []
    for i in range(len(df)):
        if lang == "ZH":
            rows.append(
                (
                    _norm(df[c_code].iloc[i]),
                    _norm(df[c_clist].iloc[i]) if c_clist else None,
                    _norm(df[c_ext].iloc[i]) if c_ext else None,
                    _norm(df[c_name].iloc[i]) if c_name else None,
                    _norm(df[c_name_zh].iloc[i]) if c_name_zh else None,
                    _norm(df[c_sub].iloc[i]) if c_sub else None,
                    _norm(df[c_sub_zh].iloc[i]) if c_sub_zh else None,
                    _norm(df[c_syn].iloc[i]) if c_syn else None,
                    _norm(df[c_syn_zh].iloc[i]) if c_syn_zh else None,
                    _norm(df[c_def].iloc[i]) if c_def else None,
                    _norm(df[c_nci].iloc[i]) if c_nci else None,
                    i,
                )
            )
        else:
            rows.append(
                (
                    _norm(df[c_code].iloc[i]),
                    _norm(df[c_clist].iloc[i]) if c_clist else None,
                    _norm(df[c_ext].iloc[i]) if c_ext else None,
                    _norm(df[c_name].iloc[i]) if c_name else None,
                    None,
                    _norm(df[c_sub].iloc[i]) if c_sub else None,
                    None,
                    _norm(df[c_syn].iloc[i]) if c_syn else None,
                    None,
                    _norm(df[c_def].iloc[i]) if c_def else None,
                    _norm(df[c_nci].iloc[i]) if c_nci else None,
                    i,
                )
            )
    return rows


def import_all():
    if not CT_DIR.is_dir():
        raise SystemExit(f"CT 目录不存在: {CT_DIR}")

    conn = pymysql.connect(
        host=DEFAULT_HOST,
        user=DEFAULT_USER,
        password=DEFAULT_PASSWORD,
        database=DEFAULT_DB,
        charset="utf8mb4",
        autocommit=False,
    )
    sql_pkg = (
        "INSERT INTO ct_package (standard_type, release_date, language_code, file_name, sheet_name, row_count) "
        "VALUES (%s,%s,%s,%s,%s,%s)"
    )
    sql_term = (
        "INSERT INTO ct_term (package_id, term_code, codelist_code, codelist_extensible, "
        "codelist_name, codelist_name_zh, cdisc_submission_value, cdisc_submission_value_zh, "
        "cdisc_synonyms, cdisc_synonyms_zh, cdisc_definition, nci_preferred_term, sort_order) "
        "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)"
    )
    chunk = 2000
    try:
        with conn.cursor() as cur:
            for path in sorted(CT_DIR.glob("*.xls")):
                std, lang, rel = classify_file(path)
                df, sheet = load_frame(path)
                if rel is None and lang == "ZH":
                    m2 = re.search(r"CT(\d{4})(\d{2})\d*", sheet, re.I)
                    if m2:
                        rel = date(int(m2.group(1)), int(m2.group(2)), 1)
                body = build_term_rows(df, lang)

                cur.execute(
                    sql_pkg,
                    (
                        std,
                        rel,
                        lang,
                        path.name,
                        sheet,
                        len(body),
                    ),
                )
                pid = cur.lastrowid
                tuples = [(pid,) + t for t in body]

                for off in range(0, len(tuples), chunk):
                    cur.executemany(sql_term, tuples[off : off + chunk])
                print(f"OK {path.name} -> package_id={pid} rows={len(tuples)}")
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    import_all()
