#ifndef EXIT_SIGNAL_H_
#define EXIT_SIGNAL_H_

#include <signal.h>

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <Windows.h>
#endif

namespace ExitSignal {

#ifdef FAH_CORE
#ifdef _WIN32
	void setLifeline(DWORD pid);
#else
	void setLifeline(pid_t pid);
#endif
#endif

/* Initialize */
void init();

/* If the core should exit or not */
bool shouldExit();

void forceExit();

/* Number of seconds the core should run for */
void setExitTime(int t_in_seconds);

}

#endif
