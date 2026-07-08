#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
配置文件读取器 - 支持从命令行参数读取JSON配置
"""

import json
import sys
import argparse
from pathlib import Path

def read_config_from_args():
    """从命令行参数读取配置文件"""
    parser = argparse.ArgumentParser(description='P21报告生成器')
    parser.add_argument('--config', type=str, help='配置文件路径')
    parser.add_argument('--task-id', type=str, help='任务ID')

    args = parser.parse_args()

    if args.config:
        config_path = Path(args.config)
        if config_path.exists():
            with open(config_path, 'r', encoding='utf-8') as f:
                config = json.load(f)
            print(f"已读取配置文件: {config_path}")
            print(f"任务ID: {args.task_id}")
            return config
        else:
            print(f"配置文件不存在: {config_path}")
            return None

    # 如果没有指定配置文件，使用基于脚本位置推导的默认配置
    pgm_dir = Path(__file__).resolve().parent
    python_dir = pgm_dir.parent
    base_dir = python_dir.parent
    default_config = {
        'PROTOCAL': 'MJR-MR001-01',
        'STUDY_TITLE': 'MR001药物I期临床试验',
        'BASE_DIR': str(base_dir),
        'SPEC': 'spec.xlsx',
        'P21_SPEC': 'define.xlsx',
        'CT_SHEETNAME': '',
        'OUTPUT_DIR': str(python_dir / 'output')
    }

    print("使用默认配置")
    return default_config

if __name__ == '__main__':
    config = read_config_from_args()
    if config:
        print("配置内容:")
        for key, value in config.items():
            print(f"  {key}: {value}")