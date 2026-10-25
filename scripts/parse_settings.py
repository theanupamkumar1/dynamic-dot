import re, sys

res_map = {}
for enc in ['utf-8', 'utf-16le', 'latin1']:
    try:
        with open('D:/projects/dynamic iland/all_resources.txt', 'r', encoding=enc, errors='ignore') as rf:
            for line in rf:
                m = re.search(r'resource (0x[0-9a-fA-F]+)\s+[^\s]+:(?:string|drawable|layout|id)/([a-zA-Z0-9_]+)', line)
                if m:
                    res_map[m.group(1).lower()] = m.group(2)
        if len(res_map) > 100:
            print(f'Loaded {len(res_map)} resources with encoding {enc}')
            break
    except Exception as e:
        pass

string_val_map = {}
for enc in ['utf-8', 'utf-16le', 'latin1']:
    try:
        with open('D:/projects/dynamic iland/dynamicspot_strings.txt', 'r', encoding=enc, errors='ignore') as sf:
            for line in sf:
                m = re.search(r'resource (0x[0-9a-fA-F]+)\s+string/([a-zA-Z0-9_]+)\s+:\s*\"(.*)\"', line)
                if m:
                    string_val_map[m.group(1).lower()] = (m.group(2), m.group(3))
        if len(string_val_map) > 100:
            print(f'Loaded {len(string_val_map)} strings with encoding {enc}')
            break
    except Exception as e:
        pass

with open('D:/projects/dynamic iland/decompiled_settings.txt', 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

with open('D:/projects/dynamic iland/settings_detailed.txt', 'w', encoding='utf-8') as out:
    elem = ''
    for line in text.splitlines():
        if line.startswith('=== '):
            out.write('\n\n' + '='*60 + '\n' + line + '\n' + '='*60 + '\n')
        m_elem = re.search(r'E:\s+([A-Za-z0-9_.]+)', line)
        if m_elem:
            elem = m_elem.group(1).split('.')[-1]
        m_key = re.search(r'android:key\([^\)]*\)=\"([^\"]+)\"', line)
        if m_key:
            out.write(f'\n  [{elem}] Key: {m_key.group(1)}\n')
        m_title = re.search(r'android:title\([^\)]*\)=(@0x[0-9a-fA-F]+|\"[^\"]*\")', line)
        if m_title:
            raw = m_title.group(1)
            hex_id = raw.replace('@', '').lower()
            name = res_map.get(hex_id, '')
            val = string_val_map.get(hex_id, ('', ''))[1]
            out.write(f'    Title: ({name}) \"{val}\"\n')
        m_sum = re.search(r'android:summary\([^\)]*\)=(@0x[0-9a-fA-F]+|\"[^\"]*\")', line)
        if m_sum:
            raw = m_sum.group(1)
            hex_id = raw.replace('@', '').lower()
            name = res_map.get(hex_id, '')
            val = string_val_map.get(hex_id, ('', ''))[1]
            out.write(f'    Summary: ({name}) \"{val}\"\n')
        m_def = re.search(r'android:defaultValue\([^\)]*\)=([^\s]+)', line)
        if m_def:
            out.write(f'    Default: {m_def.group(1)}\n')

print('Done generating settings_detailed.txt')
