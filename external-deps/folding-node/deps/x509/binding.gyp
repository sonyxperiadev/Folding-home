{
  'target_defaults': {
    'variables': {
      'version': '<!(node -pe \'require("./package.json").version\')'
    },
    'defines': [
      'VERSION="<@(version)"'
    ]
  },

  'targets': [
    {
      'target_name': 'x509',
      'type': '<(library)',
      'sources': [
        'src/addon.cc',
        'src/x509.cc'
      ],
      'include_dirs': [
        'include',
        '../nan', '../../src', '../v8/include', '../uv/include', '../openssl/openssl/include'
      ],
      'conditions': [
        [ 'OS=="win"', {
          'conditions': [
            # "openssl_root" is the directory on Windows of the OpenSSL files
            ['target_arch=="x64"', {
              'variables': {
                'openssl_root%': '../openssl/openssl/include'
              },
            }, {
              'variables': {
                'openssl_root%': '../openssl/openssl/include'
              },
            }],
          ],
          'defines': [
            'uint=unsigned int',
          ],
          'libraries': [ 
            '-l<(openssl_root)/lib/libeay32.lib',
          ],
          'include_dirs': [
            '<(openssl_root)/include',
          ],
        }, { # OS!="win"
          'include_dirs': [
            # use node's bundled openssl headers on Unix platforms
            '../openssl/openssl/include'
          ],
        }],
      ],	  
    }
  ]
}
