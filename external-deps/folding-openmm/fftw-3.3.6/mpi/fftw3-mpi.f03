! Generated automatically.  DO NOT EDIT!

  include 'fftw3.f03'

  integer(C_INTPTR_T), parameter :: FFTW_MPI_DEFAULT_BLOCK = 0
  integer(C_INT), parameter :: FFTW_MPI_SCRAMBLED_IN = 134217728
  integer(C_INT), parameter :: FFTW_MPI_SCRAMBLED_OUT = 268435456
  integer(C_INT), parameter :: FFTW_MPI_TRANSPOSED_IN = 536870912
  integer(C_INT), parameter :: FFTW_MPI_TRANSPOSED_OUT = 1073741824

  type, bind(C) :: fftw_mpi_ddim
     integer(C_INTPTR_T) n, ib, ob
  end type fftw_mpi_ddim

  interface
  end interface

  type, bind(C) :: fftwf_mpi_ddim
     integer(C_INTPTR_T) n, ib, ob
  end type fftwf_mpi_ddim

  interface
  end interface
