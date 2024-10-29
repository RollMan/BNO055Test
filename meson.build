project('Bno055Test', 'java',
  version : '0.1',
  meson_version: '>= 1.1, < 2.0',
  default_options : ['warning_level=3'])

main_class = 'Bno055Test'
src_files = files(
  'Bno055Test.java',
  'net/coppelab/java/bno055/Bno055.java',
  'net/coppelab/java/bno055/AMGRaw.java',
  'net/coppelab/java/geo/Point3D.java',
)

processing_root = get_option('processing_path')

if processing_root == ''
    error('Please provide the path to processing library e.g., $HOME/.local/share/processing-4.3.')
endif

lib = [
  processing_root / 'core' / 'library' / 'core.jar',
  processing_root / 'core' / 'library' / 'gluegen-rt.jar',
  processing_root / 'core' / 'library' / 'jogl-all.jar',
  processing_root / 'modes' / 'java' / 'libraries' / 'serial' / 'library' / 'jssc.jar',
  processing_root / 'modes' / 'java' / 'libraries' / 'serial' / 'library' / 'serial.jar',
]
classpath = ':'.join(lib)

# WIP: Extract required jars
# tar = find_program('tar')
# foreach lib_jar : lib
#   custom_target('compile_lib_jar',
#     input: lib_jar,
#     command: [tar, 'xf', '@INPUT@', '-C', meson.current_build_dir()
# endforeach

javac_args = [
  '--class-path', classpath,
]

bno055_test_jar = jar('Bno055Test',
    src_files,
    main_class: main_class,
    install : false,
    java_args: javac_args,
)

devenv = environment()
devenv.set('CLASSPATH', ':'.join(lib + [meson.current_build_dir() / (main_class + '.jar')]))
meson.add_devenv(devenv)
