cmake_minimum_required(VERSION 3.22.1)

project("security")

# ── Existing API & update-check security library ──────────────────────────
add_library(
    security
    SHARED
    security.c
    ma-check.c
)

find_library(log-lib log)

target_link_libraries(
    security
    ${log-lib}
)

# ── Integrity & anti-tamper library ───────────────────────────────────────
# Compiled as a separate .so so that IntegrityGuard.kt loads it independently.
# Keeping it separate means a patch to security.so cannot disable the checks.
add_library(
    integrity
    SHARED
    integrity.c
)

target_link_libraries(
    integrity
    ${log-lib}
)
