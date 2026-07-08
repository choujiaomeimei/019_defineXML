import fitz  # PyMuPDF
import pandas as pd
import os
import sys

def extract_pdf_annotations(pdf_path, output_excel):
    """
    提取PDF注释并保存到Excel文件
    
    Args:
        pdf_path: 输入PDF文件路径
        output_excel: 输出Excel文件路径
    
    Returns:
        bool: 是否成功
    """
    try:
        # 检查输入文件是否存在
        if not os.path.exists(pdf_path):
            print(f"错误: 输入PDF文件不存在: {pdf_path}")
            return False
        
        print(f"开始处理PDF文件: {pdf_path}")
        print(f"输出文件路径: {output_excel}")
        
        # 提取注释
        annotations = []
        doc = fitz.open(pdf_path)

        # 新字段: seq, FillColor, font
        for page_num in range(len(doc)):
            page = doc[page_num]
            annots = list(page.annots()) if page.annots() else []
            for seq, annot in enumerate(annots, 1):
                # 只保留FreeText类型的注释
                if annot.type[1] != 'FreeText':
                    continue
                # FillColor
                fill_color = None
                if hasattr(annot, 'colors') and annot.colors and 'stroke' in annot.colors:
                    color = annot.colors['stroke']
                    if color:
                        # 转换为十六进制格式 #RRGGBB
                        r = int(color[0] * 255)
                        g = int(color[1] * 255)
                        b = int(color[2] * 255)
                        fill_color = f"#{r:02X}{g:02X}{b:02X}"
                # 尝试多种方法获取字体信息
                font = ""
                
                # 方法1: 从info获取
                if 'fontname' in annot.info:
                    font = annot.info['fontname']
                    print(f"Found font in info: {font}")
                
                # 方法2: 尝试获取文本状态
                try:
                    if hasattr(annot, 'get_textpage'):
                        text_page = annot.get_textpage()
                        if text_page:
                            print(f"TextPage available")
                except:
                    pass
                    
                # 方法3: 获取注释的原始数据
                try:
                    xref = annot.xref
                    if xref > 0:
                        obj = doc.xref_get_key(xref, "DA")  # Default Appearance
                        if obj:
                            print(f"Default Appearance: {obj}")
                            font = obj  # DA字符串可能包含字体信息
                except:
                    pass
                    
                # 方法4: 尝试获取AP流
                try:
                    ap = annot.info.get('AP', '')
                    if ap:
                        print(f"Appearance stream: {ap}")
                except:
                    pass
                    
                # 方法5: 检查注释的内容流
                try:
                    if hasattr(annot, 'get_contents'):
                        contents = annot.get_contents()
                        print(f"Contents stream: {contents}")
                except:
                    pass
                    
                print(f"Page {page_num + 1}, Annotation {seq}: Type={annot.type[1]}, Font='{font}'")
                
                # 解析字体信息，提取字体名称和大小
                font_info = ""
                font_size_info = ""
                if font:
                    # 如果font是元组，取第二个元素（实际字符串内容）
                    font_str = font[1] if isinstance(font, tuple) else font
                    print(f"Processing font string: {font_str}")
                    
                    # 查找斜杠后面的内容
                    slash_pos = font_str.rfind('/')  # 找到最后一个斜杠
                    if slash_pos != -1:
                        after_slash = font_str[slash_pos + 1:]  # 获取斜杠后的内容
                        print(f"After slash: {after_slash}")
                        
                        # 解析字体名称和大小 (格式: FontName size Tf)
                        import re
                        match = re.match(r'([^,\s]+(?:,[^,\s]+)*)\s+(\d+(?:\.\d+)?)\s+Tf', after_slash)
                        if match:
                            font_info = match.group(1)  # 只保存字体名称
                            font_size_info = match.group(2)  # 单独保存字体大小
                            print(f"Extracted: font={font_info}, size={font_size_info}")
                        else:
                            font_info = after_slash  # 如果解析失败，保留斜杠后的内容
                    else:
                        font_info = font_str  # 如果没有斜杠，保留原字符串
                info = {
                    'page': page_num + 1,
                    'seqnum': seq,
                    'Type': annot.type[1],
                    'Contents': annot.info.get('content', ''),
                    'FillColor': fill_color or '',
                    'font': font_info,
                    'font_size': font_size_info,
                }
                annotations.append(info)

        doc.close()

        # 写入Excel
        if annotations:
            df = pd.DataFrame(annotations, columns=["page", "seqnum", "Type", "Contents", "FillColor", "font", "font_size"])
            os.makedirs(os.path.dirname(output_excel), exist_ok=True)
            df.to_excel(output_excel, index=False)
            from excel_style import style_excel_file
            style_excel_file(output_excel)
            print(f"已提取{len(annotations)}条注释到: {output_excel}")
            return True
        else:
            print("未找到任何注释！")
            return False
            
    except Exception as e:
        print(f"处理过程中发生错误: {str(e)}")
        return False

def main():
    """主函数，处理命令行参数"""
    if len(sys.argv) < 3:
        print("用法: python extract_pdf_annotations.py <输入PDF路径> <输出Excel路径>")
        print("示例: python extract_pdf_annotations.py input.pdf output.xlsx")
        return 1
    
    pdf_path = sys.argv[1]
    output_excel = sys.argv[2]
    
    print(f"API_RESULT:")
    if extract_pdf_annotations(pdf_path, output_excel):
        print('{"success": true, "message": "PDF注释提取成功", "output_file": "' + os.path.basename(output_excel) + '"}')
    else:
        print('{"success": false, "error": "PDF注释提取失败"}')
    print("====")
    
    return 0

if __name__ == "__main__":
    sys.exit(main())