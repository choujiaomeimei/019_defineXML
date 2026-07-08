# 配置文件：存放所有路径、文件名、参数等
from pathlib import Path
import datetime

# 根目录
BASE_DIR = Path(r'E:/JAVAPROJ/008_defineXML')

# 主要目录
BASE_DATA_DIR = BASE_DIR / 'Python' / 'define'  # 基础目录
XPT_DATA_DIR = BASE_DATA_DIR / 'sdtm define package'  # XPT数据目录
SPEC_DIR = BASE_DIR / 'Python' / 'output' / 'Spec_out'  # 项目规范目录
P21_SPEC_DIR = BASE_DATA_DIR / 'p21空spec'  # P21空规范目录
CT_DIR = BASE_DATA_DIR / 'CT'  # CT目录
OUTPUT_DIR = BASE_DIR / 'Python' / 'output'  # Python输出目录
PGM_DIR = BASE_DIR / 'Python' / 'pgm'  # Python脚本目录

# 主要文件名
P21_SPEC_FILE = 'define.xlsx'
PROJECT_SPEC_FILE = 'spec.xlsx'
CT_VERSION = 'SDTM Terminology_2023-06-30.xls'
CT_SHEETNAME = 'SDTM Terminology 2023-06-30'
CT_VERSION_CN = 'Controlled_Terminology_Chinese_Translation.xls'
CT_SHEETNAME_CN = 'Most Commonly Used CT202103'
ADAM_CT = 'ADaM IG 1.1兼容：ADaM Terminology 2019-03-29.xls'
ADAM_CT_SHEETNAME = 'ADaM Terminology 2019-03-29'

# 输出define.xlsx文件名（可根据日期自动生成）
now = datetime.datetime.now().strftime('%Y%m%d')
OUTSPEC = f'sdtm_define_out_{now}.xlsx'

# 其他参数
IG = 'SDTM'  # 或 'ADaM'
LANG = 'CN'  # 或 'EN'
PROTOCAL = 'MJR-MR001-01'
STUDY_TITLE = 'MR001药物I期临床试验'

# Excel工作表名称映射
EXCEL_SHEETS = {
    'DEFINE': 'Define',
    'DATASETS': 'Datasets', 
    'VARIABLES': 'Variables',
    'VALUELEVEL': 'ValueLevel',
    'CODELISTS': 'Codelists',
    'DICTIONARIES': 'Dictionaries',
    'METHODS': 'Methods',
    'COMMENTS': 'Comments',
    'DOCUMENTS': 'Documents'
}

# 配置字典 - 供主程序使用
CONFIG = {
    'BASE_DIR': str(BASE_DIR),
    'DATA_DIR': str(XPT_DATA_DIR),
    'SPEC_DIR': str(SPEC_DIR),
    'P21_SPEC_DIR': str(P21_SPEC_DIR),
    'CT_DIR': str(CT_DIR),
    'OUTPUT_DIR': str(OUTPUT_DIR),
    'P21_SPEC': P21_SPEC_FILE,
    'SPEC': PROJECT_SPEC_FILE,
    'CT_VERSION': CT_VERSION,
    'CT_SHEETNAME': CT_SHEETNAME,
    'CT_VERSION_CN': CT_VERSION_CN,
    'CT_SHEETNAME_CN': CT_SHEETNAME_CN,
    'OUTSPEC': OUTSPEC,
    'IG': IG,
    'LANG': LANG,
    'PROTOCAL': PROTOCAL,
    'STUDY_TITLE': STUDY_TITLE,
    'EXCEL_SHEETS': EXCEL_SHEETS
}