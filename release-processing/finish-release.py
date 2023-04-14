#!/usr/bin/env python3
# vim: set expandtab tabstop=4 shiftwidth=4:

import os
import sys
import shutil
import subprocess

app_name = 'OpenBLCMM'

# See if we're being run from a "known" location
if os.path.exists('finish-release.py') and os.path.exists('launch_openblcmm.sh'):
    os.chdir('../store')
elif os.path.exists('README-developing.md') and os.path.exists('store'):
    os.chdir('store')

# Now check to make sure that some dirs+files exist
for dirname in [
        'compiled',
        ]:
    if not os.path.isdir(dirname):
        raise RuntimeError(f'No `{dirname}` directory found')
included_files_exe = [
        '../README.md',
        '../LICENSE.txt',
        '../src/CHANGELOG.md',
        ]
included_files_jar = included_files_exe + [
        f'{app_name}.jar',
        '../release-processing/launch_openblcmm.bat',
        '../release-processing/launch_openblcmm.sh',
        ]
for filename in included_files_jar:
    if not os.path.exists(filename):
        raise RuntimeError(f'No `{filename}` file found')

# Grab version from the installer in Output
# TODO: We should probably support seeing multiple of these and just
# building the most recent one (and maybe support an arg to specify
# building arbitrary versions)
# OpenBLCMM-1.3.0-beta.3-Installer.exe
version = None
installer = None
prefix = f'{app_name}-'
suffix = '-Installer.exe'
for filename in os.listdir('.'):
    if filename.startswith(prefix) and filename.endswith(suffix):
        if version is not None:
            raise RuntimeError('More than one installer EXE found')
        version = filename[len(prefix):-len(suffix)]
        installer = filename
if version is None:
    raise RuntimeError('No installer EXE found')

# Make sure we've got our expected DLLs and EXEs in there
exe_count = 0
dll_count = 0
other_count = 0
for filename in os.listdir('compiled'):
    if filename.endswith('.dll'):
        dll_count += 1
    elif filename.endswith('.exe'):
        exe_count += 1
    else:
        other_count += 1
if exe_count != 1:
    raise RuntimeError(f'Invalid EXE count in `compiled`: {exe_count}')
if dll_count != 10:
    raise RuntimeError(f'Invalid DLL count in `compiled`: {dll_count}')
if other_count != 0:
    raise RuntimeError(f'Invalid other count in `compiled`: {other_count}')

# Zip up the non-installer EXE version
base_win_zip = f'{app_name}-{version}-Windows'
shutil.move('compiled', base_win_zip)
for filename in included_files_exe:
    shutil.copy2(filename, base_win_zip)
subprocess.run(['zip', '-r',
    f'{base_win_zip}.zip',
    base_win_zip,
    ])

# Zip up the "native" Java Jar version
base_java_zip = f'{app_name}-{version}-Java'
os.makedirs(base_java_zip)
for filename in included_files_jar:
    shutil.copy2(filename, base_java_zip)
subprocess.run(['zip', '-r',
    f'{base_java_zip}.zip',
    base_java_zip,
    ])

# Display the results
print('')
subprocess.run(['ls', '-l',
    installer,
    f'{base_win_zip}.zip',
    f'{base_java_zip}.zip',
    ])
print('')

