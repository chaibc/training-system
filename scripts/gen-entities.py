from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
schema = (root / 'sql' / 'schema.sql').read_text(encoding='utf-8')
base = root / 'server' / 'src' / 'main' / 'java' / 'com' / 'hanmo' / 'calligraphy' / 'domain'
(base / 'entity').mkdir(parents=True, exist_ok=True)
(base / 'mapper').mkdir(parents=True, exist_ok=True)

# existing classes in system package: skip those table names
skip = {'sys_user', 'sys_role', 'sys_role_permission', 'sys_audit_log', 'parent_profile'}

type_map = {
    'BIGINT': 'Long',
    'INT': 'Integer',
    'TINYINT': 'Integer',
    'DECIMAL': 'BigDecimal',
    'VARCHAR': 'String',
    'TEXT': 'String',
    'DATETIME': 'LocalDateTime',
    'DATE': 'LocalDate',
    'TIME': 'LocalTime',
}

def pascal(name):
    return ''.join(p[:1].upper() + p[1:] for p in name.split('_'))

blocks = re.findall(r'CREATE TABLE `([^`]+)` \((.*?)\) ENGINE=', schema, re.S)
for table, body in blocks:
    if table in skip:
        continue
    cls = pascal(table)
    fields = []
    for line in body.splitlines():
        line = line.strip()
        if not line.startswith('`'):
            continue
        m = re.match(r'`([^`]+)`\s+([A-Z]+)', line)
        if not m:
            continue
        col, typ = m.group(1), m.group(2)
        if typ not in type_map:
            continue
        java_type = type_map[typ]
        field = ''.join(p[:1].upper() + p[1:] for p in col.split('_'))
        field = field[:1].lower() + field[1:]
        fields.append((field, java_type, col))

    imports = ['import com.baomidou.mybatisplus.annotation.IdType;',
               'import com.baomidou.mybatisplus.annotation.TableId;',
               'import com.baomidou.mybatisplus.annotation.TableName;',
               'import lombok.Data;']
    if any(t == 'BigDecimal' for _, t, _ in fields):
        imports.append('import java.math.BigDecimal;')
    if any(t in ('LocalDateTime', 'LocalDate', 'LocalTime') for _, t, _ in fields):
        imports.append('import java.time.*;')
    if any(c == 'deleted' for _, _, c in fields):
        imports.append('import com.baomidou.mybatisplus.annotation.TableLogic;')

    lines = [f'package com.hanmo.calligraphy.domain.entity;', '', *sorted(set(imports)), '',
             '@Data', f'@TableName("{table}")', f'public class {cls} {{', '']
    for field, typ, col in fields:
        if col == 'id':
            lines += ['    @TableId(type = IdType.AUTO)', f'    private Long id;', '']
        elif col == 'deleted':
            lines += ['    @TableLogic', f'    private Integer deleted;', '']
        else:
            lines += [f'    private {typ} {field};']
    lines += ['}', '']
    (base / 'entity' / f'{cls}.java').write_text('\n'.join(lines), encoding='utf-8')

    mapper = f'''package com.hanmo.calligraphy.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanmo.calligraphy.domain.entity.{cls};
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface {cls}Mapper extends BaseMapper<{cls}> {{
}}
'''
    (base / 'mapper' / f'{cls}Mapper.java').write_text(mapper, encoding='utf-8')

print('generated entities and mappers')
