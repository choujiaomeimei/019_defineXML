import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import pandas as pd

sys.path.insert(0, str(Path(__file__).parent))

import codelist_extractor as subject


class TextNormalizationTests(unittest.TestCase):
    def test_normalizes_integer_whitespace_and_full_width_punctuation(self):
        self.assertEqual(subject.normalize_text(1.0), "1")
        self.assertEqual(subject.normalize_text("  A　，  B  "), "A , B")
        self.assertEqual(subject.normalize_text("12.000"), "12")

    def test_deduplicates_after_normalization(self):
        values = subject.dedupe_values([" A，B ", "A,B", "a,b", None, ""])
        self.assertEqual(values, ["A,B"])

    def test_parse_where_clause_accepts_eq_and_equals(self):
        self.assertEqual(subject.parse_where_clause('LBTESTCD EQ "ALT"'), ("LBTESTCD", "ALT"))
        self.assertEqual(subject.parse_where_clause("LBTESTCD = 'ALT'"), ("LBTESTCD", "ALT"))
        self.assertIsNone(subject.parse_where_clause("unsupported clause"))


class TermExtractionTests(unittest.TestCase):
    def test_preserves_code_description_pair_and_normalizes_numbers(self):
        frame = subject.normalize_frame(pd.DataFrame({
            "LBTESTCD": ["ALT", "ALT", "AST"],
            "LBTEST": ["Alanine，aminotransferase", "Alanine,aminotransferase", "Aspartate"],
        }))
        codes, texts = subject.extract_paired_terms(frame, "LBTESTCD", "LBTEST")
        self.assertEqual(codes, [
            {"code": "ALT", "codeDes": "Alanine,aminotransferase"},
            {"code": "AST", "codeDes": "Aspartate"},
        ])
        self.assertEqual(len(texts), 2)

    def test_unique_terms_turn_integer_float_into_integer_text(self):
        frame = pd.DataFrame({"VALUE": [1.0, 1, 2.0]})
        self.assertEqual(subject.extract_unique_terms(frame, "VALUE"), [
            {"code": "1", "codeDes": ""},
            {"code": "2", "codeDes": ""},
        ])

    def test_draft_assigns_scoped_nci_codes(self):
        draft = subject.Draft()
        draft.add(
            subject.VAR_SOURCE,
            "AE.AESER",
            "Serious Event",
            [{"code": "Y", "codeDes": "Yes"}, {"code": "X", "codeDes": "Custom"}],
            submission_value="NY",
            ct_names={"NY": "No Yes Response"},
            ct_headers={"NY": "C66742"},
            ct_terms={("C66742", "Y"): "C49488"},
            terminology="SDTM Terminology 2025-09-26",
        )
        entry = draft.codelists[(subject.VAR_SOURCE, "AE.AESER")]
        self.assertEqual(entry["nciCodelistCode"], "C66742")
        self.assertEqual(entry["vlabel"], "No Yes Response")
        self.assertEqual(entry["terms"][0]["nciTermCode"], "C49488")
        self.assertEqual(entry["terms"][1]["nciTermCode"], "")

    def test_uses_canonical_system_source_names(self):
        self.assertEqual(subject.VAR_SOURCE, "extract_var_codelist")
        self.assertEqual(subject.VLM_SOURCE, "extract_vlm_codelist")

    def test_infers_numeric_codelist_types(self):
        self.assertEqual(subject.infer_data_type(["1", "2"]), "Integer")
        self.assertEqual(subject.infer_data_type(["1.5", "2"]), "Float")
        self.assertEqual(subject.infer_data_type(["Y", "N"]), "Char")


class InputFallbackTests(unittest.TestCase):
    def test_read_xpt_uses_declared_encoding_order(self):
        frame = pd.DataFrame({"term": ["A"]})
        with patch.object(subject.pyreadstat, "read_xport") as reader:
            reader.side_effect = [
                UnicodeDecodeError("utf8", b"x", 0, 1, "bad"),
                (frame, object()),
            ]
            actual, encoding = subject.read_xpt(Path("dummy.xpt"))
        self.assertEqual(encoding, "latin1")
        self.assertEqual(actual.columns.tolist(), ["TERM"])
        self.assertEqual(reader.call_count, 2)
        self.assertEqual(reader.call_args_list[0].kwargs, {})
        self.assertEqual(reader.call_args_list[1].kwargs, {"encoding": "latin1"})

    def test_dataset_xpt_is_read_only_once(self):
        frame = pd.DataFrame({"A": ["x"]})
        with tempfile.TemporaryDirectory() as directory:
            xpt = Path(directory) / "lb.xpt"
            xpt.touch()
            cache = subject.DatasetCache(Path(directory))
            with patch.object(subject, "read_xpt", return_value=(frame, "auto")) as reader:
                self.assertIsNotNone(cache.get("LB", ("A",)))
                self.assertIsNotNone(cache.get("LB", ("A",)))
            reader.assert_called_once_with(xpt)

    def test_edc_csv_fills_missing_xpt_column(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            edc = root / "edc.csv"
            pd.DataFrame({"DOMAIN": ["LB"], "SUBMISSION": ["A"]}).to_csv(edc, index=False)
            cache = subject.DatasetCache(root, str(edc))
            frame = cache.get("LB", ("SUBMISSION",))
            self.assertEqual(frame["SUBMISSION"].tolist(), ["A"])
            self.assertEqual(cache.fallbacks[0]["reason"], "missing XPT")


if __name__ == "__main__":
    unittest.main()
