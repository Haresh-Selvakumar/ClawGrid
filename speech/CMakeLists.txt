cmake_minimum_required(VERSION 3.16)
set(SDKCONFIG_DEFAULTS "${CMAKE_CURRENT_LIST_DIR}/../native/sdkconfig.defaults;${CMAKE_CURRENT_LIST_DIR}/sdkconfig.defaults")
# The pinned PlatformIO builder does not forward board_build.sdkconfig_defaults.
# Select the same verified UART0 defaults before IDF generates a fresh config.
get_filename_component(CLAWGRID_SDKCONFIG_NAME "${SDKCONFIG}" NAME)
if(CLAWGRID_SDKCONFIG_NAME MATCHES "^sdkconfig[.]speech_intelligence_uart(_short_v1|_mn7_eval|_light_preflight)?$")
    list(APPEND SDKCONFIG_DEFAULTS "${CMAKE_CURRENT_LIST_DIR}/../native/sdkconfig.uart.defaults")
endif()
if(CLAWGRID_SDKCONFIG_NAME STREQUAL "sdkconfig.speech_intelligence_uart_mn7_eval")
    list(APPEND SDKCONFIG_DEFAULTS "${CMAKE_CURRENT_LIST_DIR}/sdkconfig.mn7.defaults")
endif()
set(COMPONENTS main __pio_env esp-sr)
include($ENV{IDF_PATH}/tools/cmake/project.cmake)
project(clawgrid_speech)
# The MN6 probability-to-log adapter uses a verified private layout because
# this pinned upstream public setter stores the wrong units. Refuse an archive
# change until the adapter and its tests are reviewed for that new ABI.
file(SHA256 "${CMAKE_CURRENT_LIST_DIR}/components/esp-sr/lib/esp32s3/libmultinet.a" CLAWGRID_MN6_ARCHIVE_HASH)
if(NOT CLAWGRID_MN6_ARCHIVE_HASH STREQUAL "c5d8c65bee0467ad3620252c62d038364747e1a58344e6e070e9d10da8062e22")
    message(FATAL_ERROR "Unverified MN6 archive: review the pinned threshold-unit adapter before building")
endif()
# PlatformIO supplies one outer archive group. Nested flags exported by the
# upstream component are reordered by its CMake importer; remove those flags
# from target properties while retaining every upstream library and source.
idf_component_get_property(sr_library esp-sr COMPONENT_LIB)
foreach(property LINK_LIBRARIES INTERFACE_LINK_LIBRARIES)
    get_target_property(sr_links ${sr_library} ${property})
    if(sr_links)
        list(REMOVE_ITEM sr_links "-Wl,--start-group" "-Wl,--end-group")
        set_property(TARGET ${sr_library} PROPERTY ${property} "${sr_links}")
    endif()
endforeach()
