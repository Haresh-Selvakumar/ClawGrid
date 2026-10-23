cmake_minimum_required(VERSION 3.16)
# The pinned PlatformIO IDF builder does not forward board_build.sdkconfig_defaults.
# Select defaults here, before project.cmake, so a clean UART environment is
# reproducible without an ignored hand-edited sdkconfig file.
set(SDKCONFIG_DEFAULTS "${CMAKE_CURRENT_LIST_DIR}/sdkconfig.defaults")
get_filename_component(CLAWGRID_SDKCONFIG_NAME "${SDKCONFIG}" NAME)
if(CLAWGRID_SDKCONFIG_NAME MATCHES "^sdkconfig[.]native_voice_uart(_diagnostic|_bench)?$")
    list(APPEND SDKCONFIG_DEFAULTS "${CMAKE_CURRENT_LIST_DIR}/sdkconfig.uart.defaults")
endif()
set(COMPONENTS main __pio_env)
include($ENV{IDF_PATH}/tools/cmake/project.cmake)
project(clawgrid_native)
