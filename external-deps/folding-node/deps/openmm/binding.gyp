{
  'targets': [
    {
      'target_name': 'openmm',
      'type': '<(library)',
      'sources': [
        'src/bindings.cc',
        'src/NodeCore.cpp',
        'src/ExitSignal.cpp',
        'src/OpenMMCore.cpp',
        'src/XTCWriter.cpp',
        'src/StateTests.cpp'
      ],
      'include_dirs': [
        './include',
        './src',
        '../nan',
        '../../src',
        '../../deps/uv/include',
        '../../deps/v8/include'
      ],
      'cflags_cc': ['-frtti', '-fexceptions' ],
      'xcode_settings': {
          'GCC_ENABLE_CPP_RTTI': 'YES',
          'GCC_ENABLE_CPP_EXCEPTIONS': 'YES',
          'CLANG_CXX_LANGUAGE_STANDARD':'c++11',
          'MACOSX_DEPLOYMENT_TARGET':'10.11'
      },
      'link_settings': {
        'ldflags': [
          '-L${PWD}/deps/openmm/libraries/<(OS)',
          '-L${PWD}/deps/openmm/libraries/<(OS)/plugins'
        ]},
      'conditions': [
        ['OS=="android"', {
          'defines': ['ARM'],
          'link_settings' : {'libraries': ['-lOpenMM','-lOpenMMCPU','-lOpenMMDrude','-lOpenMMRPMD']}
        }],
        ['OS=="linux"', {
          'defines': ['x86'],
          'link_settings' : {'libraries': ['-lOpenMM','-lOpenMMCPU','-lOpenMMDrude','-lOpenMMRPMD']}
        }],
        ['OS=="mac"', {
          'defines': ['x86'],
          'link_settings' : {'libraries': [
              '${PWD}/deps/openmm/libraries/<(OS)/libOpenMM.dylib',
              '${PWD}/deps/openmm/libraries/<(OS)/plugins/libOpenMMCPU.dylib',
              '${PWD}/deps/openmm/libraries/<(OS)/libOpenMMDrude.dylib',
              '${PWD}/deps/openmm/libraries/<(OS)/libOpenMMRPMD.dylib'
            ]}
        }],
		['OS=="win"', {
          'defines': ['x86', 'OPENMM_USE_STATIC_LIBRARIES'],
          'link_settings' : {'libraries': [
              '-l<!(echo %cd%)/libraries/<(OS)/OpenMM.lib',
              '-l<!(echo %cd%)/libraries/<(OS)/plugins/OpenMMCPU.lib',
              '-l<!(echo %cd%)/libraries/<(OS)/OpenMMDrude.lib',
              '-l<!(echo %cd%)/libraries/<(OS)/OpenMMRPMD.lib'
            ]}
        }],
      ],
      'defines': [
         'OPENMM_CPU',
         'NODEJS'
      ]
    }]
}
