#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P21报告生成器API版本
适用于后端API调用，无需用户交互
"""

import sys
import json
from pathlib import Path

# 添加程序目录到路径
sys.path.append(str(Path(__file__).parent / 'pgm'))

def main():
    """主函数，返回执行结果"""
    try:
        from p21report_main import ModularP21Processor
        
        print("=" * 60)
        print("        模块化P21 Define.xlsx 生成器 (API版本)")
        print("=" * 60)
        print()
        print("功能：基于P21空spec和项目spec生成完整的define.xlsx")
        print("特点：")
        print("  ✓ 模块化设计，每个sheet独立工作")
        print("  ✓ 类似SAS程序结构，便于维护")
        print("  ✓ 保持原P21格式和变量顺序")
        print("  ✓ 智能缓存，提高处理速度")
        print()
        
        # 创建处理器实例并运行
        processor = ModularP21Processor()
        result = processor.run()
        
        print()
        print("=" * 60)
        print("        生成完成!")
        print("=" * 60)
        print()
        print("输出文件位置：")
        print(f"  {processor.output_dir}")
        print()
        print("包含以下9个完整表单：")
        print("  ✓ Define - 研究基础信息")
        print("  ✓ Datasets - 数据集信息")
        print("  ✓ Variables - 变量详细信息（核心功能）") 
        print("  ✓ ValueLevel - 值级别信息")
        print("  ✓ Codelists - 代码列表")
        print("  ✓ Dictionaries - 字典信息")
        print("  ✓ Methods - 方法定义")
        print("  ✓ Comments - 注释定义")
        print("  ✓ Documents - 文档引用")
        print()
        
        # 返回成功状态和输出文件路径
        return {
            "success": True,
            "message": "Define.xlsx生成成功",
            "output_dir": str(processor.output_dir),
            "output_file": str(processor.output_dir / processor.config['OUTSPEC'])
        }
        
    except ImportError as e:
        error_msg = f"导入错误：{e}\n请确保所有依赖包已安装：pip install pandas numpy openpyxl xlrd"
        print(error_msg)
        return {
            "success": False,
            "error": error_msg
        }
    except Exception as e:
        error_msg = f"运行错误：{e}\n请检查：\n1. 配置文件路径是否正确（config.py）\n2. P21空spec文件是否存在\n3. 项目spec文件是否存在"
        print(error_msg)
        return {
            "success": False,
            "error": error_msg
        }

if __name__ == "__main__":
    result = main()
    # 输出JSON结果供API读取
    print("\n" + "="*60)
    print("API_RESULT:")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    print("="*60)
    
    # 返回适当的退出码
    sys.exit(0 if result["success"] else 1)