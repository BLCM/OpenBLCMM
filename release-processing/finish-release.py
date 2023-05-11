#!/usr/bin/env python3
# vim: set expandtab tabstop=4 shiftwidth=4:

import os
import sys
import enum
import shutil
import subprocess

# Some control vars
app_name = 'OpenBLCMM'
base_openblcmm = 'https://github.com/BLCM/OpenBLCMM'
release_openblcmm = f'{base_openblcmm}/releases'
release_data = 'https://github.com/BLCM/OpenBLCMM-Data/releases'
supported_java = '8, 11, 17, and 20'
included_files_base = [
        '../README.md',
        '../LICENSE.txt',
        '../src/CHANGELOG.md',
        ]

# Pure-Java Archive Types
class ArchiveType(enum.Enum):
    ZIP = enum.auto()
    TAR = enum.auto()

class PureJava:
    """
    Class to contain a bunch of info about "pure" Java releases.  These are
    all handled pretty similarly, and lets us do various looping.
    """

    def __init__(self, os_name, archive_type,
            files,
            launch_text,
            do_header=True,
            java_text='We recommend [Adoptium Temurin](https://adoptium.net/)',
            extra_points=None,
            redirect_files=None,
            ):
        self.os_name = os_name
        self.archive_type = archive_type
        self.files = files
        self.launch_text = launch_text
        self.do_header = do_header
        self.zipfile = None
        self.java_text = java_text
        if extra_points is None:
            self.extra_points = []
        else:
            self.extra_points = extra_points
        if redirect_files is None:
            self.redirect_files = {}
        else:
            self.redirect_files = redirect_files

# Construct a list of "pure" Java zips that we'll construct.
pure_javas = [
        PureJava('Windows',
            ArchiveType.ZIP,
            files=included_files_base + [
                f'{app_name}.jar',
                '../release-processing/launch_openblcmm.bat',
                ],
            launch_text="Launch with `launch_openblcmm.bat` once Java is installed",
            do_header=False,
            ),
        PureJava('Linux',
            ArchiveType.TAR,
            files=included_files_base + [
                f'{app_name}.jar',
                '../release-processing/launch_openblcmm.sh',
                ],
            launch_text="Launch with `launch_openblcmm.sh` one Java is installed",
            java_text="Just install via your distro's package manager",
            ),
        PureJava('Mac',
            ArchiveType.TAR,
            files=included_files_base + [
                f'{app_name}.jar',
                '../release-processing/launch_openblcmm.command',
                ],
            launch_text="Launch by doubleclicking on `launch_openblcmm.command`",

            # "Old" parameters from trying to figure out Automator-wrapped .app
            # distribution.  This seems rather finnicky, so at the *moment*
            # we're not doing that.
            #files=included_files_base + [
            #    '../release-processing/osx-app/__MACOSX',
            #    '../release-processing/osx-app/OpenBLCMM.app',
            #    ],
            #redirect_files={
            #    # Current app bundling wants the jarfile *inside* the .app bundle
            #    f'{app_name}.jar': 'OpenBLCMM.app',
            #    },
            #extra_points=[
            #    "**Note:** This launcher is slightly experimental!  Feedback on how well it works is appreciated!",
            #    ],
            ),
        ]

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
total_files = set(included_files_base)
for pj in pure_javas:
    total_files |= set(pj.files)
for filename in sorted(total_files):
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

to_report = [installer]

# Zip up the non-installer EXE version
print('Creating non-installer EXE Zip...')
base_win_zip = f'{app_name}-{version}-Windows'
full_win_zip = f'{base_win_zip}.zip'
shutil.move('compiled', base_win_zip)
for filename in included_files_base:
    shutil.copy2(filename, base_win_zip)
subprocess.run(['zip', '-r',
    full_win_zip,
    base_win_zip,
    ])
print('')
to_report.append(full_win_zip)

# Zip up the "native" Java Jar versions
for pj in pure_javas:
    match pj.archive_type:
        case ArchiveType.ZIP:
            label = 'Zip'
            ext = 'zip'
            command = ['zip', '-r']
        case ArchiveType.TAR:
            label = 'TGZ'
            ext = 'tgz'
            command = ['tar', '-zcvf']
        case _:
            raise RuntimeError(f'Unknown archive type: {pj.archive_type}')

    print(f'Creating {pj.os_name} Java {label}...')
    base_java_zip = f'{app_name}-{version}-Java-{pj.os_name}'
    pj.zipfile = f'{base_java_zip}.{ext}'
    os.makedirs(base_java_zip)
    for filename in pj.files:
        if os.path.isdir(filename):
            _, last_part = os.path.split(filename)
            shutil.copytree(filename, os.path.join(base_java_zip, last_part))
        else:
            shutil.copy2(filename, base_java_zip)
    for orig_filename, new_location in pj.redirect_files.items():
        shutil.copy2(orig_filename, os.path.join(base_java_zip, new_location))
    subprocess.run([*command,
        pj.zipfile,
        base_java_zip,
        ])
    print('')
    to_report.append(pj.zipfile)

# Output our release text to slap in the 'releases' area
release_with_version = f'{release_openblcmm}/download/v{version}'
print('Release Files')
print('-------------')
print('')
print('### Windows')
print('')
print(f'- **Installer (Recommended!)** - [`{installer}`]({release_with_version}/{installer})')
print('  - This is the easiest to get going!  You\'ll also have a start menu entry, and optionally a desktop icon.')
print(f'- **Zipfile EXE** - [`{full_win_zip}`]({release_with_version}/{full_win_zip})')
print('  - If you don\'t want to use an installer, this is the second-easiest.  Just unzip wherever you like and doubleclick on `OpenBLCMM.exe` to run!')
print('    Note that the Zipfile EXE requires [Microsoft\'s Visual C++ Redistributable](https://aka.ms/vs/17/release/vc_redist.x64.exe)')
for pj in pure_javas:
    if pj.do_header:
        print(f'### {pj.os_name}')
        print('')
    print(f'- **Pure Java** - [`{pj.zipfile}`]({release_with_version}/{pj.zipfile})')
    print(f'  - This version *requires* that Java is installed, and will *not* install Java for you.  {pj.java_text}.  Supported Java versions are: {supported_java}.  {pj.launch_text}.')
    for point in pj.extra_points:
        print(f'  - {point}')
    print('')
print('### Object Explorer Data Packs')
print('')
print(f'- **Datapack Releases**: {release_data}')
print('  - Datapacks must now be downloaded manually.  Download the ones you want and store them in the same directory as `OpenBLCMM.exe` or `OpenBLCMM.jar`.  The app will see them on the next startup!')
print('')
print('Changelog')
print('---------')
print('')
print(f'[v{version} Full Changelog]({base_openblcmm}/blob/v{version}/src/CHANGELOG.md)')
print('')

# Display the results
print('File uploads:')
print('')
subprocess.run(['ls', '-l', *to_report])
print('')

