#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P21报告生成器API版本
适用于后端API调用，无需用户交互
支持命令行参数覆盖 config.py 中的默认路径
"""

import sys
import json
import argparse
from pathlib import Path

sys.path.append(str(Path(__file__).parent / 'pgm'))


def parse_args():
    parser = argparse.ArgumentParser(description='P21 Define.xlsx Generator')
    parser.add_argument('--spec-file', type=str, help='Path to project spec Excel file (overrides config.py)')
    parser.add_argument('--vlm-json', type=str, help='Path to VLM data JSON file')
    parser.add_argument('--codelist-json', type=str, help='Path to codelist data JSON file')
    parser.add_argument('--pages-json', type=str, help='Path to pages data JSON file')
    parser.add_argument('--spec-json', type=str, help='Path to spec (dfspec) data JSON file')
    parser.add_argument('--output-dir', type=str, help='Output directory for generated define.xlsx')
    parser.add_argument('--ig', type=str, help='Standard type: SDTM or ADaM')
    parser.add_argument('--lang', type=str, help='Language: CN or EN')
    parser.add_argument('--protocol', type=str, help='Protocol number')
    parser.add_argument('--study-title', type=str, help='Study title')
    parser.add_argument('--p21-template', type=str, help='Path to P21 blank spec template')
    return parser.parse_args()


def main():
    """主函数，返回执行结果"""
    args = parse_args()

    try:
        import config as cfg
        from p21report_main import ModularP21Processor

        # Override config with CLI arguments when provided
        if args.spec_file:
            spec_path = Path(args.spec_file)
            cfg.SPEC_DIR = spec_path.parent
            cfg.PROJECT_SPEC_FILE = spec_path.name
            cfg.CONFIG['SPEC_DIR'] = str(spec_path.parent)
            cfg.CONFIG['SPEC'] = spec_path.name

        if args.output_dir:
            cfg.OUTPUT_DIR = Path(args.output_dir)
            cfg.CONFIG['OUTPUT_DIR'] = args.output_dir

        if args.ig:
            cfg.IG = args.ig
            cfg.CONFIG['IG'] = args.ig

        if args.lang:
            cfg.LANG = args.lang
            cfg.CONFIG['LANG'] = args.lang

        if args.protocol:
            cfg.PROTOCAL = args.protocol
            cfg.CONFIG['PROTOCAL'] = args.protocol

        if args.study_title:
            cfg.STUDY_TITLE = args.study_title
            cfg.CONFIG['STUDY_TITLE'] = args.study_title

        if args.p21_template:
            tpl = Path(args.p21_template)
            cfg.P21_SPEC_DIR = tpl.parent
            cfg.P21_SPEC_FILE = tpl.name
            cfg.CONFIG['P21_SPEC_DIR'] = str(tpl.parent)
            cfg.CONFIG['P21_SPEC'] = tpl.name

        print("=" * 60)
        print("        模块化P21 Define.xlsx 生成器 (API版本)")
        print("=" * 60)

        processor = ModularP21Processor()

        # If external VLM/Codelist JSON provided, inject before processing
        if args.vlm_json and Path(args.vlm_json).exists():
            import pandas as pd
            vlm_data = json.loads(Path(args.vlm_json).read_text(encoding='utf-8'))
            if vlm_data:
                processor._external_vlm = pd.DataFrame(vlm_data)
                print(f"  外部VLM数据已加载: {len(vlm_data)} 条记录")

        if args.codelist_json and Path(args.codelist_json).exists():
            import pandas as pd
            cl_data = json.loads(Path(args.codelist_json).read_text(encoding='utf-8'))
            if cl_data:
                processor._external_codelist = pd.DataFrame(cl_data)
                print(f"  外部Codelist数据已加载: {len(cl_data)} 条记录")

        if args.pages_json and Path(args.pages_json).exists():
            import pandas as pd
            pages_data = json.loads(Path(args.pages_json).read_text(encoding='utf-8'))
            if pages_data:
                processor._external_pages = pd.DataFrame(pages_data)
                print(f"  外部Pages数据已加载: {len(pages_data)} 条记录")

        if args.spec_json and Path(args.spec_json).exists():
            import pandas as pd
            spec_records = json.loads(Path(args.spec_json).read_text(encoding='utf-8'))
            if spec_records:
                spec_df = pd.DataFrame(spec_records)
                col_map = {
                    'variable': 'Variable Name', 'label': 'Variable Label',
                    'type': 'Type', 'length': 'Length',
                    'controlledTermsOrFormat': 'Controlled Terms or Format',
                    'cdiscSubmissionValue': 'CDISC Submission Value',
                    'origin': 'Origin', 'role': 'Role', 'core': 'Core',
                    'codelist': 'Codelist', 'format': 'Format',
                    'comment': 'Comment', 'cdiscNotes': 'CDISC Notes',
                    'domain': 'domain', 'pages': 'Pages',
                    'textContent': 'Text', 'derivation': 'Text',
                }
                for old_col, new_col in col_map.items():
                    if old_col in spec_df.columns and new_col not in spec_df.columns:
                        spec_df.rename(columns={old_col: new_col}, inplace=True)
                processor._external_spec = spec_df
                print(f"  外部Spec(dfspec)数据已加载: {len(spec_records)} 条记录")

        result = processor.run()

        print()
        print("=" * 60)
        print("        生成完成!")
        print("=" * 60)

        return {
            "success": True,
            "message": "Define.xlsx生成成功",
            "output_dir": str(processor.output_dir),
            "output_file": str(processor.output_dir / processor.config['OUTSPEC'])
        }

    except ImportError as e:
        error_msg = f"导入错误：{e}\n请确保所有依赖包已安装：pip install pandas numpy openpyxl xlrd"
        print(error_msg)
        return {"success": False, "error": error_msg}
    except Exception as e:
        error_msg = f"运行错误：{e}"
        print(error_msg)
        return {"success": False, "error": error_msg}


if __name__ == "__main__":
    result = main()
    print("\n" + "=" * 60)
    print("API_RESULT:")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    print("=" * 60)
    sys.exit(0 if result["success"] else 1)