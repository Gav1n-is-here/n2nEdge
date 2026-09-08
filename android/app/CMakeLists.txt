cmake_minimum_required(VERSION 3.22.1)
project(n2nEdge C)
get_filename_component(ROOT "${CMAKE_CURRENT_SOURCE_DIR}/.." ABSOLUTE)
set(N2N "${ROOT}/vendor/n2n")
set(BRIDGE "${ROOT}/vendor/bridge")
set(BUNDLES "${ROOT}/vendor")
set(CMAKE_C_STANDARD 99)
add_compile_definitions(__ANDROID_NDK__ N2N_V3 CMAKE_BUILD SKIP_MULTICAST_PEERS_DISCOVERY
    N2N_VERSION="3.0.0" N2N_OSNAME="Android" GIT_RELEASE="3.0"
    PACKAGE_VERSION="3.0.0" PACKAGE_OSNAME="Android" UIP_CONF_BYTE_ORDER=1234)
set(CORE n2n edge_management curve25519 auth edge_utils wire minilzo speck pearson tf
    header_encryption transform_null transform_tf transform_aes transform_speck transform_cc20
    cc20 aes network_traffic_filter sn_selection random_numbers)
list(TRANSFORM CORE PREPEND "${N2N}/src/")
list(TRANSFORM CORE APPEND ".c")
add_library(edge_jni SHARED ${CORE}
    src/main/cpp/edge_jni.c
    ${BRIDGE}/edge_jni/edge_android_v2.c
    ${BRIDGE}/edge_jni/tuntap_android_v2.c
    ${BUNDLES}/uip/uip/uip.c ${BUNDLES}/uip/uip/uip_arp.c
    ${BUNDLES}/tun2tap/tun2tap.c)
target_include_directories(edge_jni PRIVATE ${N2N}/include ${BRIDGE} ${BRIDGE}/edge_jni
    ${BUNDLES} ${BUNDLES}/uip ${BUNDLES}/uip/uip ${BUNDLES}/tun2tap)
target_compile_options(edge_jni PRIVATE -Wno-incompatible-pointer-types -Wno-deprecated-declarations)
target_link_libraries(edge_jni log)
