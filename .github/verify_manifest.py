"""Fail before publication if a dependency silently changes the expected manifest surface."""
import json,sys,xml.etree.ElementTree as ET
from pathlib import Path
manifest=ET.parse(sys.argv[1]).getroot();ns='{http://schemas.android.com/apk/res/android}'
expected=json.loads(Path(sys.argv[2]).read_text(encoding='utf-8'))
actual={'permissions':sorted(node.get(ns+'name') for node in manifest.findall('uses-permission')),
        'startup_initializers':sorted(node.get(ns+'name') for node in manifest.iter('meta-data') if node.get(ns+'value')=='androidx.startup')}
for key in expected:
 if actual[key]!=expected[key]:
  raise SystemExit(key+' differ: added='+repr(sorted(set(actual[key])-set(expected[key])))+' removed='+repr(sorted(set(expected[key])-set(actual[key]))))
assert manifest.find('instrumentation') is None, 'Main APK must exclude instrumentation'
assert manifest.find('application').get(ns+'debuggable','false')=='false', 'Main APK must be non-debuggable'
print('Merged permissions and startup initializers match this checkpoint')
