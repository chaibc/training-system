# -*- coding: utf-8 -*-
"""
把 MySQL 版 SQL 转成 H2（MySQL 兼容模式）版，供本地 dev profile 使用。

用法：
    python scripts/gen-h2.py

输入/输出：
    sql/schema.sql        → server/src/main/resources/db/schema-h2.sql
    sql/seed.sql          → server/src/main/resources/db/data-h2.sql

⚠️ 输出文件是**生成物**，手工修改会在下次执行本脚本时被覆盖。
    要改表结构请改 sql/ 下的源文件，再重新生成。

转换内容（H2 不兼容 MySQL 的部分）：
    1. 去掉 CREATE DATABASE / USE / SET NAMES；
    2. 去掉 ENGINE=InnoDB、CHARSET、COMMENT、ON UPDATE CURRENT_TIMESTAMP；
    3. 普通 KEY 索引直接丢弃；UNIQUE KEY 转成 CONSTRAINT ... UNIQUE；
    4. 修正 H2 要求的约束间逗号。
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
sql = (root / 'sql' / 'schema.sql').read_text(encoding='utf-8')
seed = (root / 'sql' / 'seed.sql').read_text(encoding='utf-8')

# schema: remove database/use, engine/charset/comment, key lines, ON UPDATE
sql = re.sub(r'CREATE DATABASE[\s\S]*?USE `hanmo_calligraphy`;\n', '', sql, count=1)
sql = re.sub(r' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=\'[^\']*\';', ';', sql)
sql = re.sub(r" COMMENT '[^']*'", '', sql)
sql = re.sub(r' ON UPDATE CURRENT_TIMESTAMP', '', sql)
lines = []
for line in sql.splitlines():
    stripped = line.strip()
    if re.match(r'^(UNIQUE )?KEY `', stripped):
        if stripped.startswith('UNIQUE KEY'):
            m = re.match(r'UNIQUE KEY `([^`]+)` \((.+)\),?$', stripped)
            if m:
                lines.append(f'  CONSTRAINT `{m.group(1)}` UNIQUE ({m.group(2)})')
            continue
        # normal index: drop (H2 doesn't need it)
        continue
    # remove trailing comma from the previous line if we just dropped the last key
    if stripped == ');' and lines and lines[-1].rstrip().endswith(','):
        lines[-1] = lines[-1].rstrip()[:-1]
    lines.append(line)
sql = '\n'.join(lines)
# H2 needs commas between consecutive table constraints
sql = re.sub(r'(PRIMARY KEY \([^)]*\))\n(\s*CONSTRAINT)', r'\1,\n\2', sql)
sql = re.sub(r'(CONSTRAINT `[^`]+` UNIQUE \([^)]*\))\n(\s*CONSTRAINT)', r'\1,\n\2', sql)
# clean dangling commas before )
sql = re.sub(r',\s*\n\)', '\n)', sql)
(root / 'server' / 'src' / 'main' / 'resources' / 'db').mkdir(parents=True, exist_ok=True)
(root / 'server' / 'src' / 'main' / 'resources' / 'db' / 'schema-h2.sql').write_text(sql, encoding='utf-8')

seed = seed.replace('USE `hanmo_calligraphy`;\n', '').replace('SET NAMES utf8mb4;\n', '')
(root / 'server' / 'src' / 'main' / 'resources' / 'db' / 'data-h2.sql').write_text(seed, encoding='utf-8')
print('generated schema-h2.sql and data-h2.sql')
