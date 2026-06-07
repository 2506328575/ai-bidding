"""在原模板中注入 AI 内容并返回填充后的 docx。支持 Markdown 表格、**粗体**、# 标题。"""
import sys, json, re
from docx import Document
from docx.shared import Pt, Inches
from docx.oxml.ns import qn
from lxml import etree

W_NS = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
NSMAP = {'w': W_NS}

def fill(template_path, content_json_path, output_path):
    doc = Document(template_path)
    with open(content_json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    # 模式判断: fillMap 模式 = 简单替换 {{placeholder}}
    # contentMap 模式 = 在标题后插入AI内容
    is_fill_map = all('{{' not in k for k in data.keys() if isinstance(data, dict))

    if is_fill_map or len(data) > 0 and isinstance(data, dict):
        # fillMap 模式: 全文替换 {{placeholder}}
        fill_map_simple(doc, data)
    else:
        fill_content_mode(doc, data)

    doc.save(output_path)

def fill_map_simple(doc, fill_map):
    """简单模式：全文替换 {{placeholder}} 为对应值"""
    for para in doc.paragraphs:
        para_text = para.text
        replaced = False
        for key, value in fill_map.items():
            placeholder = '{{' + key + '}}'
            if placeholder in para_text:
                para_text = para_text.replace(placeholder, str(value))
                replaced = True
        if replaced:
            # 保留格式 + 替换文本
            for run in para.runs:
                run.text = ''
            if para.runs:
                para.runs[0].text = para_text
    # 表格中也替换
    for table in doc.tables:
        for row in table.rows:
            for cell in row.cells:
                for para in cell.paragraphs:
                    for key, value in fill_map.items():
                        placeholder = '{{' + key + '}}'
                        if placeholder in para.text:
                            for run in para.runs:
                                run.text = run.text.replace(placeholder, str(value))

def fill_content_mode(doc, content_map):
            insertions.append((i, content_map[text]))

    # 从后往前插入
    for idx, ai_text in reversed(insertions):
        target_elem = doc.paragraphs[idx]._element
        next_elem = target_elem.getnext()

        # 解析 AI 内容为 blocks（段落 / 表格）
        blocks = parse_blocks(ai_text)

        for block in blocks:
            if block['type'] == 'table':
                insert_table_after(target_elem, next_elem, block['rows'])
            else:
                insert_paragraph_after(target_elem, next_elem, block['text'])

    doc.save(output_path)

def parse_blocks(text):
    """将 AI 文本解析为段落和表格的混合列表"""
    lines = text.split('\n')
    blocks = []
    table_lines = []
    in_table = False

    for line in lines:
        stripped = line.strip()
        if not stripped:
            if in_table:
                blocks.append({'type': 'table', 'rows': table_lines})
                table_lines = []
                in_table = False
            continue

        if is_table_line(stripped):
            if not in_table:
                in_table = True
                table_lines = []
            if not is_table_separator(stripped):
                table_lines.append(stripped)
        else:
            if in_table:
                blocks.append({'type': 'table', 'rows': table_lines})
                table_lines = []
                in_table = False
            blocks.append({'type': 'para', 'text': stripped})

    if in_table and table_lines:
        blocks.append({'type': 'table', 'rows': table_lines})

    return blocks

def is_table_line(line):
    return line.startswith('|') and '|' in line[1:]

def is_table_separator(line):
    return re.match(r'^\|[\s\-:|\s]+\|$', line)

def insert_table_after(after_elem, next_elem, rows_data):
    """在 after_elem 后插入真实 Word 表格"""
    if len(rows_data) < 2:
        return
    # 解析表格数据
    rows = []
    for line in rows_data:
        cells = [c.strip() for c in line.split('|')[1:-1]]
        rows.append(cells)

    parent = after_elem.getparent()
    tbl_elem = etree.SubElement(parent, '{%s}tbl' % W_NS)

    # 表格属性
    tblPr = etree.SubElement(tbl_elem, '{%s}tblPr' % W_NS)
    tblW = etree.SubElement(tblPr, '{%s}tblW' % W_NS)
    tblW.set('{%s}w' % W_NS, '5000')
    tblW.set('{%s}type' % W_NS, 'pct')
    # tblGrid (required by OOXML spec)
    tblGrid = etree.SubElement(tbl_elem, '{%s}tblGrid' % W_NS)
    num_cols = len(rows[0]) if rows else 2
    for _ in range(num_cols):
        gc = etree.SubElement(tblGrid, '{%s}gridCol' % W_NS)
        gc.set('{%s}w' % W_NS, str(5000 // num_cols))
    borders = etree.SubElement(tblPr, '{%s}tblBorders' % W_NS)
    for border_name in ['top', 'left', 'bottom', 'right', 'insideH', 'insideV']:
        border = etree.SubElement(borders, '{%s}%s' % (W_NS, border_name))
        border.set('{%s}val' % W_NS, 'single')
        border.set('{%s}sz' % W_NS, '4')
        border.set('{%s}space' % W_NS, '0')
        border.set('{%s}color' % W_NS, '000000')

    for ri, row_data in enumerate(rows):
        tr = etree.SubElement(tbl_elem, '{%s}tr' % W_NS)
        is_header = ri == 0
        for cell_text in row_data:
            tc = etree.SubElement(tr, '{%s}tc' % W_NS)
            tcPr = etree.SubElement(tc, '{%s}tcPr' % W_NS)
            tcW = etree.SubElement(tcPr, '{%s}tcW' % W_NS)
            tcW.set('{%s}w' % W_NS, str(5000 // len(row_data)))
            tcW.set('{%s}type' % W_NS, 'dxa')
            p = etree.SubElement(tc, '{%s}p' % W_NS)
            write_runs(p, cell_text, is_header, font_size=20 if is_header else 20)

    # 移到正确位置
    if next_elem is not None:
        next_elem.addprevious(tbl_elem)

def insert_paragraph_after(after_elem, next_elem, text):
    """在 after_elem 后插入段落，支持内联 **粗体**"""
    parent = after_elem.getparent()
    p_elem = etree.SubElement(parent, '{%s}p' % W_NS)
    write_runs(p_elem, text, is_bold=False, font_size=22)

    if next_elem is not None:
        next_elem.addprevious(p_elem)

def write_runs(parent_elem, text, is_bold=False, font_size=22):
    """将文本写入XML元素，处理 **粗体** 分割"""
    parts = re.split(r'(\*\*.*?\*\*)', text)
    bold = False
    for part in parts:
        if part.startswith('**') and part.endswith('**'):
            part = part[2:-2]
            bold = True
        r_elem = etree.SubElement(parent_elem, '{%s}r' % W_NS)
        rPr = etree.SubElement(r_elem, '{%s}rPr' % W_NS)
        if bold or is_bold:
            etree.SubElement(rPr, '{%s}b' % W_NS)
        sz_elem = etree.SubElement(rPr, '{%s}sz' % W_NS)
        sz_elem.set('{%s}val' % W_NS, str(font_size))
        szCs = etree.SubElement(rPr, '{%s}szCs' % W_NS)
        szCs.set('{%s}val' % W_NS, str(font_size))
        t_elem = etree.SubElement(r_elem, '{%s}t' % W_NS)
        t_elem.text = part
        t_elem.set('{http://www.w3.org/XML/1998/namespace}space', 'preserve')
        bold = False

if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else 'fill'
    if cmd == 'fill':
        fill(sys.argv[2], sys.argv[3], sys.argv[4])
